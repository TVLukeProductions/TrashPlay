package de.lukeslog.trashplay.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;

import de.lukeslog.trashplay.R;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.support.Logger;
import de.lukeslog.trashplay.ui.MainControl;
import de.lukeslog.trashplay.ui.SettingsActivity;

public class NotificationController {

    public static final String TAG = TrashPlayConstants.TAG;

    private static int MAIN_NOTIFICATION_NUMBER =5646;
    private static int RADIO_NOTIFICATION_NUMBER =5647;

    public static void createNotification(String bigText, String smallText) {
        if(TrashPlayService.serviceRunning()) {
            stopNotification();
            Logger.d(TAG, "createNotification");
            int icon = R.drawable.recopen_small;
            Notification note = new Notification(icon, "TrashPlayer", System.currentTimeMillis());
            Intent i = new Intent(TrashPlayService.getContext(), MainControl.class);

            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pi = PendingIntent.getActivity(TrashPlayService.getContext(), 0,
                    i, 0);

            note.setLatestEventInfo(TrashPlayService.getContext(), bigText,
                    smallText,
                    pi);
            note.flags |= Notification.FLAG_ONGOING_EVENT
                    | Notification.FLAG_NO_CLEAR;
            TrashPlayService.getContext().startForeground(MAIN_NOTIFICATION_NUMBER, note);
        }
    }


    public static void createRadioNotification(String notificationText) {
        if(TrashPlayService.serviceRunning()) {
            int icon = R.drawable.radio_small;
            Notification note = new Notification(icon, "New Radio Station", System.currentTimeMillis());
            Intent i = new Intent(TrashPlayService.getContext(), SettingsActivity.class);

            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pi = PendingIntent.getActivity(TrashPlayService.getContext(), 0,
                    i, 0);

            note.setLatestEventInfo(TrashPlayService.getContext(), "TrashPlayer",
                    notificationText,
                    pi);
            note.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
            TrashPlayService.getContext().startForeground(RADIO_NOTIFICATION_NUMBER, note);
        }
    }

    public static void createNotification(String notificationText) {
        createNotification("Trash Player", notificationText);
    }

    protected static void stopNotification() {
        if(TrashPlayService.serviceRunning()) {
            NotificationManager notificationManager = (NotificationManager)
                    TrashPlayService.getContext().getSystemService(TrashPlayService.getContext().NOTIFICATION_SERVICE);
            notificationManager.cancel(MAIN_NOTIFICATION_NUMBER);
        }
    }
}
