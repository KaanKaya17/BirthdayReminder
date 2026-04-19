package com.example.birthdayreminder.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BirthdayNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int birthdayId = intent.getIntExtra("birthday_id", -1);
        String name = intent.getStringExtra("birthday_name");
        int daysBefore = intent.getIntExtra("days_before", 0);

        if (birthdayId != -1 && name != null) {
            NotificationHelper.showBirthdayNotification(context, birthdayId, name, daysBefore);
        }
    }
}

