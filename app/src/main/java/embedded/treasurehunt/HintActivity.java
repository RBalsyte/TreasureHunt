package embedded.treasurehunt;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import embedded.treasurehunt.model.Hint;
import embedded.treasurehunt.model.Treasure;

public class HintActivity extends AppCompatActivity {

    private ImageView image;
    private TextView instructions;
    private Treasure treasure;
    private Hint currentHint;
    private int currentHintPos = 0;
    private int selectedId = -1;
    private Gson customGson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hint);

        Intent intent = getIntent();
        selectedId = intent.getIntExtra("treasureId", -1);

        customGson = new GsonBuilder().registerTypeHierarchyAdapter(byte[].class,
                new ByteArrayToBase64TypeAdapter()).create();

        getTreasure();

        instructions = (TextView) findViewById((R.id.instructionText));
        image = (ImageView) findViewById((R.id.hintImage));

        final Button compassButton = (Button)findViewById(R.id.compassButton);
        compassButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showCompass();
                Log.d("onCompassClick", "compass button clicked.");
            }
        });
    }

    private void showCompass(){
        Intent intentLocation = new Intent(this, CompassActivity.class);
        intentLocation.putExtra("treasure", treasure);
        intentLocation.putExtra("currentHintPos", currentHintPos);
        this.startActivity(intentLocation);
    }

    private void getTreasure(){
        if(selectedId < 0){
            // show toast error
            Log.e("STATUS", "Invalid selected id");
            return;
        }

        AsyncHttpClient client = new AsyncHttpClient();
        JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Type listType = new TypeToken<Treasure>(){}.getType();
                treasure = customGson.fromJson(response.toString(), listType);
                Log.d("STATUS", "Got treasure"+treasure.toString());
                updateData();
            }
        };
        responseHandler.setUsePoolThread(true);

        client.get("http://192.168.0.102:8151/treasures/" + selectedId, null, responseHandler);
    }

    private void updateData() {
        runOnUiThread(new Runnable() {
            public void run() {
                currentHint = treasure.getHints().get(currentHintPos);
                if(currentHint != null){
                    instructions.setText(currentHint.getInstructions());
                    Bitmap bm = BitmapFactory.decodeByteArray(currentHint.getImage(), 0, currentHint.getImage().length);
                    image.setImageBitmap(bm);
                }
            }
        });
    }

    class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Base64.decode(json.getAsString(), Base64.NO_WRAP);
        }

        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.encodeToString(src, Base64.NO_WRAP));
        }
    }
}
