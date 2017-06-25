package embedded.treasurehunt;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import embedded.treasurehunt.model.GestureType;
import embedded.treasurehunt.model.Hint;
import embedded.treasurehunt.model.Treasure;

public class GestureActivity extends AppCompatActivity{

    private final List<String> counterStrings = Arrays.asList("3", "2", "1", "GO!");
    private int count = 0;
    private AlertDialog counterAlertDialog;
    private TextView alertTextView;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;

    private long lastUpdate;
    private float x,y,z;
    private float last_x,last_y,last_z;
    private float[] values = new float[3];

    private static final int SHAKE_THRESHOLD = 1000;
    private boolean success = false;

    Treasure treasure;
    private int currentHintPos;
    private Hint currentHint;
    private GestureType gestureType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture);

        Intent intent = getIntent();
        treasure = (Treasure)intent.getSerializableExtra("treasure");
        currentHintPos = intent.getIntExtra("currentHintPos", -1);
        currentHint = treasure.getHints().get(currentHintPos);
        gestureType = currentHint.getGestureType();

        

        AlertDialog.Builder popupBuilder = new AlertDialog.Builder(this);
        alertTextView = new TextView(this);
        alertTextView.setGravity(Gravity.CENTER);
        alertTextView.setTextSize(32);
        popupBuilder.setView(alertTextView);

        counterAlertDialog = popupBuilder.create();
        counterAlertDialog.setTitle("");
        counterAlertDialog.setCancelable(false);

        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onPause() {
        stop();
        Log.d("onPause", "Gesture Activity paused");
        super.onPause();
    }

    @Override
    protected void onResume() {
        start();
        Log.d("onResume", "Gesture Activity resumed");
        super.onResume();
    }

    private void startCounter(){
        count = 0;
        counterAlertDialog.show();

        new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d("onTick", counterStrings.get(count));
                alertTextView.setText(counterStrings.get(count));
                count++;
            }

            @Override
            public void onFinish() {
                Log.d("onFinish", "finished.");
                counterAlertDialog.hide();
                startGestureTracking();
            }
        }.start();
    }

    private void start(){
        startCounter();
    }

    private void stop(){
        counterAlertDialog.cancel();
    }

    private boolean startGestureTracking(){
        if(gestureType == GestureType.None){
            return true;
        }
        else if (gestureType == GestureType.Shake){
            new GestureTimer(new ShakeSensorEventListener()).start();
        }
        else if (gestureType == GestureType.Rotate){
            return rotate();
        }
        else if (gestureType == GestureType.Eight){
            return infinity();
        }
        return false;
    }

    private boolean rotate(){
        return false;
    }

    private boolean infinity(){
        return false;
    }

    private void registerListener(SensorEventListener listener){
        int finePermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int coarsePermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        Log.d("FinePermission", finePermissionCheck + "");
        Log.d("CoarsePermission", finePermissionCheck + "");
        // check for permissions
        if (finePermissionCheck != PackageManager.PERMISSION_GRANTED
                || coarsePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            //TODO
            return;
        }
        sensorManager.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
    }

    private void unregisterListener(SensorEventListener listener){
        sensorManager.unregisterListener(listener, accelerometerSensor);
    }

    private class ShakeSensorEventListener implements SensorEventListener{

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long curTime = System.currentTimeMillis();
                // only allow one update every 100ms.
                if ((curTime - lastUpdate) > 100) {
                    long diffTime = (curTime - lastUpdate);
                    lastUpdate = curTime;

                    x = sensorEvent.values[0];
                    y = sensorEvent.values[1];
                    z = sensorEvent.values[2];

                    float speed = Math.abs(x+y+z - last_x - last_y - last_z) / diffTime * 10000;

                    if (speed > SHAKE_THRESHOLD) {
                        Log.d("sensor", "shake detected w/ speed: " + speed);
                        success = true;
                    }
                    last_x = x;
                    last_y = y;
                    last_z = z;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

    private class GestureTimer extends CountDownTimer{
        SensorEventListener listener;

        GestureTimer(SensorEventListener listener){
            super(3000, 1000);
            this.listener = listener;
            success = false;
            registerListener(listener);
        }
        @Override
        public void onTick(long millisUntilFinished) {}

        @Override
        public void onFinish() {

            if (!success){


            }else{
                //TODO show another dialog
            }
            unregisterListener(listener);
        }
    }

    private AlertDialog.Builder makeOkCancelAlertDialog(String title){
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(title);

        TextView view = new TextView(this);
        view.setGravity(Gravity.CENTER);
        view.setTextSize(32);
        adb.setView(view);

        adb.setIcon(android.R.drawable.ic_dialog_alert);
        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                    //TODO restart
            } });

        adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //TODO do not restart
            } });

        return adb;
    }

}
