package embedded.treasurehunt;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import embedded.treasurehunt.model.Hint;
import embedded.treasurehunt.model.Treasure;

public class CompassActivity extends AppCompatActivity implements SensorEventListener, LocationListener {


    public enum Status{
        VERY_FAR("Very far"),
        FAR("Far"),
        CLOSE("Close"),
        ALMOST("Almost there!"),
        GOAL("You found it!");

        private String string;
        private Status(String string){
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    private static final String TAG = "Compass";
    private static final int REQUEST_PERFORM_GESTURE = 2;

    //TODO
    //private Hint hint = null;

    private TextView latTextView;
    private TextView longTextView;
    private TextView statusTextView;
    private Button gestureButton;
    private Button mapButton;
    private ImageView arrowView;

    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor magnetometerSensor;

    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float azimuth = 0f;
    private float currectAzimuth = 0;

    private Treasure treasure;
    private int currentHintPos;
    private Hint currentHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        Intent intent = getIntent();
        treasure = (Treasure)intent.getSerializableExtra("treasure");
        currentHintPos = intent.getIntExtra("currentHintPos", -1);
        currentHint = treasure.getHints().get(currentHintPos);

        latTextView = (TextView) findViewById(R.id.latTextView);
        longTextView = (TextView) findViewById(R.id.longTextView);
        statusTextView = (TextView) findViewById(R.id.statusTextView);
        mapButton = (Button) findViewById(R.id.mapButton);
        mapButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showMap();
                Log.d("onClick", "Map button clicked.");
            }
        });
        gestureButton = (Button) findViewById(R.id.gestureButton);
        gestureButton.setVisibility(View.INVISIBLE);
        gestureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startGesture();
                Log.d("onClick", "Gesture button clicked.");
            }
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        arrowView = (ImageView) findViewById(R.id.compassArrow);
    }

    @Override
    protected void onPause() {
        stop();
        Log.d("onPause", "Compass Activity paused");
        super.onPause();
    }

    @Override
    protected void onResume() {
        start();
        Log.d("onResume", "Compass Activity resumed");
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("onActivityResult", "returned from GestureActivity");
        if (requestCode == REQUEST_PERFORM_GESTURE && resultCode == Activity.RESULT_OK) {
            // pass the new current hint position to the hint activity
            int newHintPos = data.getIntExtra("newHintPos", 0);
            CompassActivity.this.setResult(Activity.RESULT_OK, new Intent().putExtra("newHintPos", newHintPos));
            CompassActivity.this.finish();
        }
        // else it paused
    }

    private void adjustArrow() {
        if (arrowView == null) {
            Log.e(TAG, "arrow view is not set");
            return;
        }

        //Log.d(TAG, "will set rotation from " + currectAzimuth + " to "+ azimuth);

        Animation an = new RotateAnimation(-currectAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        currectAzimuth = azimuth;

        an.setDuration(100);
        an.setRepeatCount(0);
        an.setFillAfter(true);

        arrowView.startAnimation(an);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.97f;

        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2];
            }

            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0];
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1];
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2];
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

    @Override
    public void onLocationChanged(Location location) {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        longTextView.setText(String.format( "%.2f", longitude ));
        latTextView.setText(String.format( "%.2f", latitude ));
        Log.d("onLocationChanged", "Latitude" + latitude);
        Log.d("onLocationChanged", "Longitude" + longitude);

        //TODO calculate distance in meters from the goal in straight line
        float [] dist = new float[1];
        //Location.distanceBetween(longitude, latitude, hint.longitude, hint.latitude, dist);
        float distance = dist[0];
        if (distance < 2){
            statusTextView.setText(Status.GOAL.toString());
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

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private void start(){

        boolean isOn = isNetworkOn() && isGPSOn();
        if (isOn){
            int finePermissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            int coarsePermissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION);
            Log.d("FinePermission", finePermissionCheck + "");
            Log.d("CoarsePermission", finePermissionCheck + "");
            // check for permissions
            if (finePermissionCheck != PackageManager.PERMISSION_GRANTED
                    || coarsePermissionCheck != PackageManager.PERMISSION_GRANTED) {
                //ActivityCompat.requestPermissions(this,
                  //      new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    //    1340);

                //TODO
                return;
            }

            // request location updates every second independent from distance (0)
            // (minDistance -> minimum distance between location updates, in meters)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);

            sensorManager.registerListener(CompassActivity.this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(CompassActivity.this, magnetometerSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void stop(){
        locationManager.removeUpdates(this);
        sensorManager.unregisterListener(this, accelerometerSensor);
        sensorManager.unregisterListener(this, magnetometerSensor);
    }

    private Boolean isNetworkOn(){
        //TODO show error msg
        Boolean isOn = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.d("isNetworkOn", isOn.toString());
        return isOn;
    }

    private Boolean isGPSOn(){
        //TODO show error msg
        Boolean isOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.d("isGPSOn", isOn.toString());
        return isOn;
    }

    private void showMap(){
        //TODO
    }

    private void startGesture(){
        //TODO switch activity
        Intent intent = new Intent(this, GestureActivity.class);
        intent.putExtra("treasure", treasure);
        intent.putExtra("currentHintPos", currentHintPos);

        startActivityForResult(intent, REQUEST_PERFORM_GESTURE);
    }
}