package com.example.birthdayreminder.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.birthdayreminder.data.Birthday;
import com.example.birthdayreminder.data.BirthdayDatabase;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * Re-schedules all birthday alarms after device reboot.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Executors.newSingleThreadExecutor().execute(() -> {
                BirthdayDatabase db = BirthdayDatabase.getDatabase(context);
                List<Birthday> birthdays = db.birthdayDao().getAllBirthdaysSync();

                for (Birthday birthday : birthdays) {
                    if (birthday.isNotifyEnabled()) {
                        BirthdayAlarmScheduler.scheduleAlarm(context, birthday);
                    }
                }
            });
        }
    }
}
