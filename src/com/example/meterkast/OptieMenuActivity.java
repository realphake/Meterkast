package com.example.meterkast;

import android.app.Activity;

import android.os.Bundle;

import android.view.View;

import android.widget.RadioGroup;


/**
 * @author Arjen Swellengrebel
 */
public class OptieMenuActivity extends Activity {
    
    private static final String METERTYPE = "Metersoort";
	private static final String NUMBEROCCUPANTS = "Inwoneraantal";
	private static final String TYPEOFHOUSE = "Woonsituatie";
	RecordingData data;
    RadioGroup groupTypeHouse;
    RadioGroup groupOccupants;
    RadioGroup groupMeterType;

    @Override /** Do some basic things to create the screen. */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Call onCreate function.
        setContentView(R.layout.activity_optie_menu); // Set layout.

        // Intent intent = getIntent(); // Only necessary when passing info.

        // Initialize the RadioGroup variables so they can be read/set.
        this.groupTypeHouse = (RadioGroup) findViewById(R.id.radioGroupWoonsituatie);
        this.groupOccupants = (RadioGroup) findViewById(R.id.radioGroupInwoners);
        this.groupMeterType = (RadioGroup) findViewById(R.id.radioGroupMeterSoort);

        this.data = new RecordingData(getSharedPreferences("PersonalInfo", MODE_PRIVATE), getSharedPreferences("MeterInfo", MODE_PRIVATE));
        
        // Set the RadioGroups to the values recorded in the PersonalInfo file.
        this.groupTypeHouse.check(data.getSettingSelection(TYPEOFHOUSE));
        this.groupOccupants.check(data.getSettingSelection(NUMBEROCCUPANTS));
        this.groupMeterType.check(data.getSettingSelection(METERTYPE));
    }

    /**
     * Go back to the main screen
     *
     * @param view
     */
    public void goBack(View view) {
        super.onBackPressed(); // Simulates pressing the phone's back key.
    }
    
    public void switchUser(View view) {
    	data.switchUser();
    }

    /**
     * Save settings, then go back to the main screen
     *
     * @param view
     */
    public void saveSettings(View view) {
        data.recordSettings(groupTypeHouse, groupOccupants, groupMeterType);
        super.onBackPressed(); // After saving, close the window.
    }
}
