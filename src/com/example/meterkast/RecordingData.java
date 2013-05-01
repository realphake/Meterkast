package com.example.meterkast;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.widget.RadioGroup;
import android.widget.Toast;

public class RecordingData {
	// Define some constants.
	private static final String KINDOFMETER = "Metersoort";
	private static final String NUMBEROFOCCUPANTS = "Inwoneraantal";
	private static final String TYPEOFHOUSE = "Woonsituatie";
	static final int MILLISINADAY = 1000 * 60 * 60 * 24;
	static final int BOTTOMTEXTDISTANCE = 10;
	static final int TOPTEXTDISTANCE = 30;
	static final int HIGHESTPOSSIBLERECORDING = 999999 * 2;

	private static final int HASNOTBEENRECORDED = -1;
	public SharedPreferences recordings;
	public SharedPreferences settings;

	public RecordingData(SharedPreferences s, SharedPreferences r) {
		settings = s; // Open settings file.
        recordings = r; // Open data storage file.
	}
	
	// Has the option for "single recording type" been selected?
	public boolean singleRecording() {
		return getSettingSelection(KINDOFMETER) == R.id.radioEnkeleStand;
	}
	
	/**
     * This gives the full path to the picture that we currently want to look at. For example, if the last recording was numbered
     * "2", this will give a path to "3pict.jpg". It also creates the "meterkast_foto" folder if it doesn't exist yet.
     *
     * @return
     */
    File getRelevantPhotoPath() {
        // Create a file in the following steps:
        // Determine the filename for this one, such as "3pict.jpg" (where 3 is the number of the next (this) recording)
        String numPict = Integer.valueOf(getSettingSelection("nOfRecords") + 2).toString() + "pict"+getCurrentUser()+".jpg";

        // Create the directory like "/sdcard/meterkast_foto/" if it doesn't exist yet
        new File(Environment.getExternalStorageDirectory() + "/meterkast_foto/").mkdirs();

        // Combine to determine the full path, such as "/sdcard/meterkast_foto/3pict.jpg"
        File fullPath = new File(Environment.getExternalStorageDirectory() + "/meterkast_foto/" + numPict);

        return fullPath; // Return that.
    }
    
    public long getDate(int i) {
    	// Returns the date on which the i-th recording was made. 
    	return recordings.getLong(i+"date"+getCurrentUser(), HASNOTBEENRECORDED);
    }

    
	
	public void makeRecording(String field1, String field2, Context context) {
		// Create an editor object for writing to the settings file.
        SharedPreferences.Editor editor = recordings.edit();
        SharedPreferences.Editor editorSettings = settings.edit();

        // Counting the recordings, we need the next one so + 1
        Integer recordNumber = Integer.valueOf(getSettingSelection("nOfRecords") + 1);

        /** Recordings will look like this:
         * "3date1" => 39485093218475 (milliseconds since epoch)
         * "3frst1" => 503341
         * "3scnd1" => 403215
         *
         * Picture filenames look like "/meterkast_foto/3pict1.jpg"
         */

        /** Record the first field, if available. */
        if (!field1.equals("")) {
            // Enter the recorded info into the "editor" (which is the file "MeterInfo.whatever").
            long currentTime = System.currentTimeMillis();

            editor.putLong(recordNumber.toString() + "date"+getCurrentUser(), currentTime);
            
            // editor.putLong(recordNumber.toString() + "date", System.currentTimeMillis());
            editor.putInt(recordNumber.toString() + "frst"+getCurrentUser(), Integer.parseInt(field1));

            /** Record the second field, if necessary. */
            if (!field2.equals("")
                    && getSettingSelection(KINDOFMETER) == R.id.radioTweeStanden) {
                // Enter the recorded info into the "editor" (which is the file "MeterInfo.whatever").
                editor.putInt(recordNumber.toString() + "scnd"+getCurrentUser(), Integer.parseInt(field2));

                // We have to remember how many settings have been recorded now!
                editorSettings.putInt("nOfRecords"+getCurrentUser(), recordNumber.intValue());
            }

            // We have to remember how many settings have been recorded now!
            editorSettings.putInt("nOfRecords"+getCurrentUser(), recordNumber.intValue());
        } else {
            Toast.makeText(context, R.string.incomplete, Toast.LENGTH_SHORT).show();
        }

        editorSettings.commit();
        editor.commit();
	}

