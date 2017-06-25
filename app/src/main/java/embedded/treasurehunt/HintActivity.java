package embedded.treasurehunt;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class HintActivity extends AppCompatActivity {

    private ImageView image;
    private TextView instructions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hint);

        instructions = (TextView) findViewById((R.id.instructionText));

        final Button compassButton = (Button)findViewById(R.id.compassButton);
        compassButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showCompass();
                Log.d("onCompassClick", "compass button clicked.");
            }
        });

    }

    private void setupHint(){
        //TODO first remove previous image? or just replace somehow?
        //instructions.setText(hint.getInstructions());
        //if (hint.getImage() != null){
        //    image = new ImageView(this);
        //    image.setLayoutParams(new android.view.ViewGroup.LayoutParams(80,60));
        //    image.setMaxHeight(20);
        //    image.setMaxWidth(20);
//
//            // Adds the view to the layout
//            layout.addView(image);
//        }
    }

    private void showCompass(){
        //TODO switch activity
        Intent intentLocation = new Intent(this, CompassActivity.class);
        this.startActivity(intentLocation);
    }
}
