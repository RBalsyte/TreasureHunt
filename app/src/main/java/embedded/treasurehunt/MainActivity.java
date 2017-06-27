package embedded.treasurehunt;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

import static android.widget.AbsListView.CHOICE_MODE_SINGLE;

/*
* The app is intended to be used outside, therefore we use GPS location, that is based on satellite data,
* instead of Network location, which is based on cell towers and wifi points
* */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_GAME_FINISH = 0;
    private static final int REQUEST_CODE = 1340;
    private static final String TAG = "MainActivity";

    private ListView treasureListView;
    private int selectedPosition;

    private List<Integer> ids;
    private List<String> names = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private int selectedTreasureId = -1;
    
    private boolean gameFinished = true;// initial is true
    private boolean idsProcessed = false;
    private boolean namesProcessed = false;
    
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // request permissions
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        requestPermissions();

        treasureListView = (ListView)findViewById(R.id.treasuresList);
        final Button startButton = (Button)findViewById(R.id.startButton);
        if (treasureListView == null || startButton == null){
            new CustomAlertDialog(MainActivity.this,
                    "Something went wrong with the game. Try to start a new game.",
                    getString(R.string.ok), new CloseAppListener());
            return;
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        treasureListView.setAdapter(adapter);
        treasureListView.setChoiceMode(CHOICE_MODE_SINGLE);

        treasureListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                int newId = ids.get(position);
                if(selectedTreasureId < 0 && !gameFinished){
                    new CustomAlertDialog(MainActivity.this,
                            "You selected a new game. Are you sure you want to start a new game?",
                            getString(R.string.yes), new NewGameListener(newId, position),
                            getString(R.string.cancel), new CancelListener()).show();
                }else{
                    selectedPosition = position;
                    selectedTreasureId = newId;
                    Log.d(TAG, "Selected treasure id: " + selectedTreasureId);
                }
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                start();
            }
        });

        // fill the list of available games
        getTreasuresLists();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Returned from HintActivity");
        if (requestCode == REQUEST_GAME_FINISH && resultCode == Activity.RESULT_OK) {
            gameFinished = true;
            Log.d(TAG, "Game finished.");
        }
        // else it paused and doesn't need update
    }

    private void  requestPermissions() {
        if (!isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            Toast toast = Toast.makeText(MainActivity.this, "Please turn on your wifi.", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        
        if (!isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast toast = Toast.makeText(MainActivity.this, "Please turn on your GPS", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        
        int finePermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        // request if needed
        if (finePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty -> permission denied -> close the app
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissions denied");
                new CustomAlertDialog(MainActivity.this,
                        "This app won't work without this permission. App will be closed.",
                        getString(R.string.ok), new CloseAppListener()).show();
            }
        }
    }

    private void start(){
        //by now it should be granted, but check again
        requestPermissions();

        if (!idsProcessed){
            Toast toast = Toast.makeText(MainActivity.this, "Still retrieving information from the database. Please try again in a bit.", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        if (!namesProcessed){
            Toast toast = Toast.makeText(MainActivity.this, "Still retrieving information from the database. Please try again in a bit.", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        if (ids == null || ids.isEmpty() || names.isEmpty()){
            new CustomAlertDialog(MainActivity.this,
                    "Failed to retrieve data from the database. App will be closed.",
                    getString(R.string.ok), new CloseAppListener()).show();
            return;
        }

        if(selectedTreasureId < 0){
            CharSequence text = "Select a game from the list first.";
            Toast toast = Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        Log.d(TAG, "Starting a game");
        gameFinished = false;
        Intent intent = new Intent(this, HintActivity.class);
        intent.putExtra("treasureId", selectedTreasureId);
        startActivityForResult(intent, REQUEST_GAME_FINISH);
    }

    private void getTreasuresLists(){
        AsyncHttpClient client = new AsyncHttpClient();
        JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);
                Gson gson = new Gson();
                Type listType = new TypeToken<List<Integer>>(){}.getType();
                ids = gson.fromJson(response.toString(), listType);
                Log.d(TAG, "Got ids of the games "+ ids.toString());
                idsProcessed = true;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.d(TAG, "Failed to get ids of the games");
                idsProcessed = true;
            }
        };
        responseHandler.setUsePoolThread(true);
        client.get("http://140.78.187.215:8151/treasures/ids", null, responseHandler);

        JsonHttpResponseHandler responseHandler2 = new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<String>>(){}.getType();
                names = gson.fromJson(response.toString(), listType);
                runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.addAll(names);
                        adapter.notifyDataSetChanged();
                    }
                });
                Log.d(TAG, "Got names of the games "+ names.toString());
                namesProcessed = true;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.d(TAG, "Failed to get names of the games");
                namesProcessed = true;
            }
        };
        responseHandler2.setUsePoolThread(true);
        client.get("http://140.78.187.215:8151/treasures/names", null, responseHandler2);
    }

    private class CloseAppListener implements DialogInterface.OnClickListener{

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            Log.d(TAG, "Closing");
            MainActivity.this.finish();
        }
    }

    private class NewGameListener implements DialogInterface.OnClickListener{
        int newId;
        int newPosition;

        NewGameListener(int newId, int newPosition){
            this.newId = newId;
            this.newPosition = newPosition;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            selectedTreasureId = newId;
            selectedPosition = newPosition;

            Log.d(TAG, "New game selected. Closing all activities up to main to prepare for the new game.");

            // finish all activities except Main
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    private class CancelListener implements DialogInterface.OnClickListener{

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            Log.d(TAG, "Canceling new game start.");
            // Set back to the previous position
            treasureListView.setSelection(selectedPosition);
        }
    }
    
    private Boolean isProviderEnabled(String provider){
        boolean isOn = locationManager.isProviderEnabled(provider);
        Log.d(TAG, provider + " is " + (isOn ? "on" : "off"));
        return isOn;
    }
}
