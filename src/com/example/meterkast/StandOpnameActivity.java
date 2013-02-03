package com.example.meterkast;


import android.os.Bundle;
import android.text.format.Time;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.app.Activity;
//import android.content.Intent;
//import android.view.Menu;
import android.content.SharedPreferences;

public class StandOpnameActivity extends Activity {

	SharedPreferences recordings, settings;
	EditText editText1, editText2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stand_opname);
		//Intent intent = getIntent(); // Only neccessary when passing info between views.

		settings = getSharedPreferences("PersonalInfo", MODE_PRIVATE); // Open settings file.
		recordings = getSharedPreferences("MeterInfo", MODE_PRIVATE); // Open data storage file.

		// Initialize the textfield variables so they can be read/set.
		editText1 = (EditText)findViewById(R.id.editText1);
		editText2 = (EditText)findViewById(R.id.editText2);

		// If you have selected single meters, don't show the second text window.
		if( settings.getInt("Metersoort", -1) == R.id.radioEnkeleStand ) {
			editText2.setVisibility(View.INVISIBLE); // Invisible, but not gone.
		}

	}

	/** Go back to the main screen */
	public void goBack(View view) {

		super.onBackPressed(); // Simulates pressing the phone's back key.
	}

	/** Save settings, then go back to the main screen */
	public void saveNumbers(View view) {

		// Create an editor object for writing to the settings file.
		SharedPreferences.Editor editor = recordings.edit();
		SharedPreferences.Editor editorSettings = settings.edit();

		// Counting the recordings, we need the next one so + 1
		Integer recordNumber = settings.getInt("nOfRecords", 0) + 1;

		/** Recordings will look like this:
		 * "3date" => 39485093218475
		 * "3frst" => 503341
		 * "3scnd" => 403215
		 */

		/** Record the first field, if available. */
		if (!editText1.getText().toString().equals("")) {
			// Enter the recorded info into the "editor" (which is the file "MeterInfo.whatever").
			Time ctime = new Time(); // Current time is used as the KEY.
			ctime.setToNow();
			editor.putLong(recordNumber.toString().concat("date"), ctime.toMillis(false) );
			editor.putInt(recordNumber.toString().concat("frst"), Integer.parseInt(editText1.getText().toString()) );

			/** Record the second field, if necessary. */
			if ( !editText2.getText().toString().equals("") &&
					settings.getInt("Metersoort", -1) == R.id.radioTweeStanden ) {
				// Enter the recorded info into the "editor" (which is the file "MeterInfo.whatever").
				editor.putInt(recordNumber.toString().concat("scnd"), Integer.parseInt(editText2.getText().toString()) );
				// We have to remember how many settings have been recorded now!
				editorSettings.putInt("nOfRecords", recordNumber);
			}

			// We have to remember how many settings have been recorded now!
			editorSettings.putInt("nOfRecords", recordNumber);
			
		}
		else {
			Toast.makeText(getApplicationContext(), "Incompleet ingevuld!", 
					Toast.LENGTH_SHORT).show();
		}

		editorSettings.commit(); // Commit the edits.
		editor.commit(); 
		super.onBackPressed(); // After saving, close the window.
	}

}
