package embedded.treasurehunt;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

public class GestureActivity extends AppCompatActivity implements SensorEventListener{

    public enum Gesture{
        NONE,
        SHAKE,
        ROTATE,
        INFINITY;
    }

    private List<String> counterStrings;
    private int count;
    private AlertDialog counterAlertDialog;
    private TextView alertTextView;
    private Gesture taskGesture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture);

        counterStrings = Arrays.asList("3", "2", "1", "GO!");
        count = 0;

        AlertDialog.Builder popupBuilder = new AlertDialog.Builder(this);

        alertTextView = new TextView(this);
        alertTextView.setGravity(Gravity.CENTER);
        alertTextView.setTextSize(32);
        popupBuilder.setView(alertTextView);

        counterAlertDialog = popupBuilder.create();
        counterAlertDialog.setTitle("");
        counterAlertDialog.setCancelable(false);

        taskGesture = Gesture.SHAKE;
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

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

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
        if(taskGesture == Gesture.NONE){
            return true;
        }
        else if (taskGesture == Gesture.SHAKE){
            return shake();
        }
        else if (taskGesture == Gesture.ROTATE){
            return rotate();
        }
        else if (taskGesture == Gesture.INFINITY){
            return infinity();
        }
        return false;
    }

    private boolean shake(){
        return false;
    }

    private boolean rotate(){
        return false;
    }

    private boolean infinity(){
        return false;
    }
}
