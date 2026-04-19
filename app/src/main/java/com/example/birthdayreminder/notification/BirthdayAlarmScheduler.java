package com.example.birthdayreminder.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.birthdayreminder.data.Birthday;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class BirthdayAlarmScheduler {

    private static final int ALARM_OFFSET = 0;

    /**
     * Schedule an alarm based on the user's custom notification preferences.
     * Uses notifyDaysBefore (0=same day, 1, 2, 7) and notifyHour/notifyMinute.
     */
    public static void scheduleAlarm(Context context, Birthday birthday) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) return;

        // Calculate next birthday date
        LocalDate today = LocalDate.now();
        LocalDate nextBirthday;
        try {
            nextBirthday = LocalDate.of(today.getYear(), birthday.getMonth(), birthday.getDay());
        } catch (Exception e) {
            nextBirthday = LocalDate.of(today.getYear(), birthday.getMonth(),
                    Math.min(birthday.getDay(), 28));
        }

        if (nextBirthday.isBefore(today) || nextBirthday.isEqual(today)) {
            try {
                nextBirthday = LocalDate.of(today.getYear() + 1, birthday.getMonth(), birthday.getDay());
            } catch (Exception e) {
                nextBirthday = LocalDate.of(today.getYear() + 1, birthday.getMonth(),
                        Math.min(birthday.getDay(), 28));
            }
        }

        // Calculate alarm date: birthday minus daysBefore
        int daysBefore = birthday.getNotifyDaysBefore();
        int hour = birthday.getNotifyHour();
        int minute = birthday.getNotifyMinute();

        LocalDate alarmDate = nextBirthday.minusDays(daysBefore);
        LocalDateTime alarmDateTime = alarmDate.atTime(hour, minute);
        long alarmMillis = alarmDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        // Only schedule if the alarm time is in the future
        if (alarmMillis <= System.currentTimeMillis()) {
            return;
        }

        Intent intent = new Intent(context, BirthdayNotificationReceiver.class);
        intent.putExtra("birthday_id", birthday.getId());
        intent.putExtra("birthday_name", birthday.getName());
        intent.putExtra("days_before", daysBefore);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, birthday.getId() + ALARM_OFFSET, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmMillis, pendingIntent);
        } catch (SecurityException e) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmMillis, pendingIntent);
        }
    }

    /**
     * Cancel alarms for a birthday.
     */
    public static void cancelAlarm(Context context, int birthdayId) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, BirthdayNotificationReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, birthdayId + ALARM_OFFSET, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }
}

