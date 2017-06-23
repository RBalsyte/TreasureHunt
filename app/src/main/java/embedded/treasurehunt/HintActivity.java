package embedded.treasurehunt;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;

public class HintActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hint);

        final TextView instructionTextView = (TextView) findViewById((R.id.instructionText));
        // TODO set instruction text
        instructionTextView.setText( "Instructions:\n" + "blah blah blah");

        final Button mapButton = (Button)findViewById(R.id.mapButton);
        mapButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showMap();
                Log.d("onMapClick", "map button clicked.");
            }
        });

        final Button compassButton = (Button)findViewById(R.id.compassButton);
        compassButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showCompass();
                Log.d("onCompassClick", "compass button clicked.");
            }
        });

        final Button checkLocationButton = (Button)findViewById(R.id.checkLocationButton);
        checkLocationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkLocation();
                Log.d("onCheckLocationClick", "check location button clicked.");
            }
        });

    }

    private void showCompass(){
        //TODO switch activity
    }

    private void showMap(){
        //TODO switch activity
    }

    private void checkLocation(){
        //TODO switch activity
        Intent intentLocation = new Intent(this, LocationActivity.class);
        this.startActivity(intentLocation);
        Log.d("Hint layout ", "Layout changed to location.");
    }
}
