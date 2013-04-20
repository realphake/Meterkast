package com.example.meterkast;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;

import android.content.Context;
import android.content.Intent;

import android.os.IBinder;

import android.support.v4.app.NotificationCompat;

import android.widget.Toast;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


/**
 * @author Arjen Swellengrebel
 */
public class NotificationService extends Service {
    private static final int MILLISINADAY = 1000 * 60 * 60 * 24;

	@Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, R.string.make_note, Toast.LENGTH_LONG).show();

        Date date = new Date(System.currentTimeMillis() + MILLISINADAY);
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    makeNotification();
                    stopSelf();
                }
            };
        timer.schedule(timerTask, date);
    }

    /**
     * Pops up a note.
     */
    public void makeNotification() {
        NotificationCompat.Builder bob = new NotificationCompat.Builder(this);
        bob.setSmallIcon(R.drawable.ic_launcher);
        bob.setContentTitle("Stand opnemen");
        bob.setContentText("Maak een nieuwe opname van de meterstand!");

        Notification note = bob.build();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, note);
    }
}
