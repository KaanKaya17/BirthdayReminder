package com.example.birthdayreminder.notification;

import android.app.Application;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.birthdayreminder.data.BirthdayRepository;
import com.example.birthdayreminder.widget.BirthdayWidget;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            PendingResult pending = goAsync();
            NotificationHelper.createNotificationChannel(context);
            BirthdayWidget.refreshAsync(context, pending::finish);
            return;
        }
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            PendingResult pending = goAsync();
            if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
                NotificationHelper.createNotificationChannel(context);
            }
            new BirthdayRepository((Application) context.getApplicationContext())
                    .refreshAlarms(pending::finish);
        }
    }
}
