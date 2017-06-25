package embedded.treasurehunt;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private List<Integer> treasuresids;
    private List<String> treasuresNames = new ArrayList<>();
    private ListView treasureListView;
    private ArrayAdapter<String> adapter;
    private int selectedTreasure = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getTreasuresLists();

        treasureListView = (ListView)findViewById(R.id.treasuresList);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, treasuresNames);
        treasureListView.setAdapter(adapter);
        treasureListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        treasureListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                selectedTreasure = treasuresids.get(position);
                Log.d("STATE", "You selected treasure number: " + selectedTreasure);
            }
        });

                final Button startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(selectedTreasure < 0){
                    // show error that no treasure has been chosen
                    return;
                }
                start();
                Log.d("start button clicked.", "onStartClick");
            }
        });

    }

    private void start(){
        Intent intentMain = new Intent(this, HintActivity.class);
        intentMain.putExtra("treasureId", selectedTreasure);
        this.startActivity(intentMain);
        Log.i("Layout changed to hint ","Main layout ");
    }

    private void getTreasuresLists(){
        AsyncHttpClient client = new AsyncHttpClient();
        JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<Integer>>(){}.getType();
                treasuresids = gson.fromJson(response.toString(), listType);
                Log.d("STATUS", "Got treasures"+treasuresids.toString());
            }
        };
        responseHandler.setUsePoolThread(true);

        client.get("http://192.168.0.102:8151/treasures/ids", null, responseHandler);

        JsonHttpResponseHandler responseHandler2 = new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<String>>(){}.getType();
                treasuresNames = gson.fromJson(response.toString(), listType);
                updateList();
                Log.d("STATUS", "Got treasures"+treasuresNames.toString());
            }
        };
        responseHandler2.setUsePoolThread(true);

        client.get("http://192.168.0.102:8151/treasures/names", null, responseHandler2);
    }



    private void updateList() {
        runOnUiThread(new Runnable() {
            public void run() {
                adapter.addAll(treasuresNames);
                adapter.notifyDataSetChanged();
            }
        });
    }
}
