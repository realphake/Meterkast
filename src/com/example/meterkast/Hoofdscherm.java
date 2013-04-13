package com.example.meterkast;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import android.graphics.Bitmap;

import android.graphics.Bitmap.CompressFormat;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import android.graphics.Paint.Align;

import android.graphics.Rect;

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
    SharedPreferences settings, recordings; // The info files used by the app.
    TextView textView1;
    ImageView imageView1;

    @Override
    /** This is called when the app starts.
     * Initializes the window, opens the settings files,
     * also checks whether the options have been set and if not prompts user to do so. */
    protected void onCreate(Bundle savedInstanceState) {
        // initialize the main screen itself.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hoofdscherm);

        this.settings = getSharedPreferences("PersonalInfo", MODE_PRIVATE); // Open settings file.
        this.recordings = getSharedPreferences("MeterInfo", MODE_PRIVATE); // Open data storage file.
        this.textView1 = (TextView) findViewById(R.id.textView1);
        this.imageView1 = (ImageView) findViewById(R.id.imageView1);

        // See if all settings have been recorded.
        if (this.settings.getInt("Woonsituatie", -1) == -1 || this.settings.getInt("Inwoneraantal", -1) == -1
                || this.settings.getInt("Metersoort", -1) == -1) {
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
        int noRecs = this.settings.getInt("nOfRecords", 0);
        long startTime = this.recordings.getLong("1date", -1);
        long endTime = this.recordings.getLong(Integer.valueOf(noRecs).toString() + "date", -1);
        long duration = endTime - startTime;
        long millisInADay = (1000 * 60 * 60 * 24);
        int totalDays = (int) (duration / millisInADay);
        int highestRecorded = 0;
        int width = 640;
        int height = 480;
        Bitmap graph = BitmapFactory.decodeResource(getResources(), R.drawable.grid);
        graph = Bitmap.createScaledBitmap(graph, width, height, false);

        for (int i = 1; i <= noRecs; i++) {
            int thisRecordingFRST = this.recordings.getInt(i + "frst", -1);
            int thisRecordingSCND = this.recordings.getInt(i + "scnd", -1);

            if (thisRecordingFRST + thisRecordingSCND > highestRecorded) {
                highestRecorded = thisRecordingFRST + thisRecordingSCND;
            }
        }

        for (int day = 0; day < totalDays; day += 1) {
            long morning = startTime + (day * millisInADay);
            long evening = startTime + (day + 1) * millisInADay;
            int usage = thisDaysUsage(morning, evening);

            double morningWidthFraction = ((double) morning / (double) duration);
            double eveningWidthFraction = ((double) evening / (double) duration);
            double daysRecFrac = (((double) usage) / ((double) highestRecorded));

            int morningWidth = (int) (morningWidthFraction * width);
            int eveningWidth = (int) (eveningWidthFraction * width);
            int usageHeight = height - (int) (daysRecFrac * height);

            Canvas canvas = new Canvas(graph);
            Paint p = new Paint();
            p.setColor(Color.RED);
            canvas.drawRect(new Rect(morningWidth, usageHeight, eveningWidth, height), p);
        }

        this.imageView1.setImageBitmap(graph);
    }

    private int thisDaysUsage(long morning, long evening) {
        int noRecs = this.settings.getInt("nOfRecords", 0);
        long thisDate;
        int beforeMorning = -1;
        int afterEvening = -1;

        for (int recording = 2; recording <= noRecs; recording += 1) {
            thisDate = this.recordings.getLong(recording + "date", -1);

            if (beforeMorning == -1 && morning < thisDate) {
                beforeMorning = recording - 1;
            }

            if (afterEvening == -1 && evening < thisDate) {
                afterEvening = recording;
            }

            if (afterEvening != -1 && beforeMorning != -1) {
                break;
            }
        }

        long millisInDay = 1000 * 60 * 60 * 24;
        long millisBetweenRecs = this.recordings.getLong(afterEvening + "date", -1)
            - this.recordings.getLong(beforeMorning + "date", -1);
        int usageBetwRecsFRST = this.recordings.getInt(afterEvening + "frst", -1)
            - this.recordings.getInt(beforeMorning + "frst", -1);
        int usageBetwRecsSCND = this.recordings.getInt(afterEvening + "scnd", -1)
            - this.recordings.getInt(beforeMorning + "scnd", -1);
        int usageBetweenRecs = usageBetwRecsFRST + usageBetwRecsSCND;
        int usageThisDay = (int) ((double) (millisInDay / millisBetweenRecs) * usageBetweenRecs);

        return usageThisDay;
    }

    private void graphRecordings() {
        int noRecs = this.settings.getInt("nOfRecords", 0);

        int highestRecorded = 0;
        int lowestRecorded = 999999 * 2;

        for (int i = 1; i <= noRecs; i++) {
            int thisRecordingFRST = this.recordings.getInt(i + "frst", -1);
            int thisRecordingSCND = this.recordings.getInt(i + "scnd", -1);

            if (thisRecordingFRST + thisRecordingSCND > highestRecorded) {
                highestRecorded = thisRecordingFRST + thisRecordingSCND;
            }

            if (thisRecordingFRST < lowestRecorded) {
                lowestRecorded = thisRecordingFRST;
            }

            if (thisRecordingSCND < lowestRecorded) {
                lowestRecorded = thisRecordingSCND;
            }
        }

        long startTime = this.recordings.getLong("1date", -1);

        long endTime = this.recordings.getLong(Integer.valueOf(noRecs).toString() + "date", -1);
        int width = 640, height = 480;

        Bitmap graph = BitmapFactory.decodeResource(getResources(), R.drawable.grid);
        graph = Bitmap.createScaledBitmap(graph, width, height, false);

        int previousTime = 0;
        int duration = (int) endTime - (int) startTime;
        double recordingSpan = highestRecorded - lowestRecorded;

        for (int i = 2; i <= noRecs; i++) {
            int thisTime = (int) (this.recordings.getLong(i + "date", -1) - startTime);
            int thisRecordingFRST = this.recordings.getInt(i + "frst", -1);
            int thisRecordingSCND = this.recordings.getInt(i + "scnd", -1);
            int iPrev = (i - 1);
            int previousRecordingFRST = this.recordings.getInt(iPrev + "frst", -1);
            int previousRecordingSCND = this.recordings.getInt(iPrev + "scnd", -1);

            double previousWidthFraction = ((double) previousTime / (double) duration);
            double thisWidthFraction = ((double) thisTime / (double) duration);

            double previousRecFractionFRST = (((double) previousRecordingFRST - (double) lowestRecorded) / recordingSpan);
            double previousRecFractionSCND = (((double) previousRecordingSCND - (double) lowestRecorded) / recordingSpan);
            double thisRecFractionFRST = (((double) thisRecordingFRST - (double) lowestRecorded) / recordingSpan);
            double thisRecFractionSCND = (((double) thisRecordingSCND - (double) lowestRecorded) / recordingSpan);

            int drawX1 = (int) (width * previousWidthFraction);
            int drawX2 = (int) (width * thisWidthFraction);

            int drawY1FRST = (int) (height - (height * previousRecFractionFRST));
            int drawY1SCND = (int) (height - (height * previousRecFractionSCND));
            int drawY1COMBO = (int) (height - (height * (previousRecFractionSCND + previousRecFractionFRST)));
            int drawY2FRST = (int) (height - (height * thisRecFractionFRST));
            int drawY2SCND = (int) (height - (height * thisRecFractionSCND));
            int drawY2COMBO = (int) (height - (height * (thisRecFractionSCND + thisRecFractionFRST)));

            Canvas canvas = new Canvas(graph);
            Paint p = new Paint();
            p.setColor(Color.BLUE);
            canvas.drawLine(drawX1, drawY1SCND, drawX2, drawY2SCND, p);
            canvas.drawLine(drawX1, drawY1SCND - 1, drawX2, drawY2SCND - 1, p);
            p.setColor(Color.GREEN);
            canvas.drawLine(drawX1, drawY1FRST, drawX2, drawY2FRST, p);
            canvas.drawLine(drawX1, drawY1FRST - 1, drawX2, drawY2FRST - 1, p);
            p.setColor(Color.BLUE);
            canvas.drawLine(drawX1, drawY1COMBO, drawX2, drawY2COMBO, p);
            p.setColor(Color.GREEN);
            canvas.drawLine(drawX1, drawY1COMBO - 1, drawX2, drawY2COMBO - 1, p);

            p.setColor(Color.RED);
            p.setTextSize(30);
            p.setTextAlign(Align.CENTER);
            canvas.drawText(Integer.toString(highestRecorded), width / 2, 0 + 30, p);
            canvas.drawText(Integer.toString(lowestRecorded), width / 2, height - 10, p);

            previousTime = thisTime;
        }

        this.imageView1.setImageBitmap(graph);
    }

    /**
     * Called when the window becomes active. Shows some info about previous recordings.
     */
    private void showRecordings() {
        // Find the number of previous recordings for reference.
        int noRecs = this.settings.getInt("nOfRecords", 0);
        String infoS = Integer.toString(noRecs); // To start the infobox with.

        // For each entry i that has a recording:
        for (int i = 1; i <= noRecs; i++) {
            String iS = Integer.valueOf(i).toString(); // Need this for reading the keys and also printing.
            Integer recI = Integer.valueOf(this.recordings.getInt(iS + "frst", -1)); // Find recording.
            int recI2 = this.recordings.getInt(iS + "scnd", -1);
            System.out.println(this.settings.getInt("Metersoort", -1));

            String entry;

            if (recI2 != -1 && this.settings.getInt("Metersoort", -1) == R.id.radioTweeStanden) {
                entry = iS + ": " + recI.toString() + ", " + Integer.valueOf(recI2).toString();
            } else {
                entry = iS + ": " + recI.toString();
            }

            infoS = infoS + "\n" + entry;
        }

        // Set the textView in the main screen to the number of prev. recordings.

        this.textView1.setText(infoS);
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
