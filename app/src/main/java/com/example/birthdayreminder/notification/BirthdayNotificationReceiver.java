package com.example.birthdayreminder.notification;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.birthdayreminder.data.BirthdayRepository;

public class BirthdayNotificationReceiver extends BroadcastReceiver {
    static final String ACTION_CELEBRATED = "com.example.birthdayreminder.CELEBRATED";
    static final String ACTION_SNOOZE = "com.example.birthdayreminder.SNOOZE";
    static final String ACTION_SNOOZE_DELIVERY = "com.example.birthdayreminder.SNOOZE_DELIVERY";

    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra("birthday_id", -1);
        if (id <= 0) return;

        PendingResult pending = goAsync();
        BirthdayRepository repository = new BirthdayRepository((Application) context.getApplicationContext());
        int year = intent.getIntExtra(BirthdayAlarmScheduler.EXTRA_YEAR, 0);
        String action = intent.getAction();
        long time = intent.getLongExtra(BirthdayAlarmScheduler.EXTRA_TIME, 0);
        if (ACTION_CELEBRATED.equals(action)) {
            repository.markFromNotification(id, year, pending::finish);
        } else if (ACTION_SNOOZE.equals(action)) {
            repository.snooze(id, year, pending::finish);
        } else if (ACTION_SNOOZE_DELIVERY.equals(action)) {
            repository.deliverSnooze(id, year, time, pending::finish);
        } else {
            int offset = intent.getIntExtra(BirthdayAlarmScheduler.EXTRA_OFFSET, -1);
            repository.deliverAlarm(id, offset, year, time, pending::finish);
        }
    }
}
