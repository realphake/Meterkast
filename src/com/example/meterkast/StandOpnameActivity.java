package com.example.meterkast;

import android.app.Activity;

import android.content.Intent;
import android.content.SharedPreferences;

import android.content.pm.ActivityInfo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import android.net.Uri;

import android.os.Bundle;
import android.os.Environment;

import android.provider.MediaStore;

import android.view.View;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

import dalvik.system.DexFile;


/**
 * This window has the main function of entering the meter data. Also, it allows the user to take a picture of their meter for
 * reference.
 *
 * @author Arjen Swellengrebel
 */
public class StandOpnameActivity extends Activity {
    StandData data;
	EditText editText1, editText2;
    ImageView imageView1;
    Beeldherkenning recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stand_opname);
        
        this.data = new StandData();

        this.data.settings = getSharedPreferences("PersonalInfo", MODE_PRIVATE); // Open settings file.
        this.data.recordings = getSharedPreferences("MeterInfo", MODE_PRIVATE); // Open data storage file.

        this.recognizer = new Beeldherkenning();

        // Initialize the textfield variables so they can be read/set.
        this.editText1 = (EditText) findViewById(R.id.editText1);
        this.editText2 = (EditText) findViewById(R.id.editText2);
        this.imageView1 = (ImageView) findViewById(R.id.imageView1);

        // If you have selected single meters, don't show the second text window.
        if (this.data.settings.getInt("Metersoort", -1) == R.id.radioEnkeleStand) {
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
        File currentRec = getRelevantPhotoPath();

        Bitmap bitmap = null;

        try {
            bitmap = BitmapFactory.decodeFile(currentRec.getAbsolutePath());
        } catch (OutOfMemoryError oomERR) {
            // oomERR.printStackTrace();
            System.gc(); // Clean system resources and try again.

            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 8;

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
    public void saveNumbers(View view) { // TODO Breaks now? But why?

        // Create an editor object for writing to the settings file.
        SharedPreferences.Editor editor = this.data.recordings.edit();
        SharedPreferences.Editor editorSettings = this.data.settings.edit();

        // Counting the recordings, we need the next one so + 1
        Integer recordNumber = Integer.valueOf(this.data.settings.getInt("nOfRecords", 0) + 1);

        /** Recordings will look like this:
         * "3date" => 39485093218475 (milliseconds since epoch)
         * "3frst" => 503341
         * "3scnd" => 403215
         *
         * Picture filenames look like "/meterkast_foto/3pict.jpg"
         */

        /** Record the first field, if available. */
        if (!this.editText1.getText().toString().equals("")) {
            // Enter the recorded info into the "editor" (which is the file "MeterInfo.whatever").
            long currentTime = System.currentTimeMillis();

            editor.putLong(recordNumber.toString() + "date", currentTime);
            
            // editor.putLong(recordNumber.toString() + "date", System.currentTimeMillis());
            editor.putInt(recordNumber.toString() + "frst", Integer.parseInt(this.editText1.getText().toString()));

            /** Record the second field, if necessary. */
            if (!this.editText2.getText().toString().equals("")
                    && this.data.settings.getInt("Metersoort", -1) == R.id.radioTweeStanden) {
                // Enter the recorded info into the "editor" (which is the file "MeterInfo.whatever").
                editor.putInt(recordNumber.toString() + "scnd", Integer.parseInt(this.editText2.getText().toString()));

                // We have to remember how many settings have been recorded now!
                editorSettings.putInt("nOfRecords", recordNumber.intValue());
            }

            // We have to remember how many settings have been recorded now!
            editorSettings.putInt("nOfRecords", recordNumber.intValue());
        } else {
            Toast.makeText(getApplicationContext(), "Incompleet ingevuld!", Toast.LENGTH_SHORT).show();
        }

        editorSettings.commit();
        editor.commit();
        super.onBackPressed(); // After saving, close the window.
    }

    /**
     * This gives the full path to the picture that we currently want to look at. For example, if the last recording was numbered
     * "2", this will give a path to "3pict.jpg". It also creates the "meterkast_foto" folder if it doesn't exist yet.
     *
     * @return
     */
    private File getRelevantPhotoPath() {
        // Create a file in the following steps:
        // Determine the filename for this one, such as "3pict.jpg" (where 3 is the number of the next (this) recording)
        String numPict = Integer.valueOf(this.data.settings.getInt("nOfRecords", 0) + 1).toString() + "pict.jpg";

        // Create the directory like "/sdcard/meterkast_foto/" if it doesn't exist yet
        new File(Environment.getExternalStorageDirectory() + "/meterkast_foto/").mkdirs();

        // Combine to determine the full path, such as "/sdcard/meterkast_foto/3pict.jpg"
        File fullPath = new File(Environment.getExternalStorageDirectory() + "/meterkast_foto/" + numPict);

        return fullPath; // Return that.
    }

    /**
     * Should take a picture. This will call a separate app to take the picture, then return here. Is called when the user presses
     * the "take picture" button.
     *
     * @param view
     */
    public void takePic(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); // Create the intent

        File rPP = getRelevantPhotoPath();

        takePictureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(rPP)); // Give a path to the intent.

        startActivityForResult(takePictureIntent, 1);

        loadPict(); // TODO this isn't called, I think. Why not?
    }
}
