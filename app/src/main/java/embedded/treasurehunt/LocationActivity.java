package embedded.treasurehunt;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LocationActivity extends AppCompatActivity {
    private static final String TAG = "onLocationChanged";

    public enum Status{
        VERY_FAR("You are very far away :("),
        FAR("You are still far away :("),
        CLOSE("You are 100 meters away from the goal!"),
        ALMOST("You are almost there! Keep walking!"),
        GOAL("You found it! You may now perform the gesture as instructed to proceed :)");

        private String string;
        private Status(String string){
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    //TODO
    //private Hint hint = null;
    private LocationManager locationManager = null;
    private LocationListener locationListener = null;
    private TextView locationTextView = null;
    private TextView statusTextView = null;
    private Button gestureButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        locationTextView = (TextView) findViewById((R.id.locationText));
        statusTextView = (TextView) findViewById((R.id.statusText));
        gestureButton = (Button) findViewById((R.id.gestureButton));
        gestureButton.setEnabled(false);

        setupLocationManager();
    }

    @Override
    protected void onPause() {

        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }

        super.onPause();
        Log.d("onPause", "Locaton Activity paused");
    }

    @Override
    protected void onResume() {
        setupLocationManager();
        super.onResume();
        Log.d("onResume", "Locaton Activity resumed");
    }

    private void setupLocationManager(){
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isOn = isNetworkOn()&& isGPSOn();
        if (isOn){
            // check for permissions
            if (Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO show error msg
                return;
            }
            // request location updates every second independent from distance (0)
            // (minDistance -> minimum distance between location updates, in meters)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new GPSLocationListener());
        }
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

    private class GPSLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {

            double longitude = loc.getLongitude();
            double latitude = loc.getLatitude();
            locationTextView.setText("Longitude:" + longitude + "\nLatitude:" + latitude);

            //TODO calculate distance in meters from the goal in straight line
            float [] dist = new float[1];
            //Location.distanceBetween(longitude, latitude, hint.longitude, hint.latitude, dist);
            float distance = dist[0];
            if (distance < 2){
                statusTextView.setText(Status.GOAL.toString());
                gestureButton.setEnabled(true);
            }else if (distance < 20){
                statusTextView.setText(Status.ALMOST.toString());
                gestureButton.setEnabled(false);
            }else if (distance < 100){
                statusTextView.setText(Status.CLOSE.toString());
                gestureButton.setEnabled(false);
            }else if (distance < 500){
                statusTextView.setText(Status.FAR.toString());
                gestureButton.setEnabled(false);
            }else {
                statusTextView.setText(Status.VERY_FAR.toString());
                gestureButton.setEnabled(false);
            }

            Log.d(TAG, locationTextView.getText().toString());
            Log.d(TAG, statusTextView.getText().toString());
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub
        }
    }
}