	/**
	 * Get the last date that has been recorded (ie the newest.)
	 */
	public long getEndDate() {
		int noRecs = getSettingSelection("nOfRecords");
		return getDate(Integer.valueOf(noRecs));
	}

	
	
	/**
	 * Find out how much electricity has been used between two moments.
	 */
	int thisDaysUsage(long morning, long evening) {
        int noRecs = getSettingSelection("nOfRecords");
        long thisDate;
        int beforeMorning = 0; // These will be set later
        int afterEvening = 0;

        // Find the recording most recent but before the "morning" moment, and;
        // Find the recording closest to but after the "evening" moment.
        for (int recording = 2; recording <= noRecs; recording += 1) {
            thisDate = getDate(recording);

            if (beforeMorning == 0 && morning < thisDate) { // DO YOU SEE
                beforeMorning = recording - 1;
            }

            if (afterEvening == 0 && evening < thisDate) { // DO YOU SEE
                afterEvening = recording;
            }

            if (afterEvening != 0 && beforeMorning != 0) {
                break; // If both are found, we can stop.
            }
        }

        // Now, find the gradient between the two encapsulating recordings...
        long millisBetweenRecs = getDate(afterEvening)
            - getDate(beforeMorning);
        int usageBetwRecsFRST = getRecording(afterEvening, "frst")
            - getRecording(beforeMorning, "frst");
        int usageBetwRecsSCND = getRecording(afterEvening, "scnd")
            - getRecording(beforeMorning, "scnd");
        int usageBetweenRecs = usageBetwRecsFRST + usageBetwRecsSCND;
        // And pinpoint the morning and evening on that gradient. Take the difference between those, and go.
        int usageThisDay = (int) ((double) (MILLISINADAY / millisBetweenRecs) * usageBetweenRecs);

        return usageThisDay;
    }

	public int getRecording(int i, String string) { // Simply get a recording with a certain number and type ("frst"or "scnd").
		String iS = Integer.valueOf(i).toString();
		return recordings.getInt(iS+string+getCurrentUser(), HASNOTBEENRECORDED);
	}

	public boolean everyOptionIsSet() { // Is every option in the options-menu actually set?
		return settings.getInt(TYPEOFHOUSE+getCurrentUser(),-1) != -1
				&& settings.getInt(NUMBEROFOCCUPANTS+getCurrentUser(),-1) != -1
                && settings.getInt(KINDOFMETER+getCurrentUser(),-1) != -1;
	}

	public void recordSettings(RadioGroup groupWoonSit, RadioGroup groupInw, RadioGroup groupMeetS) {
		// Create an editor object for writing to the settings file.
        SharedPreferences.Editor editor = settings.edit();

        // And enter their selected values into the editor.
        
        editor.putInt(TYPEOFHOUSE+getCurrentUser(), groupWoonSit.getCheckedRadioButtonId());
        editor.putInt(NUMBEROFOCCUPANTS+getCurrentUser(), groupInw.getCheckedRadioButtonId());
        editor.putInt(KINDOFMETER+getCurrentUser(), groupMeetS.getCheckedRadioButtonId());

        editor.commit(); // Commit the edits.
		
	}

	public int getSettingSelection(String string) {
		// Returns the setting of a particular option in the options menu.
		return settings.getInt(string+getCurrentUser(), HASNOTBEENRECORDED);
	}
	
	/**
	 * The app can take two different users. The one is denoted as 1, the other as -1.
	 * If no recording has been made yet, the current user is 1.
	 */
	public int getCurrentUser() {
		// Get the user
		int userSetting = settings.getInt( "User", HASNOTBEENRECORDED );
		if ( userSetting == HASNOTBEENRECORDED ) { // if couldn't find, say it's user 1
			return 1;
		}
		else return userSetting; // Else just say which user it is.
	}

	public void switchUser() { // Set the user as -1 if it's 1, and the other way around.
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt( "User", -getCurrentUser() ); // Just the opposite, which is why I chose those numbers.
		editor.commit();
		
	}
}