package com.example.birthdayreminder.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.birthdayreminder.BirthdayDetailActivity;
import com.example.birthdayreminder.R;

public class NotificationHelper {

    public static final String CHANNEL_ID = "birthday_reminders";

    public static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notification_channel_desc));
        channel.enableVibration(true);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public static void showBirthdayNotification(Context context, int birthdayId, String name,
                                                  int daysBefore) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, BirthdayDetailActivity.class);
        intent.putExtra("birthday_id", birthdayId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, birthdayId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title, text;
        if (daysBefore == 0) {
            title = context.getString(R.string.notification_today_title);
            text = context.getString(R.string.notification_today_text, name);
        } else if (daysBefore == 1) {
            title = context.getString(R.string.notification_tomorrow_title);
            text = context.getString(R.string.notification_tomorrow_text, name);
        } else {
            title = context.getString(R.string.notification_days_before_title);
            text = context.getString(R.string.notification_days_before_text, name, daysBefore);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cake)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(birthdayId, builder.build());
        }
    }
}

