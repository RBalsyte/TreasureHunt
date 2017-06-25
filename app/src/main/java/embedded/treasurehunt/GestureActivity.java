package embedded.treasurehunt;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import embedded.treasurehunt.model.GestureType;
import embedded.treasurehunt.model.Hint;
import embedded.treasurehunt.model.Treasure;

public class GestureActivity extends AppCompatActivity{

    private final int ORIENTATION_PORTRAIT = ExifInterface.ORIENTATION_ROTATE_90; // 6
    private final int ORIENTATION_LANDSCAPE_REVERSE = ExifInterface.ORIENTATION_ROTATE_180; // 3
    private final int ORIENTATION_LANDSCAPE = ExifInterface.ORIENTATION_NORMAL; // 1
    private final int ORIENTATION_PORTRAIT_REVERSE = ExifInterface.ORIENTATION_ROTATE_270; // 8

    private static final int FROM_RADS_TO_DEGS = -57;
    private final List<String> counterStrings = Arrays.asList("3", "2", "1", "GO!");
    private int count = 0;
    private AlertDialog counterAlertDialog;
    private TextView alertTextView;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
	private Sensor rotationSensor;

    private TextView failedTextView;
    private Button tryAgainButton;

    private long lastUpdate;
    private float x,y,z;
    private float last_x,last_y,last_z;
    private float[] values = new float[3];

    private static final int SHAKE_THRESHOLD = 1000;
    private boolean success = false;
	private int orientationValue = ORIENTATION_PORTRAIT;

    Treasure treasure;
    private int currentHintPos;
    private Hint currentHint;
    private GestureType gestureType;

    private boolean isLastHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture);

        Intent intent = getIntent();
        treasure = (Treasure)intent.getSerializableExtra("treasure");
        currentHintPos = intent.getIntExtra("currentHintPos", -1);
        currentHint = treasure.getHints().get(currentHintPos);
        //gestureType = currentHint.getGestureType();
        gestureType = GestureType.Shake;
        isLastHint = currentHintPos == (treasure.getHints().size() - 1);

        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        failedTextView = (TextView) findViewById(R.id.failedTextView);
        failedTextView.setVisibility(View.INVISIBLE);
        tryAgainButton = (Button) findViewById(R.id.tryAgainButton);
        tryAgainButton.setVisibility(View.INVISIBLE);
        tryAgainButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startCounter();
                Log.d("onClick", "Try again button clicked.");
            }
        });

        alertTextView = new TextView(this);
        counterAlertDialog = makeAlertDialog(alertTextView, false, null, null);
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
            new GestureTimer(new ShakeSensorEventListener(), accelerometerSensor).start();
        }
        else if (gestureType == GestureType.Rotate){
            new GestureTimer(new OrientationSensorListener(), rotationSensor).start();
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

    private void registerListener(SensorEventListener listener, Sensor sensor){
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
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);
    }

    private void unregisterListener(SensorEventListener listener, Sensor sensor){
        sensorManager.unregisterListener(listener, sensor);
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

    private class OrientationSensorListener implements SensorEventListener{
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor == rotationSensor) {
                if (event.values.length > 4) {
                    float[] truncatedRotationVector = new float[4];
                    System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4);
                    update(truncatedRotationVector);
                } else {
                    update(event.values);
                }
            }
        }

        private void update(float[] vectors) {
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, vectors);

            int worldAxisX = SensorManager.AXIS_X;
            int worldAxisZ = SensorManager.AXIS_Z;
            float[] adjustedRotationMatrix = new float[9];
            SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustedRotationMatrix);

            float[] orientation = new float[3];
            SensorManager.getOrientation(adjustedRotationMatrix, orientation);

            float pitch = (float) Math.toDegrees(orientation[1]);
            float roll = (float) Math.toDegrees(orientation[2]);
            double yaw = (float) Math.toDegrees(orientation[0]);

            if (((orientationValue == ORIENTATION_PORTRAIT || orientationValue == ORIENTATION_PORTRAIT_REVERSE)
                    && (roll > -30 && roll < 30))) {
                if (pitch > 0)
                    Log.d("sensor", "Orientation: " +  ORIENTATION_PORTRAIT_REVERSE);
                else
                    Log.d("sensor", "Orientation: " +  ORIENTATION_PORTRAIT);
            } else {
                // divides between all orientations
                if (Math.abs(pitch) >= 30) {
                    if (pitch > 0)
                        Log.d("sensor", "Orientation: " +  ORIENTATION_PORTRAIT_REVERSE);
                    else
                        Log.d("sensor", "Orientation: " +  ORIENTATION_PORTRAIT);
                } else {
                    if (roll > 0) {
                        Log.d("sensor", "Orientation: " +  ORIENTATION_LANDSCAPE_REVERSE);
                    } else {
                        Log.d("sensor", "Orientation: " +  ORIENTATION_LANDSCAPE);
                    }
                }
            }

            //Log.d("sensor", "Orientation pitch: " + pitch);
            Log.d("sensor", "Orientation roll: " + roll);
            //Log.d("sensor", "Orientation yaw: " + yaw);

        }
    }

    private class GestureTimer extends CountDownTimer{
        SensorEventListener listener;
        Sensor sensor;

        GestureTimer(SensorEventListener listener, Sensor sensor){
            super(5000, 1000);
            this.listener = listener;
            this.sensor = sensor;
            success = false;
            registerListener(listener, sensor);
        }
        @Override
        public void onTick(long millisUntilFinished) {}

        @Override
        public void onFinish() {

            if (!success){
                Log.d("onFinish", "failed");
                failedTextView.setText("You failed to do the correct gesture.");
                failedTextView.setVisibility(View.VISIBLE);
                tryAgainButton.setVisibility(View.VISIBLE);
            }else{
                TextView finishView = new TextView(GestureActivity.this);
                String textToDisplay;
                String okButtonText;
                Log.d("CurrentHintPos", "" + currentHintPos);
                Log.d("IsLastHint", "" + isLastHint);
                if (isLastHint){
                    textToDisplay = "Congratulations, you have finished the game!";
                    okButtonText = "OK";
                }else{
                    textToDisplay = "Great, you have finished this task.";
                    okButtonText = "Proceed";
                }
                finishView.setText(textToDisplay);
                makeAlertDialog(finishView, true, okButtonText, new StartNewHintListener()).show();
                Log.d("onFinish", "succeeded");
            }
            unregisterListener(listener, sensor);
        }
    }

    private AlertDialog makeAlertDialog(TextView view, boolean addOKButton, String okButtonText, DialogInterface.OnClickListener okButtonListener){
        AlertDialog.Builder adb = new AlertDialog.Builder(this);

        view.setGravity(Gravity.CENTER);
        view.setTextSize(32);
        adb.setView(view);

        if (addOKButton){
            adb.setIcon(android.R.drawable.ic_dialog_alert);
            adb.setPositiveButton(okButtonText, okButtonListener);
        }

        AlertDialog alertDialog = adb.create();
        alertDialog.setCancelable(false);
        return alertDialog;
    }

    private class StartNewHintListener implements DialogInterface.OnClickListener{

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            // increment hint position, pass it on to the Compass Activity and finish this one
            currentHintPos++;
            GestureActivity.this.setResult(Activity.RESULT_OK, new Intent().putExtra("newHintPos", currentHintPos));
            GestureActivity.this.finish();
        }
    }
}
