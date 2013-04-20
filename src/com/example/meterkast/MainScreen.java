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
public class MainScreen extends Activity {
    private static final int QUALITY = 100;
	private static final int HEIGHT = 480; // Height of the graph that displays your usage.
	private static final int WIDTH = 640;// And width of same.
	// Global variables.
    TextView textView1;
    ImageView imageView1;
	RecordingData data;

    @Override
    /** This is called when the app starts.
     * Initializes the window, opens the settings files,
     * also checks whether the options have been set and if not prompts user to do so. */
    protected void onCreate(Bundle savedInstanceState) {
        // initialize the main screen itself.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hoofdscherm);

        // Initialize the main way we'll communicate with the settings.
        this.data = new RecordingData(getSharedPreferences("PersonalInfo", MODE_PRIVATE), 
        		getSharedPreferences("MeterInfo", MODE_PRIVATE));
        
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
        showCurrentUser();
        drawBarGraphs(); // Look at the functions to see what they do
        graphRecordings();
    }

    // Draws a graph of the electricity usage over time. 
    private void drawBarGraphs() {
    	Bitmap graph = BitmapFactory.decodeResource(getResources(), R.drawable.grid);
        graph = Bitmap.createScaledBitmap(graph, WIDTH, HEIGHT, false);
    	// I just make the data do it right now; it has access to the necessary data to do it.
    	graph = data.drawBarGraphs(WIDTH, HEIGHT, graph);
        this.imageView1.setImageBitmap(graph);
    }

    
    // Draws bar graphs on the picture being displayed. These graphs are SUPER USEFUL.
    private void graphRecordings() {
    	Bitmap graph = BitmapFactory.decodeResource(getResources(), R.drawable.grid);
        graph = Bitmap.createScaledBitmap(graph, WIDTH, HEIGHT, false);
        graph = data.drawGraph(WIDTH, HEIGHT, graph); // Make data do it, because it has all the information for that.

        this.imageView1.setImageBitmap(graph);
    }

    /**
     * Called when the window becomes active. Shows some info about previous recordings.
     */
    private void showCurrentUser() {
        this.textView1.setText("User# "+data.getCurrentUser());
    }

    /**
     * Called when the user clicks the Record button Opens the Record screen.
     *
     * @param view
     */
    public void goToRecord(View view) {
        Intent intentToRecord = new Intent(this, MakeRecordingActivity.class);
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
        startService(new Intent(this, NotificationService.class)); // Just refers to the class I made for that.
    }

    /**
     * Shares the picture of the graph on any social media, like facebook or twitter.
     */
    public void shareSocialMedia(View view) {
        new File(Environment.getExternalStorageDirectory() + "/meterkast_foto/").mkdirs();

        // First I save the (temporary) picture too disk so I can refer to it.
        String toShare = Environment.getExternalStorageDirectory().getAbsolutePath() + "/meterkast_foto/shareGraph.jpg";
        View content = findViewById(R.id.imageView1);
        content.setDrawingCacheEnabled(true);
        Bitmap bitmap = content.getDrawingCache();
        File file = new File(toShare); // Create a file type to save to (in a moment)

        // This may fail, but it really shouldn't. It's just Java being a stickler for exceptions.
        try {
            file.createNewFile();
            FileOutputStream ostream = new FileOutputStream(file);
            bitmap.compress(CompressFormat.JPEG, QUALITY, ostream); // Actually save the image in the file!
            ostream.close();
        } catch (Exception e) {
            e.printStackTrace(); // This throws away info if it does crash, but whatevs guys!
        }

        // Setting up for sharing.
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/jpeg");

        // This is why I saved the image to file just now. The share intent needs a URI, not an actual picture.
        share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + toShare));
        share.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message)); // And a quick optional message.

        // And SHARE!
        startActivity(Intent.createChooser(share, getString(R.string.sharewindow_title)));
    }
}
