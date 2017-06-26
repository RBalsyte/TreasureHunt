package embedded.treasurehunt;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import embedded.treasurehunt.model.GestureType;
import embedded.treasurehunt.model.Hint;
import embedded.treasurehunt.model.Treasure;

/*
* We are using the GoogleApiClient for checking location, since it is now
* highly recommended.
* https://developer.android.com/training/location/index.html
* */

public class CompassActivity extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private enum Status{
        VERY_FAR("Very far"),
        FAR("Far"),
        CLOSE("Close"),
        ALMOST("Almost there!"),
        GOAL("You found it!");

        private String string;
        Status(String string){
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    private static final String TAG = "CompassActivity";
    private static final int REQUEST_PERFORM_GESTURE = 2;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int REQUEST_LOCATION = 1234;
    private static final long UPDATE_INTERVAL = 1500;  /* 1.5 secs */
    private static final long FASTEST_INTERVAL = 1000; /* 1 sec */


    private TextView latTextView;
    private TextView longTextView;
    private TextView statusTextView;
    private Button gestureButton;
    private ImageView arrowView;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor magnetometerSensor;
    private SensorEventListener arrowListener;

    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float azimuth = 0f;
    private float currectAzimuth = 0;

    private Treasure treasure;
    private int currentHintPos;
    private Hint hint;

    private CustomAlertDialog errorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        Intent intent = getIntent();
        treasure = (Treasure)intent.getSerializableExtra("treasure");
        currentHintPos = intent.getIntExtra("currentHintPos", -1);

        errorDialog = new CustomAlertDialog(CompassActivity.this,
                "Something went wrong with the game. Try to start a new game.",
                getString(R.string.ok), new CloseAppListener());

        latTextView = (TextView) findViewById(R.id.latTextView);
        longTextView = (TextView) findViewById(R.id.longTextView);
        statusTextView = (TextView) findViewById(R.id.statusTextView);
        Button mapButton = (Button) findViewById(R.id.mapButton);
        gestureButton = (Button) findViewById(R.id.gestureButton);
        arrowView = (ImageView) findViewById(R.id.compassArrow);

        if (latTextView == null || longTextView == null || statusTextView == null || mapButton == null
                || gestureButton == null || arrowView == null){
            Log.d(TAG, "One of the ui elements is null");
            errorDialog.show();
            return;
        }

        if (treasure == null){
            Log.d(TAG, "Treasure is null");
            errorDialog.show();
            return;
        }
        if (currentHintPos < 0 || currentHintPos >=treasure.getHints().size()){
            Log.d(TAG, "Invalid currentHintPos");
            errorDialog.show();
            return;
        }
        hint = treasure.getHints().get(currentHintPos);
        if (hint == null){
            Log.d(TAG, "Hint is null");
            errorDialog.show();
            return;
        }

        mapButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showMap();
            }
        });

        gestureButton.setVisibility(View.INVISIBLE);
        gestureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startGesture();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();

        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        arrowListener = new ArrowListener();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "Compass Activity paused");
        stop();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Compass Activity resumed");
        start();
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Returned from GestureActivity");
        if (requestCode == REQUEST_PERFORM_GESTURE && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Gesture activity successfully returned. Starting new hint");
            // pass the new current hint position to the hint activity
            int newHintPos = data.getIntExtra("newHintPos", -1);
            CompassActivity.this.setResult(Activity.RESULT_OK, new Intent().putExtra("newHintPos", newHintPos));
            CompassActivity.this.finish();
        }
        // else gesture activity was paused
    }

    private void start(){
        sensorManager.registerListener(arrowListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(arrowListener, magnetometerSensor, SensorManager.SENSOR_DELAY_UI);

        mGoogleApiClient.connect();
    }

    private void stop(){

        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        sensorManager.unregisterListener(arrowListener, accelerometerSensor);
        sensorManager.unregisterListener(arrowListener, magnetometerSensor);
    }

    private void adjustArrow() {
        //Log.d(TAG, "will set rotation from " + currectAzimuth + " to "+ azimuth);

        Animation an = new RotateAnimation(-currectAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        currectAzimuth = azimuth;

        an.setDuration(100);
        an.setRepeatCount(0);
        an.setFillAfter(true);

        arrowView.startAnimation(an);
    }

    private void showMap(){
        //TODO
    }

    private void startGesture(){
        // just in case check. Otherwise the button should not be visible
        if (hint.getGestureType() == GestureType.None){
            return;
        }

        Log.d(TAG, "Starting gesture activity");
        Intent intent = new Intent(this, GestureActivity.class);
        intent.putExtra("treasure", treasure);
        intent.putExtra("currentHintPos", currentHintPos);

        startActivityForResult(intent, REQUEST_PERFORM_GESTURE);
    }

    private double calculateDistance(Location currentLocation, double latitude2, double longitude2){
        int radius = 6371; // Radius of the earth in km
        double latitudeDelta = deg2rad(latitude2-currentLocation.getLatitude());
        double longitudeDelta = deg2rad(longitude2-currentLocation.getLongitude());

        // Theory from http://www.movable-type.co.uk/scripts/latlong.html
        // calculation must be done in radians
        // haversine = sin²(Δφ/2) + cos φ1 ⋅ cos φ2 ⋅ sin²(Δλ/2), where φ is latitude, λ is longitude
        // c = 2 ⋅ atan2( √a, √(1−a) )
        // d = R ⋅ c
        double a = Math.sin(latitudeDelta/2) * Math.sin(latitudeDelta/2)
                + Math.cos(deg2rad(currentLocation.getLatitude())) * Math.cos(deg2rad(latitude2))
                * Math.sin(longitudeDelta/2) * Math.sin(longitudeDelta/2);

        double b = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = radius * b * 1000; //distance in meters
        return distance;
    }

    private double deg2rad(double degrees){
        return degrees * (Math.PI/180);
    }

    private class CloseAppListener implements DialogInterface.OnClickListener{

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            Log.d(TAG, "Error in the app. Closing all activities up to main to prepare for the new game.");

            // finish all activities except Main
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    private class ArrowListener implements SensorEventListener{

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            final float alpha = 0.97f;

            synchronized (this) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    mGravity[0] = alpha * mGravity[0] + (1 - alpha) * sensorEvent.values[0];
                    mGravity[1] = alpha * mGravity[1] + (1 - alpha) * sensorEvent.values[1];
                    mGravity[2] = alpha * mGravity[2] + (1 - alpha) * sensorEvent.values[2];
                }

                if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * sensorEvent.values[0];
                    mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * sensorEvent.values[1];
                    mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * sensorEvent.values[2];
                }

                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);
                    azimuth = (float) Math.toDegrees(orientation[0]); // orientation
                    azimuth = (azimuth + 360) % 360;
                    adjustArrow();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }


    /**
     * LOCATION
     */
    private class StartNewHintListener implements DialogInterface.OnClickListener{

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            // increment hint position, pass it on to the Compass Activity and finish this one
            currentHintPos++;
            CompassActivity.this.setResult(Activity.RESULT_OK, new Intent().putExtra("newHintPos", currentHintPos));
            CompassActivity.this.finish();
        }
    }

    protected void startLocationUpdates(){
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request permissions at runtime
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CompassActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        } else {
            // Request location updates
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    mLocationRequest, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // New location has now been determined
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        updateLocation(location);
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(this, "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        //TODO handle exceptions
        //If the error has a resolution, send an Intent to
        //start a Google Play services activity to resolve error.
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Request permissions at runtime
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CompassActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }

        // Get last known recent location.
        Location lastKnown = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        // Note that this can be NULL if last location isn't already known.
        if (lastKnown != null) {
            // Print current location if not null
            Log.d(TAG, "current location: " + lastKnown.toString());
            mCurrentLocation = lastKnown;
        }
        startLocationUpdates();
    }

    private void updateLocation(Location newLocation){
        mCurrentLocation = newLocation;
        latTextView.setText(String.format( "%.2f", mCurrentLocation.getLatitude() ));
        longTextView.setText(String.format( "%.2f", mCurrentLocation.getLongitude() ));
        Log.d(TAG, "Latitude" + mCurrentLocation.getLatitude());
        Log.d(TAG, "Longitude" + mCurrentLocation.getLongitude());

        //TODO reenable
        //double distance = calculateDistance(mCurrentLocation, hint.getLatitude(), hint.getLongitude());
        double distance = 0;
        if (distance < 2){
            statusTextView.setText(Status.GOAL.toString());

            if (hint.getGestureType() == GestureType.None){
                TextView finishView = new TextView(CompassActivity.this);
                String textToDisplay;
                String okButtonText;
                if (currentHintPos == (treasure.getHints().size() - 1)){
                    textToDisplay = getString(R.string.gameFinished);
                    okButtonText = getString(R.string.ok);
                }else{
                    textToDisplay = getString(R.string.nextHint);
                    okButtonText = getString(R.string.proceed);
                }
                finishView.setText(textToDisplay);
                Log.d("onFinish", "succeeded");
                new CustomAlertDialog(CompassActivity.this, finishView, okButtonText, new StartNewHintListener()).show();
                return;
            }

            gestureButton.setVisibility(View.VISIBLE);
        }else if (distance < 20){
            statusTextView.setText(Status.ALMOST.toString());
            gestureButton.setVisibility(View.INVISIBLE);
        }else if (distance < 100){
            statusTextView.setText(Status.CLOSE.toString());
            gestureButton.setVisibility(View.INVISIBLE);
        }else if (distance < 500){
            statusTextView.setText(Status.FAR.toString());
            gestureButton.setVisibility(View.INVISIBLE);
        }else {
            statusTextView.setText(Status.VERY_FAR.toString());
            gestureButton.setVisibility(View.INVISIBLE);
        }
    }
}