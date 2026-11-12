package com.example.birthdayreminder.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.birthdayreminder.BirthdayDetailActivity;
import com.example.birthdayreminder.R;
import com.example.birthdayreminder.util.BirthdayUtils;

public final class NotificationHelper {
    public static final String CHANNEL_ID = "birthday_reminders";
    private static final String TAG = "birthday";
    public static final String ACTION_COMPOSE = "com.example.birthdayreminder.COMPOSE";

    private NotificationHelper() {}

    private static NotificationManager manager(Context context) {
        return context.getSystemService(NotificationManager.class);
    }

    public static void createNotificationChannel(Context context) {
        Context display = BirthdayUtils.applicationLanguageContext(context);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                display.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(display.getString(R.string.notification_channel_desc));
        channel.enableVibration(true);
        manager(context).createNotificationChannel(channel);
    }

    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isAvailable(Context context) {
        NotificationManager manager = manager(context);
        NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
        if (channel == null) {
            createNotificationChannel(context);
            channel = manager.getNotificationChannel(CHANNEL_ID);
        }
        return hasPermission(context) && manager.areNotificationsEnabled()
                && channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
    }

    public static void showTest(Context context) {
        post(context, "test", 0, builder(context, R.string.test_notification_title,
                context.getString(R.string.test_notification_body)));
    }

    public static void showBirthdayNotification(Context context, int id, String name, int daysBefore, int year) {
        showBirthdayNotification(context, id, name, daysBefore, year, false);
    }

    public static void showSnoozedBirthday(Context context, int id, String name, int year) {
        showBirthdayNotification(context, id, name, 0, year, true);
    }

    private static void showBirthdayNotification(Context context, int id, String name, int daysBefore, int year,
                                                 boolean snoozed) {
        Context display = BirthdayUtils.applicationLanguageContext(context);
        Intent detail = new Intent(context, BirthdayDetailActivity.class).putExtra("birthday_id", id);
        PendingIntent open = PendingIntent.getActivity(context, id, detail,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent compose = PendingIntent.getActivity(context, id, new Intent(detail).setAction(ACTION_COMPOSE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent celebrated = new Intent(context, BirthdayNotificationReceiver.class)
                .setAction(BirthdayNotificationReceiver.ACTION_CELEBRATED)
                .putExtra("birthday_id", id).putExtra(BirthdayAlarmScheduler.EXTRA_YEAR, year);
        PendingIntent mark = PendingIntent.getBroadcast(context, id, celebrated,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent defer = new Intent(celebrated).setAction(BirthdayNotificationReceiver.ACTION_SNOOZE);
        PendingIntent snooze = PendingIntent.getBroadcast(context, id, defer,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int title;
        String text;
        if (snoozed) {
            title = R.string.snooze_notification_title;
            text = display.getString(R.string.snooze_notification_body, name);
        } else if (daysBefore == 0) {
            title = R.string.notification_today_title;
            text = display.getString(R.string.notification_today_text, name);
        } else if (daysBefore == 1) {
            title = R.string.notification_tomorrow_title;
            text = display.getString(R.string.notification_tomorrow_text, name);
        } else {
            title = R.string.notification_days_before_title;
            text = display.getString(R.string.notification_days_before_text, name, daysBefore);
        }

        post(context, TAG, id, builder(display, title, text)
                .setContentIntent(open)
                .addAction(R.drawable.ic_notification, display.getString(R.string.snooze_one_hour), snooze)
                .addAction(R.drawable.ic_check, display.getString(R.string.mark_celebrated), mark)
                .addAction(R.drawable.ic_message, display.getString(R.string.compose_message), compose));
    }

    public static void cancel(Context context, int id) {
        manager(context).cancel(TAG, id);
        manager(context).cancel(id);
    }

    private static NotificationCompat.Builder builder(Context context, int title, String text) {
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cake)
                .setContentTitle(context.getString(title))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true);
    }

    private static void post(Context context, String tag, int id, NotificationCompat.Builder notification) {
        if (!isAvailable(context)) return;
        try {
            manager(context).notify(tag, id, notification.build());
        } catch (SecurityException ignored) {
            // Permission can change after the availability check.
        }
    }
}
