package com.example.meterkast;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.os.Environment;
import android.widget.RadioGroup;
import android.widget.Toast;

public class StandData {
	public SharedPreferences recordings;
	public SharedPreferences settings;

	public StandData(SharedPreferences s, SharedPreferences r) {
		settings = s; // Open settings file.
        recordings = r; // Open data storage file.
	}
	
	public boolean enkeleStand() {
		return settings.getInt("Metersoort", -1) == R.id.radioEnkeleStand;
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
        String numPict = Integer.valueOf(settings.getInt("nOfRecords", 0) + 1).toString() + "pict.jpg";

        // Create the directory like "/sdcard/meterkast_foto/" if it doesn't exist yet
        new File(Environment.getExternalStorageDirectory() + "/meterkast_foto/").mkdirs();

        // Combine to determine the full path, such as "/sdcard/meterkast_foto/3pict.jpg"
        File fullPath = new File(Environment.getExternalStorageDirectory() + "/meterkast_foto/" + numPict);

        return fullPath; // Return that.
    }
    
    public long getStartDate() {
    	return recordings.getLong("1date", -1);
    }
    
    public Bitmap drawGraph(int width, int height, Bitmap graph) {

        int noRecs = settings.getInt("nOfRecords", 0);

        int highestRecorded = 0;
        int lowestRecorded = 999999 * 2;

        for (int i = 1; i <= noRecs; i++) {
            int thisRecordingFRST = recordings.getInt(i + "frst", -1);
            int thisRecordingSCND = recordings.getInt(i + "scnd", -1);

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

        long startTime = recordings.getLong("1date", -1);
        long endTime = recordings.getLong(Integer.valueOf(noRecs).toString() + "date", -1);

        int previousTime = 0;
        int duration = (int) endTime - (int) startTime;
        double recordingSpan = highestRecorded - lowestRecorded;

        for (int i = 2; i <= noRecs; i++) {
            int thisTime = (int) (recordings.getLong(i + "date", -1) - startTime);
            int thisRecordingFRST = recordings.getInt(i + "frst", -1);
            int thisRecordingSCND = recordings.getInt(i + "scnd", -1);
            int iPrev = (i - 1);
            int previousRecordingFRST = recordings.getInt(iPrev + "frst", -1);
            int previousRecordingSCND = recordings.getInt(iPrev + "scnd", -1);

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
		return graph;
    }
	
	public void makeRecording(String field1, String field2, Context context) {
		// Create an editor object for writing to the settings file.
        SharedPreferences.Editor editor = recordings.edit();
        SharedPreferences.Editor editorSettings = settings.edit();

        // Counting the recordings, we need the next one so + 1
        Integer recordNumber = Integer.valueOf(settings.getInt("nOfRecords", 0) + 1);

        /** Recordings will look like this:
         * "3date" => 39485093218475 (milliseconds since epoch)
         * "3frst" => 503341
         * "3scnd" => 403215
         *
         * Picture filenames look like "/meterkast_foto/3pict.jpg"
         */

        /** Record the first field, if available. */
        if (!field1.equals("")) {
            // Enter the recorded info into the "editor" (which is the file "MeterInfo.whatever").
            long currentTime = System.currentTimeMillis();

            editor.putLong(recordNumber.toString() + "date", currentTime);
            
            // editor.putLong(recordNumber.toString() + "date", System.currentTimeMillis());
            editor.putInt(recordNumber.toString() + "frst", Integer.parseInt(field1));

            /** Record the second field, if necessary. */
            if (!field2.equals("")
                    && settings.getInt("Metersoort", -1) == R.id.radioTweeStanden) {
                // Enter the recorded info into the "editor" (which is the file "MeterInfo.whatever").
                editor.putInt(recordNumber.toString() + "scnd", Integer.parseInt(field2));

                // We have to remember how many settings have been recorded now!
                editorSettings.putInt("nOfRecords", recordNumber.intValue());
            }

            // We have to remember how many settings have been recorded now!
            editorSettings.putInt("nOfRecords", recordNumber.intValue());
        } else {
            Toast.makeText(context, "Incompleet ingevuld!", Toast.LENGTH_SHORT).show();
        }

        editorSettings.commit();
        editor.commit();
	}

	public long getEndDate() {
		int noRecs = settings.getInt("nOfRecords", 0);
		return recordings.getLong(Integer.valueOf(noRecs).toString() + "date", -1);
	}

	public Bitmap drawBarGraphs(int width, int height, Bitmap graph) {
		int noRecs = settings.getInt("nOfRecords", 0);
        long startTime = getStartDate();
        long endTime = getEndDate();
        long duration = endTime - startTime;
        long millisInADay = (1000 * 60 * 60 * 24);
        int totalDays = (int) (duration / millisInADay);
        int highestRecorded = 0;

        for (int i = 1; i <= noRecs; i++) {
            int thisRecordingFRST = recordings.getInt(i + "frst", -1);
            int thisRecordingSCND = recordings.getInt(i + "scnd", -1);

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
        
        return graph;
	}
	
	private int thisDaysUsage(long morning, long evening) {
        int noRecs = settings.getInt("nOfRecords", 0);
        long thisDate;
        int beforeMorning = -1;
        int afterEvening = -1;

        for (int recording = 2; recording <= noRecs; recording += 1) {
            thisDate = recordings.getLong(recording + "date", -1);

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
        long millisBetweenRecs = recordings.getLong(afterEvening + "date", -1)
            - recordings.getLong(beforeMorning + "date", -1);
        int usageBetwRecsFRST = recordings.getInt(afterEvening + "frst", -1)
            - recordings.getInt(beforeMorning + "frst", -1);
        int usageBetwRecsSCND = recordings.getInt(afterEvening + "scnd", -1)
            - recordings.getInt(beforeMorning + "scnd", -1);
        int usageBetweenRecs = usageBetwRecsFRST + usageBetwRecsSCND;
        int usageThisDay = (int) ((double) (millisInDay / millisBetweenRecs) * usageBetweenRecs);

        return usageThisDay;
    }

	public int getRecording(int i, String string) {
		String iS = Integer.valueOf(i).toString();
		return recordings.getInt(iS + string, -1);
	}

	public boolean everyOptionIsSet() {
		return settings.getInt("Woonsituatie", -1) == -1 || settings.getInt("Inwoneraantal", -1) == -1
                || settings.getInt("Metersoort", -1) == -1;
	}

	public void recordSettings(RadioGroup groupWoonSit, RadioGroup groupInw, RadioGroup groupMeetS) {
		// Create an editor object for writing to the settings file.
        SharedPreferences.Editor editor = settings.edit();

        // And enter their selected values into the editor.
        editor.putInt("Woonsituatie", groupWoonSit.getCheckedRadioButtonId());
        editor.putInt("Inwoneraantal", groupInw.getCheckedRadioButtonId());
        editor.putInt("Metersoort", groupMeetS.getCheckedRadioButtonId());

        editor.commit(); // Commit the edits.
		
	}

	public int getSelection(String string) {
		return settings.getInt(string, -1);
	}
}