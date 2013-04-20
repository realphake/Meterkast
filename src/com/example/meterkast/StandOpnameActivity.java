package com.example.meterkast;

import android.app.Activity;

import android.content.Intent;

import android.content.pm.ActivityInfo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import android.net.Uri;

import android.os.Bundle;

import android.provider.MediaStore;

import android.view.View;

import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;


/**
 * This window has the main function of entering the meter data. Also, it allows the user to take a picture of their meter for
 * reference.
 *
 * @author Arjen Swellengrebel
 */
public class StandOpnameActivity extends Activity {
    private static final int SAMPLESIZE = 8;
	StandData data;
	EditText editText1, editText2;
    ImageView imageView1;
    Beeldherkenning recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stand_opname);
        
        this.data = new StandData(getSharedPreferences("PersonalInfo", MODE_PRIVATE), getSharedPreferences("MeterInfo", MODE_PRIVATE));

        this.recognizer = new Beeldherkenning();

        // Initialize the textfield variables so they can be read/set.
        this.editText1 = (EditText) findViewById(R.id.editText1);
        this.editText2 = (EditText) findViewById(R.id.editText2);
        this.imageView1 = (ImageView) findViewById(R.id.imageView1);

        // If you have selected single meters, don't show the second text window.
        if (data.enkeleStand()) {
            this.editText2.setVisibility(View.INVISIBLE); // Out of sight, but not out of mind. Never forget.
        }

        // This function will load up a previously taken picture and suggest settings for the textfields.
        loadPict();
    }

	

    /**
     * Go back to the main screen
     *
     * @param view
     */
    public void goBack(View view) {
        super.onBackPressed(); // Simulates pressing the phone's back key.
    }

    /**
     * It's supposed to load the image, if any, and also fill in the text boxes with suggestions.
     */
    public void loadPict() {
        // Find the relevant picture
        File currentRec = data.getRelevantPhotoPath();

        Bitmap bitmap = null;

        try {
            bitmap = BitmapFactory.decodeFile(currentRec.getAbsolutePath());
        } catch (OutOfMemoryError oomERR) {
            // oomERR.printStackTrace();
            System.gc(); // Clean system resources and try again.

            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = SAMPLESIZE;

                bitmap = BitmapFactory.decodeFile(currentRec.getAbsolutePath(), options);
            } catch (OutOfMemoryError oomERR2) {
                // oomERR2.printStackTrace();
            }
        }

        if (bitmap != null) {
            if (bitmap.getHeight() < bitmap.getWidth()) {
                // Picture is in landscape mode. We rotate it now
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            this.imageView1.setImageBitmap(bitmap);
        }

        // If it exists, put the picture in currentRec in imageView1.
        // Also, see if you can determine what numbers go in the boxes.
        if (currentRec.exists()) {
            // Set suggestions based on the imageRecognizer(TM) (Just kidding about the (TM)) (For now)
            this.recognizer.execute(this.imageView1, this.editText1, this.editText2, currentRec,
                BitmapFactory.decodeResource(getResources(), R.drawable.numbersonblackblur));
        }
    }

    /**
     * Save settings, then go back to the main screen
     *
     * @param view
     */
    public void saveNumbers(View view) {

        data.makeRecording(this.editText1.getText().toString(), this.editText2.getText().toString(),getApplicationContext());
        super.onBackPressed(); // After saving, close the window.
    }

    

    /**
     * Should take a picture. This will call a separate app to take the picture, then return here. Is called when the user presses
     * the "take picture" button.
     *
     * @param view
     */
    public void takePic(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); // Create the intent

        File rPP = data.getRelevantPhotoPath();

        takePictureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(rPP)); // Give a path to the intent.

        startActivityForResult(takePictureIntent, 1);

        loadPict(); // TODO this isn't called, I think. Why not?
    }
}
