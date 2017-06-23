package embedded.treasurehunt;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class NextHintActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_hint);

        final Button getNextHintButton = (Button)findViewById(R.id.nextHintButton);
        getNextHintButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getNextHint();
                Log.d("start button clicked.", "onStartClick");
            }
        });
    }

    private void getNextHint(){
        //TODO
    }
}