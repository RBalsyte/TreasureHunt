package embedded.treasurehunt;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.widget.TextView;

class CustomAlertDialog{

    private AlertDialog alertDialog;

    CustomAlertDialog(Context context, TextView view) {
        makeAlertDialog(context, view, null, null, null, null);
    }

    CustomAlertDialog(Context context, TextView view, String okButtonText, DialogInterface.OnClickListener okButtonListener) {
        makeAlertDialog(context, view, okButtonText, okButtonListener, null, null);
    }

    CustomAlertDialog(Context context, String textViewText, String okButtonText, DialogInterface.OnClickListener okButtonListener) {
        TextView textView = new TextView(context);
        textView.setText(textViewText);

        makeAlertDialog(context, textView, okButtonText, okButtonListener, null, null);
    }

    CustomAlertDialog(Context context, String textViewText,
                                String okButtonText, DialogInterface.OnClickListener okButtonListener,
                                String cancelButtonText, DialogInterface.OnClickListener cancelButtonListener) {
        TextView textView = new TextView(context);
        textView.setText(textViewText);
        makeAlertDialog(context, textView, okButtonText, okButtonListener, cancelButtonText, cancelButtonListener);
    }

    private void makeAlertDialog(Context context, TextView view, String okButtonText, DialogInterface.OnClickListener okButtonListener,
                                 String cancelButtonText, DialogInterface.OnClickListener cancelButtonListener){
        AlertDialog.Builder adb = new AlertDialog.Builder(context);

        view.setGravity(Gravity.CENTER);
        view.setTypeface(Typeface.MONOSPACE);
        view.setTextSize(18);

        adb.setView(view);

        if (okButtonText != null) {
            adb.setIcon(android.R.drawable.ic_dialog_alert);
            if (okButtonListener != null)
                adb.setPositiveButton(okButtonText, okButtonListener);
        }

        if (cancelButtonText != null && cancelButtonListener != null) {
            adb.setNegativeButton(cancelButtonText,cancelButtonListener);
            if (okButtonListener != null)
                adb.setPositiveButton(okButtonText, okButtonListener);
        }

        alertDialog = adb.create();
        alertDialog.setCancelable(false);
    }

    void show(){
        if (alertDialog != null){
            alertDialog.show();
        }
    }

    void hide(){
        if (alertDialog != null){
            alertDialog.hide();
        }
    }

    void cancel(){
        if (alertDialog != null){
            alertDialog.cancel();
        }
    }
}
