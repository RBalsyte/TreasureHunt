package embedded.treasurehunt;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

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

    private final String TAG = "GestureActivity";

    // ui
    private TextView alertTextView;
    private TextView failedTextView;
    private Button tryAgainButton;
    private CustomAlertDialog counterAlertDialog;
    private final List<String> counterStrings = Arrays.asList("3", "2", "1", "GO!");
    private int count = 0;

    // model objects and variables
    Treasure treasure;
    private int currentHintPos;
    private GestureType gestureType;
    private boolean isLastHint;

    // sensors
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor magnetometerSensor;
    private Sensor proximitySensor;

    private boolean success = false;

    // shake variables
    private long lastUpdate;
    private float last_x,last_y,last_z;
    private static final int SHAKE_THRESHOLD = 1000;

    // rotation variables
    private int orientationValue = ORIENTATION_PORTRAIT;
    private int nextOrientation = ORIENTATION_LANDSCAPE_REVERSE;
    int rotationCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture);

        CustomAlertDialog errorDialog = new CustomAlertDialog(GestureActivity.this,
                "Something went wrong with the game. Try to start a new game.",
                getString(R.string.ok), new CloseAppListener());

        Intent intent = getIntent();
        treasure = (Treasure)intent.getSerializableExtra("treasure");
        currentHintPos = intent.getIntExtra("currentHintPos", -1);

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
        isLastHint = currentHintPos == (treasure.getHints().size() - 1);

        Hint currentHint = treasure.getHints().get(currentHintPos);
        if (currentHint == null){
            Log.d(TAG, "Hint is null");
            errorDialog.show();
            return;
        }

        gestureType = currentHint.getGestureType();
        if (gestureType == GestureType.None){
            errorDialog.show();
        }

        failedTextView = (TextView) findViewById(R.id.failedTextView);
        tryAgainButton = (Button) findViewById(R.id.tryAgainButton);
        if (failedTextView == null || tryAgainButton == null){
            errorDialog.show();
            return;
        }

        failedTextView.setVisibility(View.INVISIBLE);
        tryAgainButton.setVisibility(View.INVISIBLE);
        tryAgainButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startCounter();
                Log.d("onClick", "Try again button clicked.");
            }
        });

        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        alertTextView = new TextView(this);
        counterAlertDialog = new CustomAlertDialog(this, alertTextView);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "Gesture Activity paused");
        stop();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Gesture Activity resumed");
        start();
        super.onResume();
    }

    private void startCounter(){
        count = 0;
        counterAlertDialog.show();

        new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                alertTextView.setText(counterStrings.get(count));
                count++;
            }

            @Override
            public void onFinish() {
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

    private void startGestureTracking(){
        if (gestureType == GestureType.Shake){
            new GestureTimer(new ShakeSensorEventListener(), accelerometerSensor, null).start();
        }
        else if (gestureType == GestureType.Rotate){
            new GestureTimer(new OrientationSensorListener(), accelerometerSensor, magnetometerSensor).start();
        }
        else if (gestureType == GestureType.Clap){
            new GestureTimer( new ProximityListener(), proximitySensor, null).start();
        }
    }

    private void registerListener(SensorEventListener listener, Sensor sensor1, Sensor sensor2){
        sensorManager.registerListener(listener, sensor1, SensorManager.SENSOR_DELAY_UI);
		if(sensor2 != null)
            sensorManager.registerListener(listener, sensor2, SensorManager.SENSOR_DELAY_UI);
    }

    private void unregisterListener(SensorEventListener listener, Sensor sensor1, Sensor sensor2){
        sensorManager.unregisterListener(listener, sensor1);
		if(sensor2 != null)
            sensorManager.unregisterListener(listener, sensor2);
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

                    float x = sensorEvent.values[0];
                    float y = sensorEvent.values[1];
                    float z = sensorEvent.values[2];

                    float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                    if (speed > SHAKE_THRESHOLD) {
                        Log.d(TAG, "Shake detected w/ speed: " + speed);
                        success = true;
                    }
                    last_x = x;
                    last_y = y;
                    last_z = z;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {}
    }

    private class OrientationSensorListener implements SensorEventListener {
        DescriptiveStatistics pitchAvg;
        DescriptiveStatistics rollAvg;

        float[] mGravity;
        float[] mGeomagnetic;

        OrientationSensorListener() {
            pitchAvg = new DescriptiveStatistics(5);
            rollAvg = new DescriptiveStatistics(5);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values;
            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success2 = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success2) {
                    float orientationData[] = new float[3];
                    SensorManager.getOrientation(R, orientationData);
                    pitchAvg.addValue(Math.toDegrees(orientationData[1]));
                    rollAvg.addValue(Math.toDegrees(orientationData[2]));
                    update();
                }
            }
        }

        private void update() {
            if (((orientationValue == ORIENTATION_PORTRAIT || orientationValue == ORIENTATION_PORTRAIT_REVERSE)
                    && (rollAvg.getMean() > -30 && rollAvg.getMean() < 30))) {
                if (pitchAvg.getMean() > 0) {
                    orientationValue = ORIENTATION_PORTRAIT_REVERSE;
                    Log.d("sensor", "Orientation: " + ORIENTATION_PORTRAIT_REVERSE);
                } else {
                    orientationValue = ORIENTATION_PORTRAIT;
                    Log.d("sensor", "Orientation: " + ORIENTATION_PORTRAIT);
                }
            } else {
                // divides between all orientations
                if (Math.abs(pitchAvg.getMean()) >= 30) {
                    if (pitchAvg.getMean() > 0) {
                        orientationValue = ORIENTATION_PORTRAIT_REVERSE;
                        Log.d("sensor", "Orientation: " + ORIENTATION_PORTRAIT_REVERSE);
                    } else {
                        orientationValue = ORIENTATION_PORTRAIT;
                        Log.d("sensor", "Orientation: " + ORIENTATION_PORTRAIT);
                    }
                } else {
                    if (rollAvg.getMean() > 0) {
                        orientationValue = ORIENTATION_LANDSCAPE_REVERSE;
                        Log.d("sensor", "Orientation: " + ORIENTATION_LANDSCAPE_REVERSE);
                    } else {
                        orientationValue = ORIENTATION_LANDSCAPE;
                        Log.d("sensor", "Orientation: " + ORIENTATION_LANDSCAPE);
                    }
                }
            }

            if (orientationValue == nextOrientation) {
                switch (orientationValue) {
                    case ORIENTATION_PORTRAIT:
                        nextOrientation = ORIENTATION_LANDSCAPE_REVERSE;
                        rotationCount++;
                        break;
                    case ORIENTATION_PORTRAIT_REVERSE:
                        nextOrientation = ORIENTATION_LANDSCAPE;
                        rotationCount++;
                        break;
                    case ORIENTATION_LANDSCAPE_REVERSE:
                        nextOrientation = ORIENTATION_PORTRAIT_REVERSE;
                        rotationCount++;
                        break;
                    case ORIENTATION_LANDSCAPE:
                        nextOrientation = ORIENTATION_PORTRAIT;
                        rotationCount++;
                        break;
                    default:
                        break;
                }
            }

            if (rotationCount == 4){
                Log.d(TAG, "Rotation successful");
                success = true;
            }

            Log.d(TAG, "Rotation count: " + rotationCount);
        }
    }

    private class ProximityListener implements SensorEventListener {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {

            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                int sensitivity = 4;
                if (event.values[0] >= -sensitivity && event.values[0] <= sensitivity) {
                    success = true;
                }
            }
        }
    }

    private class GestureTimer extends CountDownTimer{
        SensorEventListener listener;
        Sensor sensor1;
        Sensor sensor2;

        GestureTimer(SensorEventListener listener, Sensor sensor1, Sensor sensor2){
            super(4000, 1000);
            this.listener = listener;
            this.sensor1 = sensor1;
            this.sensor2 = sensor2;
            success = false;
            registerListener(listener, sensor1, sensor2);
        }
        @Override
        public void onTick(long millisUntilFinished) {}

        @Override
        public void onFinish() {

            if (!success){
                Log.d(TAG, "User failed to do the gesture");
                failedTextView.setText(getString(R.string.failed));
                failedTextView.setVisibility(View.VISIBLE);
                tryAgainButton.setVisibility(View.VISIBLE);
            }else{
                Log.d(TAG, "User succeeded to finish the gesture");
                if (isLastHint){
                    new CustomAlertDialog(GestureActivity.this,  getString(R.string.gameFinished), getString(R.string.ok), new StartNewHintListener()).show();
                }else{
                    new CustomAlertDialog(GestureActivity.this,  getString(R.string.nextHint), getString(R.string.proceed), new StartNewHintListener()).show();
                }
            }
            unregisterListener(listener, sensor1, sensor2);
        }
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
}
