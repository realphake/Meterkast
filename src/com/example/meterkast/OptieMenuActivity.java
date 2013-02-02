package com.example.meterkast;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.app.Activity;
import android.content.SharedPreferences;

public class OptieMenuActivity extends Activity {

	SharedPreferences settings;
	RadioGroup groupWoonSit;
    RadioGroup groupInw;
    RadioGroup groupMeetS;
	
	@Override /** Do some basic things to create the screen. */
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState); // Call onCreate function.
		setContentView(R.layout.activity_optie_menu); // Set layout.
		//Intent intent = getIntent(); // Only necessary when passing info.
		
		// Initialize the RadioGroup variables so they can be read/set.
		groupWoonSit = (RadioGroup)findViewById(R.id.radioGroupWoonsituatie);
	    groupInw = (RadioGroup)findViewById(R.id.radioGroupInwoners);
	    groupMeetS = (RadioGroup)findViewById(R.id.radioGroupMeterSoort);
	    
		settings = getSharedPreferences("PersonalInfo", MODE_PRIVATE); // Open settings file.
		
		// Set the RadioGroups to the values recorded in the PersonalInfo file.
		groupWoonSit.check(settings.getInt("Woonsituatie", -1));
		groupInw.check(settings.getInt("Inwoneraantal", -1));
		groupMeetS.check(settings.getInt("Metersoort", -1));
	}
	
	/** Go back to the main screen */
	public void goBack(View view) {
		
		super.onBackPressed(); // Simulates pressing the phone's back key.
	}
	
	/** Save settings, then go back to the main screen */
	public void saveSettings(View view) {
		
		// Create an editor object for writing to the settings file.
	    SharedPreferences.Editor editor = settings.edit();
	    
	    // And enter their selected values into the editor.
	    editor.putInt("Woonsituatie", groupWoonSit.getCheckedRadioButtonId() );
	    editor.putInt("Inwoneraantal", groupInw.getCheckedRadioButtonId() );
	    editor.putInt("Metersoort", groupMeetS.getCheckedRadioButtonId() );
	    
	    editor.commit(); // Commit the edits.
		super.onBackPressed(); // After saving, close the window.
	}

}
