package com.example.meterkast;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Bitmap;

import android.graphics.Bitmap.CompressFormat;

import android.graphics.BitmapFactory;

import android.net.Uri;

import android.os.Bundle;
import android.os.Environment;

import android.view.View;

import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;


/**
 * All functions used by the main screen of the app.
 *
 * @author Arjen Swellengrebel
 */
public class Hoofdscherm extends Activity {
    // Global variables.
    TextView textView1;
    ImageView imageView1;
	StandData data;

    @Override
    /** This is called when the app starts.
     * Initializes the window, opens the settings files,
     * also checks whether the options have been set and if not prompts user to do so. */
    protected void onCreate(Bundle savedInstanceState) {
        // initialize the main screen itself.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hoofdscherm);

        this.data = new StandData(getSharedPreferences("PersonalInfo", MODE_PRIVATE), getSharedPreferences("MeterInfo", MODE_PRIVATE));
        
        this.textView1 = (TextView) findViewById(R.id.textView1);
        this.imageView1 = (ImageView) findViewById(R.id.imageView1);

        // See if all settings have been recorded.
        if (data.everyOptionIsSet()) {
            // Show a message saying we need to change some settings first.
            AlertDialog ad = new AlertDialog.Builder(this).create();

            // ad.setCancelable(false); // This blocks the 'BACK' button. Is that what we want?
            ad.setMessage(getString(R.string.welkom_bericht));

            // Make the OK-button:
            ad.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.ok_knop), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // When OK is clicked, direct the user to the Options menu...
                        goToOptions(findViewById(R.id.buttonToOptions));
                        dialog.dismiss(); // Then dismiss the dialog box.
                    }
                });
            ad.show(); // And finally show the message.
        }
    }

    @Override
    /** Function is called every time the main screen resumes.
     * Currently only used to update the info on recordings.
     */
    protected void onResume() {
        // Call the normal onResume function, so everything'll work as expected
        super.onResume();

        // Then add the following functions to that code:
        showRecordings();
        drawBarGraphs();
        graphRecordings();
    }

    private void drawBarGraphs() {
    	int width = 640;
        int height = 480;
    	Bitmap graph = BitmapFactory.decodeResource(getResources(), R.drawable.grid);
        graph = Bitmap.createScaledBitmap(graph, width, height, false);
    	
    	graph = data.drawBarGraphs(width, height, graph);
        this.imageView1.setImageBitmap(graph);
    }

    

    private void graphRecordings() {
    	int width = 640, height = 480;
    	Bitmap graph = BitmapFactory.decodeResource(getResources(), R.drawable.grid);
        graph = Bitmap.createScaledBitmap(graph, width, height, false);
        graph = data.drawGraph(width, height, graph);

        this.imageView1.setImageBitmap(graph);
    }

    /**
     * Called when the window becomes active. Shows some info about previous recordings.
     */
    private void showRecordings() {
        this.textView1.setText("User# "+data.getCurrentUser());
    }

    /**
     * Called when the user clicks the Record button Opens the Record screen.
     *
     * @param view
     */
    public void goToRecord(View view) {
        Intent intentToRecord = new Intent(this, StandOpnameActivity.class);
        startActivity(intentToRecord); // Opening the record screen.
    }

    /**
     * Called when the user clicks the Options button Opens the option-menu.
     *
     * @param view
     */
    public void goToOptions(View view) {
        Intent intentToOptions = new Intent(this, OptieMenuActivity.class);
        startActivity(intentToOptions); // Opening the options screen.
    }

    /**
     * Sets a timer that will shoot the user a message after some time has elapsed.
     *
     * @param view
     */
    public void scheduleNotification(View view) {
        startService(new Intent(this, NotificationService.class));
    }

    /**
     * @param view
     */
    public void shareSocialMedia(View view) {
        new File(Environment.getExternalStorageDirectory() + "/meterkast_foto/").mkdirs();

        String toShare = Environment.getExternalStorageDirectory().getAbsolutePath() + "/meterkast_foto/shareGraph.jpg";

        View content = findViewById(R.id.imageView1);
        content.setDrawingCacheEnabled(true);

        Bitmap bitmap = content.getDrawingCache();
        File file = new File(toShare);

        try {
            file.createNewFile();

            FileOutputStream ostream = new FileOutputStream(file);
            bitmap.compress(CompressFormat.JPEG, 100, ostream);
            ostream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/jpeg");

        share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + toShare));
        share.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message));

        startActivity(Intent.createChooser(share, getString(R.string.sharewindow_title)));
    }
}
