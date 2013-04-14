package com.example.meterkast;

import android.app.Activity;

import android.os.Bundle;

import android.view.View;

import android.widget.RadioGroup;


/**
 * @author Arjen Swellengrebel
 */
public class OptieMenuActivity extends Activity {
    
    StandData data;
    RadioGroup groupWoonSit;
    RadioGroup groupInw;
    RadioGroup groupMeetS;

    @Override /** Do some basic things to create the screen. */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Call onCreate function.
        setContentView(R.layout.activity_optie_menu); // Set layout.

        // Intent intent = getIntent(); // Only necessary when passing info.

        // Initialize the RadioGroup variables so they can be read/set.
        this.groupWoonSit = (RadioGroup) findViewById(R.id.radioGroupWoonsituatie);
        this.groupInw = (RadioGroup) findViewById(R.id.radioGroupInwoners);
        this.groupMeetS = (RadioGroup) findViewById(R.id.radioGroupMeterSoort);

        this.data = new StandData(getSharedPreferences("PersonalInfo", MODE_PRIVATE), getSharedPreferences("MeterInfo", MODE_PRIVATE));
        
        // Set the RadioGroups to the values recorded in the PersonalInfo file.
        this.groupWoonSit.check(data.getSelection("Woonsituatie"));
        this.groupInw.check(data.getSelection("Inwoneraantal"));
        this.groupMeetS.check(data.getSelection("Metersoort"));
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
     * Save settings, then go back to the main screen
     *
     * @param view
     */
    public void saveSettings(View view) {
        data.recordSettings(groupWoonSit, groupInw, groupMeetS);
        super.onBackPressed(); // After saving, close the window.
    }
}
