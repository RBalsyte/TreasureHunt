package embedded.treasurehunt;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.Base64;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.lang.reflect.Type;

import cz.msebera.android.httpclient.Header;
import embedded.treasurehunt.model.Hint;
import embedded.treasurehunt.model.Treasure;

public class HintActivity extends AppCompatActivity {

    private static final int REQUEST_NEW_HINT_POSITION = 1;
    private static final String TAG = "HintActivity";

    private ImageView image;
    private TextView instructions;

    private Treasure treasure;
    private Hint currentHint;
    private int currentHintPos = 0;
    private int treasureId = -1;

    private Gson customGson;

    private boolean doneRetrieving = false;
    private CustomAlertDialog errorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hint);

        Intent intent = getIntent();
        treasureId = intent.getIntExtra("treasureId", -1);

        errorDialog = new CustomAlertDialog(HintActivity.this,
                "Something went wrong with the game. Try to start a new game.",
                getString(R.string.ok), new HintActivity.CloseAppListener());

        if(treasureId < 0){
            Log.e(TAG, "Invalid game id passed on from main.");
            errorDialog.show();
            return;
        }

        instructions = (TextView) findViewById((R.id.instructionText));
        image = (ImageView) findViewById((R.id.hintImage));
        final Button compassButton = (Button)findViewById(R.id.compassButton);

        if (instructions == null || image == null || compassButton == null){
            Log.d(TAG, "One of the ui elements is null");
            errorDialog.show();
            return;
        }

        compassButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showCompass();
            }
        });

        customGson = new GsonBuilder().registerTypeHierarchyAdapter(byte[].class,
                new ByteArrayToBase64TypeAdapter()).create();

        getTreasure();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Returned from Compass activity");
        if (requestCode == REQUEST_NEW_HINT_POSITION && resultCode == Activity.RESULT_OK) {
            int newHintPos = data.getIntExtra("newHintPos", 0);
            // finished the game? -> go back to main and finish this activity
            if (newHintPos == treasure.getHints().size()){
                Log.d(TAG, "Game finished. Closing");
                HintActivity.this.setResult(Activity.RESULT_OK, new Intent().putExtra("finished", true));
                HintActivity.this.finish();
            }
            else {
                Log.d(TAG, "Advancing to hint " + newHintPos);
                currentHintPos = newHintPos;
                doneRetrieving = false;
                updateData();
            }
        }
        // else compass activity was paused and this one doesn't need update
    }

    private void showCompass(){
        if (!doneRetrieving){
            Toast toast = Toast.makeText(HintActivity.this, "Still retrieving data from the database. Please try again in a bit.", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        if (currentHint == null){
            errorDialog.show();
            return;
        }

        Log.d(TAG, "Starting Compass Activity");
        Intent intent = new Intent(this, CompassActivity.class);
        intent.putExtra("treasure", treasure);
        intent.putExtra("currentHintPos", currentHintPos);
        startActivityForResult(intent, REQUEST_NEW_HINT_POSITION);
    }

    private void getTreasure(){
        doneRetrieving = false;
        AsyncHttpClient client = new AsyncHttpClient();
        JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Type listType = new TypeToken<Treasure>(){}.getType();
                treasure = customGson.fromJson(response.toString(), listType);
                Log.d(TAG, "Got treasure " + treasure.toString());
                updateData();
            }
        };
        responseHandler.setUsePoolThread(true);

        client.get("http://140.78.187.215:8151/treasures/" + treasureId, null, responseHandler);
    }

    private void updateData() {
        runOnUiThread(new Runnable() {
            public void run() {
                currentHint = treasure.getHints().get(currentHintPos);
                if (currentHint == null){
                    return;
                }
                instructions.setText(currentHint.getInstructions());
                if (currentHint.getImage() != null){
                    Bitmap bm = BitmapFactory.decodeByteArray(currentHint.getImage(), 0, currentHint.getImage().length);
                    image.setImageBitmap(bm);
                }else{
                    image.setImageResource(R.drawable.default_image);
                }
                doneRetrieving = true;
            }
        });
    }

    private class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Base64.decode(json.getAsString(), Base64.NO_WRAP);
        }

        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.encodeToString(src, Base64.NO_WRAP));
        }
    }

    private class CloseAppListener implements DialogInterface.OnClickListener{

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            HintActivity.this.finish();
        }
    }
}
