package com.rohitsuratekar.NCBSinfo.background;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;
import com.rohitsuratekar.NCBSinfo.R;
import com.rohitsuratekar.NCBSinfo.common.utilities.Utilities;
import com.rohitsuratekar.NCBSinfo.database.NotificationData;
import com.rohitsuratekar.NCBSinfo.database.TalkData;
import com.rohitsuratekar.NCBSinfo.database.models.NotificationModel;
import com.rohitsuratekar.NCBSinfo.database.models.TalkModel;
import com.rohitsuratekar.NCBSinfo.interfaces.NetworkConstants;
import com.rohitsuratekar.NCBSinfo.interfaces.UserInformation;
import com.rohitsuratekar.NCBSinfo.online.dashboard.DashBoard;
import com.rohitsuratekar.NCBSinfo.online.events.Events;
import com.rohitsuratekar.NCBSinfo.online.temp.camp.CAMPevents;

/**
 * All notifications should be handled by this class
 * All notifications should use one universal notifier 'notifySystem'
 * Notifications will be send only in following conditions
 * (1) No notification will be send in 'Offline' mode
 * (2) Research Talk notifications will be sent only in 'Online' mode
 * (3) FCM notifications will be send in all modes except 'Offline'. (FCM data can still be accessed in 'Offline' mode)
 */
public class NotificationService implements UserInformation, NetworkConstants {

    private Context context;
    private final String TAG = getClass().getSimpleName();
    private int notificationNumber = 1; //Default

    public NotificationService(Context context) {
        this.context = context;
        Log.i(TAG, "Notification service called at" + new Utilities().timeStamp());
    }

    //Regular Notification
    public void sendNotification(String title, String notificationMessage, Class c) {
        int requestID = (int) System.currentTimeMillis();
        Intent notificationIntent = new Intent(context, c);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, requestID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.notification_icon)
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setSound(sound)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationMessage))
                .setContentText(notificationMessage).setAutoCancel(true)
                .setContentIntent(contentIntent);
        notifySystem(mBuilder, notificationNumber);
    }

    //Notification from FCM
    public void sendNotification(RemoteMessage remoteMessage) {
        int requestID = (int) System.currentTimeMillis();
        String title, message, extra;
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            message = remoteMessage.getNotification().getBody();
            extra = keys.values.EXTRA_PERSONAL;
        } else {
            title = remoteMessage.getData().get(keys.TITLE);
            message = remoteMessage.getData().get(keys.MESSAGE);
            if (remoteMessage.getData().get(keys.EXTRA) != null) {
                extra = remoteMessage.getData().get(keys.EXTRA);
            } else {
                extra = keys.values.EXTRA_PERSONAL;
            }
        }

        //Add to notification data
        NotificationModel note = new NotificationModel();
        note.setTimestamp(new Utilities().timeStamp());
        note.setTitle(title);
        note.setMessage(message);
        note.setFrom(remoteMessage.getFrom());
        note.setExtraVariables(extra);
        new NotificationData(context).add(note);
        Log.i("Tag", "notification data added");

        Intent notificationIntent;
        if (note.getFrom().contains(topics.CAMP16)) {
            notificationIntent = new Intent(context, CAMPevents.class);
        } else {
            notificationIntent = new Intent(context, DashBoard.class);
        }
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, requestID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.notification_icon)
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setSound(sound)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentText(message).setAutoCancel(true)
                .setContentIntent(contentIntent);

        //Notify
        notifySystem(mBuilder, notificationNumber);
    }

    //Event Notification
    public void sendNotification(int code) {
        int requestID = (int) System.currentTimeMillis();
        Intent notificationIntent = new Intent(context, Events.class);
        notificationIntent.putExtra(Events.EVENT_CODE, String.valueOf(code));
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, requestID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        TalkModel talk = new TalkData(context).getEntry(code);
        if (talk != null) {
            if (talk.getActionCode() != NetworkOperations.ACTIONCODE_NOTIFIED) {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setColor(context.getResources().getColor(R.color.colorPrimary))
                        .setSound(sound)
                        .setContentTitle(talk.getNotificationTitle())
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(talk.getTitle()))
                        .setContentText(talk.getTitle()).setAutoCancel(true)
                        .setContentIntent(contentIntent);
                notifySystem(mBuilder, notificationNumber);
                talk.setActionCode(NetworkOperations.ACTIONCODE_NOTIFIED);
                new TalkData(context).update(talk); //Update event as notified to avoid further spam
            }
        }
    }

    //Update notification
    public void updateNotification(String title, String notificationMessage) {
        int requestID = (int) System.currentTimeMillis();

        final String appPackageName = context.getPackageName(); // getPackageName() from Context or Activity object
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String url = "";
        try {
            //Check whether Google Play store is installed or not:
            context.getPackageManager().getPackageInfo("com.android.vending", 0);
            url = "market://details?id=" + appPackageName;
        } catch (final Exception e) {
            url = "https://play.google.com/store/apps/details?id=" + appPackageName;
        }
        Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, requestID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.notification_icon)
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setSound(sound)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationMessage))
                .setContentText(notificationMessage).setAutoCancel(true)
                .setContentIntent(contentIntent);

        notifySystem(mBuilder, notificationNumber);

    }

    //Multiple Notification
    public void sendNotification(String title, String notificationMessage, Class c, int notificationNumber) {
        int requestID = (int) System.currentTimeMillis();
        Intent notificationIntent = new Intent(context, c);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, requestID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.notification_icon)
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setSound(sound)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationMessage))
                .setContentText(notificationMessage).setAutoCancel(true)
                .setContentIntent(contentIntent);
        notifySystem(mBuilder, notificationNumber);
    }

    private void notifySystem(NotificationCompat.Builder mBuilder, int notificationNumber) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        //Notifications will be send only if user has not changed default value and it is not "offline" mode.
        if (pref.getBoolean(preferences.NOTIFICATIONS, true) && !pref.getString(MODE, ONLINE).equals(OFFLINE)) {
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(notificationNumber, mBuilder.build());
        }
    }

}
