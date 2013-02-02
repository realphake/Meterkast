package com.example.meterkast;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
//import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class Hoofdscherm extends Activity {

	// Global variables.
	SharedPreferences settings, recordings; // The info files used by the app.

	@Override 
	/** This is called when the app starts.
	 * Initializes the window, opens the settings files,
	 * also checks whether the options have been set and if not prompts user to do so. */
	protected void onCreate(Bundle savedInstanceState) {

		// initialize the main screen itself.
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_hoofdscherm);

		settings = getSharedPreferences("PersonalInfo", MODE_PRIVATE); // Open settings file.
		recordings = getSharedPreferences("MeterInfo", MODE_PRIVATE); // Open data storage file.

		// See if all settings have been recorded. 
		if ( settings.getInt("Woonsituatie", -1) == -1 ||
				settings.getInt("Inwoneraantal", -1) == -1 ||
				settings.getInt("Metersoort", -1) == -1 ) {

			// Show a message saying we need to change some settings first.
			AlertDialog ad = new AlertDialog.Builder(this).create();  
			//ad.setCancelable(false); // This blocks the 'BACK' button. Is that what we want?
			ad.setMessage(getString(R.string.welkom_bericht));
			// Make the OK-button:
			ad.setButton(AlertDialog.BUTTON_POSITIVE,getString(R.string.ok_knop), 
					new DialogInterface.OnClickListener() {  
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
		// Then add the following function to that code:
		showRecordings();
	}

	/** Called when the window becomes active.
	 * Shows some info about previous recordings. */
	private void showRecordings() {

		// Find the number of previous recordings for reference.
		Integer noRecs = settings.getInt("nOfRecords", 0);
		String infoS = Integer.toString( noRecs ); // To start the infobox with.

		// For each entry i that has a recording:
		for (Integer i = 1; i <= noRecs; i++) {

			String iS = i.toString(); // Need this for reading the keys and also printing.
			Integer recI = recordings.getInt(iS.concat("frst"), -1); // Find recording.
			Integer recI2 = recordings.getInt(iS.concat("scnd"), -1);
			System.out.println ( settings.getInt("Metersoort", -1) );
			String entry;
			if (recI2 != -1 && settings.getInt("Metersoort", -1) == R.id.radioTweeStanden ) { 
				entry = iS.concat(": ").concat(recI.toString()).concat(", ").concat(recI2.toString());
			} else {
				entry = iS.concat(": ").concat(recI.toString());
			}
			infoS = infoS.concat("\n").concat(entry);
		}

		// Set the textView in the main screen to the number of prev. recordings.
		TextView textView1 = (TextView)findViewById(R.id.textView1);
		textView1.setText(infoS);

	}

	/** Called when the user clicks the Record button
	 * Opens the Record screen. */
	public void goToRecord(View view) {

		Intent intentToRecord = new Intent(this, StandOpnameActivity.class);
		startActivity(intentToRecord); // Opening the record screen.
	}

	/** Called when the user clicks the Options button 
	 * Opens the option-menu.*/
	public void goToOptions(View view) {

		Intent intentToOptions = new Intent(this, OptieMenuActivity.class);
		startActivity(intentToOptions); // Opening the options screen.
	}

	/** Called when the user clicks the RESET button
	 * Deletes all data from all settings files. */
	public void reset(View view) {

		// Open editors for both settings files.
		SharedPreferences.Editor editor = recordings.edit();
		SharedPreferences.Editor editorSettings = settings.edit();

		// "clear" both settings files.
		editor.clear();
		editorSettings.clear();

		// Commit the changes.
		editor.commit();
		editorSettings.commit();
	}


}
