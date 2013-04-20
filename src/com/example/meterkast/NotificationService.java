package com.example.meterkast;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.Context;
import android.content.Intent;

import android.os.IBinder;

import android.support.v4.app.NotificationCompat;

import android.widget.Toast;



/**
 * @author Arjen Swellengrebel
 */
public class NotificationService extends Service {
    private static final int MILLISINADAY = 1000 * 60 * 60 * 24;
    AlarmManager am;

	@Override
    public IBinder onBind(Intent arg0) {
        return null; // Actually, I'm not sure what this does. It was just generated, it's fine I guess
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Let the user know a notification is being planned:
        Toast.makeText(this, getString(R.string.make_note), Toast.LENGTH_LONG).show();
        
        am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = new Intent(this, NotificationService.class); // This will call the onReceive method of this class.
        PendingIntent makeNote = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT); // Make it pending
        am.set(AlarmManager.RTC, System.currentTimeMillis() + MILLISINADAY, makeNote); // Set the alarm to call the intent

    }

    /**
     * Pops up a note.
     */
    public void onReceive() { // This is run when the alarm goes off. It just shows a notification in the appropriate bar.
        NotificationCompat.Builder bob = new NotificationCompat.Builder(this); // Called bob. Get it? Stop. Bad.
        bob.setSmallIcon(R.drawable.ic_launcher);
        bob.setContentTitle(getString(R.string.makerecording)); // Set the basic data.
        bob.setContentText(getString(R.string.makerecordingimplore));

        Notification note = bob.build(); // Heh.
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, note); // And fire.
    }
}
