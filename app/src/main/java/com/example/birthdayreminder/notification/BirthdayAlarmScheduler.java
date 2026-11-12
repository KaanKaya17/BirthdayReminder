package com.example.birthdayreminder.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.birthdayreminder.data.Birthday;
import com.example.birthdayreminder.data.ReminderSlot;
import com.example.birthdayreminder.util.BirthdayUtils;

import java.time.LocalDate;
import java.time.Instant;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class BirthdayAlarmScheduler {
    static final String EXTRA_OFFSET = "reminder_offset";
    static final String EXTRA_YEAR = "occurrence_year";
    static final String EXTRA_TIME = "reminder_time";

    private BirthdayAlarmScheduler() {}

    public static final class Reminder {
        public final int daysBefore;
        public final LocalDate birthdayDate;
        public final ZonedDateTime time;

        Reminder(int daysBefore, LocalDate birthdayDate, ZonedDateTime time) {
            this.daysBefore = daysBefore;
            this.birthdayDate = birthdayDate;
            this.time = time;
        }
    }

    public static Reminder nextReminder(Birthday birthday, ZonedDateTime now) {
        if (birthday.isArchived() || !birthday.isNotifyEnabled()) return null;

        Reminder next = null;
        for (ReminderSlot slot : birthday.reminderSlots()) {
            int offset = slot.daysBefore;
            LocalDate occurrence = BirthdayUtils.nextBirthday(
                    birthday.getDay(), birthday.getMonth(), now.toLocalDate().plusDays(offset));
            ZonedDateTime time = occurrence.minusDays(offset).atTime(slot.time).atZone(now.getZone());
            while (!time.isAfter(now) || occurrence.getYear() == birthday.getCelebratedYear()
                    || time.toInstant().toEpochMilli() <= birthday.getLastRegularDelivery()) {
                occurrence = BirthdayUtils.nextBirthday(birthday.getDay(), birthday.getMonth(), occurrence.plusDays(1));
                time = occurrence.minusDays(offset).atTime(slot.time).atZone(now.getZone());
            }
            if (next == null || time.isBefore(next.time)) {
                next = new Reminder(offset, occurrence, time);
            }
        }
        return next;
    }

    public static LocalDate validDelivery(Birthday birthday, int offset, int year, long millis, ZonedDateTime now) {
        if (birthday.isArchived() || !birthday.isNotifyEnabled() || year == birthday.getCelebratedYear()
                || millis <= birthday.getLastRegularDelivery()
                || !ReminderSlot.DAY_OFFSETS.contains(offset)
                || year < now.getYear() || year > now.getYear() + 1) {
            return null;
        }
        LocalDate occurrence = MonthDay.of(birthday.getMonth(), birthday.getDay()).atYear(year);
        if (now.toInstant().toEpochMilli() < millis || now.toLocalDate().isAfter(occurrence)) return null;
        for (ReminderSlot slot : birthday.reminderSlots()) {
            if (slot.daysBefore == offset && occurrence.minusDays(offset).atTime(slot.time)
                    .atZone(now.getZone()).toInstant().toEpochMilli() == millis) {
                return occurrence;
            }
        }
        return null;
    }

    public static boolean canSnooze(Birthday birthday, int year, LocalDate today) {
        if (birthday.isArchived() || !birthday.isNotifyEnabled() || year == birthday.getCelebratedYear()
                || year < today.getYear() - 1 || year > today.getYear() + 1) return false;
        LocalDate occurrence = MonthDay.of(birthday.getMonth(), birthday.getDay()).atYear(year);
        return !today.isBefore(occurrence.minusDays(7)) && !today.isAfter(occurrence.plusDays(7));
    }

    public static ZonedDateTime pendingSnooze(Birthday birthday, ZonedDateTime now) {
        long time = birthday.getSnoozedUntil();
        if (time <= 0 || !canSnooze(birthday, birthday.getSnoozedYear(), now.toLocalDate())) return null;
        return Instant.ofEpochMilli(time).atZone(now.getZone());
    }

    public static boolean canScheduleExactly(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        return manager != null && manager.canScheduleExactAlarms();
    }

    public static void updateAlarm(Context context, Birthday birthday) {
        updateRegularAlarm(context, birthday);
        updateSnoozeAlarm(context, birthday);
    }

    public static void updateRegularAlarm(Context context, Birthday birthday) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) return;

        Intent intent = new Intent(context, BirthdayNotificationReceiver.class);
        Reminder next = nextReminder(birthday, ZonedDateTime.now(ZoneId.systemDefault()));
        if (next == null) {
            cancel(context, manager, birthday.getId(), intent);
            NotificationHelper.cancel(context, birthday.getId());
            return;
        }

        long millis = next.time.toInstant().toEpochMilli();
        intent.putExtra("birthday_id", birthday.getId());
        intent.putExtra(EXTRA_OFFSET, next.daysBefore);
        intent.putExtra(EXTRA_YEAR, next.birthdayDate.getYear());
        intent.putExtra(EXTRA_TIME, millis);
        schedule(context, manager, birthday.getId(), intent, millis);
    }

    public static void updateSnoozeAlarm(Context context, Birthday birthday) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) return;
        Intent intent = new Intent(context, BirthdayNotificationReceiver.class)
                .setAction(BirthdayNotificationReceiver.ACTION_SNOOZE_DELIVERY);
        if (pendingSnooze(birthday, ZonedDateTime.now()) == null) {
            cancel(context, manager, birthday.getId(), intent);
            return;
        }
        intent.putExtra("birthday_id", birthday.getId());
        intent.putExtra(EXTRA_YEAR, birthday.getSnoozedYear());
        intent.putExtra(EXTRA_TIME, birthday.getSnoozedUntil());
        schedule(context, manager, birthday.getId(), intent, birthday.getSnoozedUntil());
    }

    private static void cancel(Context context, AlarmManager manager, int id, Intent intent) {
        PendingIntent old = PendingIntent.getBroadcast(context, id, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (old != null) {
            manager.cancel(old);
            old.cancel();
        }
    }

    private static void schedule(Context context, AlarmManager manager, int id, Intent intent, long millis) {
        PendingIntent pending = PendingIntent.getBroadcast(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (manager.canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending);
                return;
            }
        } catch (SecurityException e) {
            Log.d("BirthdayReminder", "Exact alarm permission changed", e);
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending);
    }
}
