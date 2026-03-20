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

    private static final int ALARM_OFFSET_TODAY = 0;
    private static final int ALARM_OFFSET_DAY_BEFORE = 10000;

    /**
     * Schedule alarms for a birthday (day-of at 9:00 AM + day-before at 9:00 AM).
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

        // Day-of alarm at 9:00 AM
        LocalDateTime dayOfAlarm = nextBirthday.atTime(9, 0);
        long dayOfMillis = dayOfAlarm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        Intent todayIntent = new Intent(context, BirthdayNotificationReceiver.class);
        todayIntent.putExtra("birthday_id", birthday.getId());
        todayIntent.putExtra("birthday_name", birthday.getName());
        todayIntent.putExtra("is_today", true);

        PendingIntent todayPendingIntent = PendingIntent.getBroadcast(
                context, birthday.getId() + ALARM_OFFSET_TODAY, todayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dayOfMillis, todayPendingIntent);
        } catch (SecurityException e) {
            // Fallback to inexact alarm
            alarmManager.set(AlarmManager.RTC_WAKEUP, dayOfMillis, todayPendingIntent);
        }

        // Day-before alarm at 9:00 AM
        LocalDateTime dayBeforeAlarm = nextBirthday.minusDays(1).atTime(9, 0);
        long dayBeforeMillis = dayBeforeAlarm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        if (dayBeforeMillis > System.currentTimeMillis()) {
            Intent dayBeforeIntent = new Intent(context, BirthdayNotificationReceiver.class);
            dayBeforeIntent.putExtra("birthday_id", birthday.getId());
            dayBeforeIntent.putExtra("birthday_name", birthday.getName());
            dayBeforeIntent.putExtra("is_today", false);

            PendingIntent dayBeforePendingIntent = PendingIntent.getBroadcast(
                    context, birthday.getId() + ALARM_OFFSET_DAY_BEFORE, dayBeforeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dayBeforeMillis, dayBeforePendingIntent);
            } catch (SecurityException e) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, dayBeforeMillis, dayBeforePendingIntent);
            }
        }
    }

    /**
     * Cancel alarms for a birthday.
     */
    public static void cancelAlarm(Context context, int birthdayId) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, BirthdayNotificationReceiver.class);

        PendingIntent todayPending = PendingIntent.getBroadcast(
                context, birthdayId + ALARM_OFFSET_TODAY, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(todayPending);

        PendingIntent dayBeforePending = PendingIntent.getBroadcast(
                context, birthdayId + ALARM_OFFSET_DAY_BEFORE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(dayBeforePending);
    }
}
