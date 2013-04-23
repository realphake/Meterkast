package com.example.meterkast;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import android.graphics.Bitmap.CompressFormat;
import android.graphics.Paint.Align;

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
        
        if ( data.getSettingSelection("nOfRecords") >= 2 ) {
        	drawBarGraphs(); // Look at the functions to see what they do
            graphRecordings();
        }
        
    }

    // Draws a graph of the electricity usage over time. 
    private void drawBarGraphs() {
    	Bitmap graph = BitmapFactory.decodeResource(getResources(), R.drawable.grid);
        graph = Bitmap.createScaledBitmap(graph, WIDTH, HEIGHT, false);
    	// I just make the data do it right now; it has access to the necessary data to do it.
    	graph = drawBarGraphs(WIDTH, HEIGHT, graph);
        this.imageView1.setImageBitmap(graph);
    }

    
    /**
	 *  Similar to the drawGraphs function, but drawing the daily usage (one bar each day.)
	 */
	public Bitmap drawBarGraphs(int width, int height, Bitmap graph) { 
		// It takes a graph because it will be drawing over previously drawn stuff.
		int noRecs = data.getSettingSelection("nOfRecords");
        long startTime = data.getDate(1); // Again harvesting some values first.
        long endTime = data.getEndDate();
        long duration = endTime - startTime;
        int totalDays = (int) (duration / RecordingData.MILLISINADAY);
        int highestRecorded = 0;

        // Find the highest of all recordings...
        for (int i = 1; i <= noRecs; i++) {
            String frst = "frst";
			int thisRecordingFRST = data.getRecording(i, frst);
            int thisRecordingSCND = data.getRecording(i, "scnd");

            if (thisRecordingFRST + thisRecordingSCND > highestRecorded) { // The highest of all TOTALS, that is
                highestRecorded = thisRecordingFRST + thisRecordingSCND;
            }
        }

        for (int day = 0; day < totalDays; day += 1) {
            long morning = startTime + (day * RecordingData.MILLISINADAY); // Starting time for the beginning of the current day
            long evening = startTime + (day + 1) * RecordingData.MILLISINADAY; // And 24 hours later, or the end
            int usage = data.thisDaysUsage(morning, evening);

            // Fractions are between 0 and 1, and they help us view the image in percentages.
            double morningWidthFraction = ((double) morning / (double) duration);
            double eveningWidthFraction = ((double) evening / (double) duration);
            double daysRecFrac = (((double) usage) / ((double) highestRecorded));

            // Then convert them to pixels by multiplying with the image's width.
            int morningWidth = (int) (morningWidthFraction * width);
            int eveningWidth = (int) (eveningWidthFraction * width);
            int usageHeight = height - (int) (daysRecFrac * height);

            Canvas canvas = new Canvas(graph);
            Paint p = new Paint();
            p.setColor(Color.RED);// And for each day, I draw one rectangle for its usage.
            canvas.drawRect(new Rect(morningWidth, usageHeight, eveningWidth, height), p);
        }
        
        return graph;
	}
    
    // Draws bar graphs on the picture being displayed. These graphs are SUPER USEFUL.
    private void graphRecordings() {
    	Bitmap graph = BitmapFactory.decodeResource(getResources(), R.drawable.grid);
        graph = Bitmap.createScaledBitmap(graph, WIDTH, HEIGHT, false);
        graph = drawGraph(WIDTH, HEIGHT, graph); // Make data do it, because it has all the information for that.

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
    
 // Draw a graph showing the progression of electricity usage in this household.
    public Bitmap drawGraph(int width, int height, Bitmap graph) {

        int noRecs = data.getSettingSelection("nOfRecords");

        int highestRecorded = 0;
        int lowestRecorded = RecordingData.HIGHESTPOSSIBLERECORDING;
        
        // Find the highest recording and the lowest recording.
        // These values are used to determine the bounding of the image.
        for (int i = 1; i <= noRecs; i++) {
            int thisRecordingFRST = data.getRecording(i, "frst");
            int thisRecordingSCND = data.getRecording(i, "scnd");

            if (thisRecordingFRST + thisRecordingSCND > highestRecorded) {
            	// Keep updating the values. 
                highestRecorded = thisRecordingFRST + thisRecordingSCND;
            }

            if (thisRecordingFRST < lowestRecorded) {
                lowestRecorded = thisRecordingFRST; 
            }

            if (thisRecordingSCND < lowestRecorded) {
                lowestRecorded = thisRecordingSCND;
            }
        }

        // Determine the necessary variables. These are used for the left and right bounds of the image.
        long startTime = data.getDate(1);
        long endTime = data.getDate(Integer.valueOf(noRecs));

        int previousTime = 0;
        int duration = (int) endTime - (int) startTime;
        double recordingSpan = highestRecorded - lowestRecorded;

        // For the second recording and all recordings after it:
        for (int i = 2; i <= noRecs; i++) { // Start at two because we consider recordings in pairs, each with its preceding.
            
        	// In the coming block I'm going to calculate a whole load of variables...
        	int thisTime = (int) (data.getDate(i) - startTime);
            int thisRecordingFRST = data.getRecording(i, "frst");
            int thisRecordingSCND = data.getRecording(i,"scnd");
            int iPrev = (i - 1);
            int previousRecordingFRST = data.getRecording(iPrev ,"frst");
            int previousRecordingSCND = data.getRecording(iPrev , "scnd");

            // This is the part of the image the time falls on:
            double previousWidthFraction = ((double) previousTime / (double) duration);
            double thisWidthFraction = ((double) thisTime / (double) duration);

            // And the part of the image the recording's value falls on. 0 <= X <= 1
            double previousRecFractionFRST = (((double) previousRecordingFRST - (double) lowestRecorded) / recordingSpan);
            double previousRecFractionSCND = (((double) previousRecordingSCND - (double) lowestRecorded) / recordingSpan);
            double thisRecFractionFRST = (((double) thisRecordingFRST - (double) lowestRecorded) / recordingSpan);
            double thisRecFractionSCND = (((double) thisRecordingSCND - (double) lowestRecorded) / recordingSpan);

            // And based on the fractions, here are the pixels that make the locations on the image:
            int drawX1 = (int) (width * previousWidthFraction);
            int drawX2 = (int) (width * thisWidthFraction);

            int drawY1FRST = (int) (height - (height * previousRecFractionFRST));
            int drawY1SCND = (int) (height - (height * previousRecFractionSCND));
            int drawY1COMBO = (int) (height - (height * (previousRecFractionSCND + previousRecFractionFRST)));
            int drawY2FRST = (int) (height - (height * thisRecFractionFRST));
            int drawY2SCND = (int) (height - (height * thisRecFractionSCND));
            int drawY2COMBO = (int) (height - (height * (thisRecFractionSCND + thisRecFractionFRST)));

            // Now then, drawing:
            Canvas canvas = new Canvas(graph);
            Paint p = new Paint();
            p.setColor(Color.BLUE); // Blue is nightly usage;
            // Draw lines between two datapoints. Each datapoint is some combinations of the above variables.
            canvas.drawLine(drawX1, drawY1SCND, drawX2, drawY2SCND, p);
            canvas.drawLine(drawX1, drawY1SCND - 1, drawX2, drawY2SCND - 1, p);
            p.setColor(Color.GREEN); // Green is daily usage,
            canvas.drawLine(drawX1, drawY1FRST, drawX2, drawY2FRST, p);
            canvas.drawLine(drawX1, drawY1FRST - 1, drawX2, drawY2FRST - 1, p);
            p.setColor(Color.BLUE);// And here I draw both colors once, meaning this is combined usage.
            canvas.drawLine(drawX1, drawY1COMBO, drawX2, drawY2COMBO, p);
            p.setColor(Color.GREEN);
            canvas.drawLine(drawX1, drawY1COMBO - 1, drawX2, drawY2COMBO - 1, p);

            p.setColor(Color.RED);
            p.setTextSize(30);
            p.setTextAlign(Align.CENTER);
            // Then I note the upper and lower bounds of the recordings in the image, for reference.
            canvas.drawText(Integer.toString(highestRecorded), width / 2, RecordingData.TOPTEXTDISTANCE, p);
            canvas.drawText(Integer.toString(lowestRecorded), width / 2, height - RecordingData.BOTTOMTEXTDISTANCE, p);

            previousTime = thisTime; // Update the time for the loop. Then do it again.
        }
		return graph;
    }
}
