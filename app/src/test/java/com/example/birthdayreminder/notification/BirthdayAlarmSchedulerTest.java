package com.example.birthdayreminder.notification;

import com.example.birthdayreminder.data.Birthday;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BirthdayAlarmSchedulerTest {
    @Test
    public void deliversAllSelectedOffsetsInDateOrder() {
        Birthday birthday = birthday(10, 5, (1 << 7) | (1 << 1) | 1);
        ZonedDateTime now = ZonedDateTime.parse("2026-05-01T08:00+03:00[Europe/Istanbul]");

        BirthdayAlarmScheduler.Reminder week = BirthdayAlarmScheduler.nextReminder(birthday, now);
        BirthdayAlarmScheduler.Reminder dayBefore = BirthdayAlarmScheduler.nextReminder(birthday, week.time);
        BirthdayAlarmScheduler.Reminder sameDay = BirthdayAlarmScheduler.nextReminder(birthday, dayBefore.time);

        assertEquals(LocalDate.of(2026, 5, 3), week.time.toLocalDate());
        assertEquals(LocalDate.of(2026, 5, 9), dayBefore.time.toLocalDate());
        assertEquals(LocalDate.of(2026, 5, 10), sameDay.time.toLocalDate());
        assertEquals(LocalDate.of(2027, 5, 3),
                BirthdayAlarmScheduler.nextReminder(birthday, sameDay.time).time.toLocalDate());
    }

    @Test
    public void supportsSeparateTimesOnTheSameDay() {
        Birthday birthday = birthday(10, 5, 1);
        birthday.setReminderSchedule("7/18:00,0/09:00,0/19:30");
        ZonedDateTime morning = ZonedDateTime.parse("2026-05-10T08:00+03:00[Europe/Istanbul]");
        BirthdayAlarmScheduler.Reminder first = BirthdayAlarmScheduler.nextReminder(birthday, morning);
        BirthdayAlarmScheduler.Reminder second = BirthdayAlarmScheduler.nextReminder(birthday, first.time);

        assertEquals(9, first.time.getHour());
        assertEquals(19, second.time.getHour());
        assertEquals(30, second.time.getMinute());
        long delivered = first.time.toInstant().toEpochMilli();
        birthday.setLastRegularDelivery(delivered);
        assertEquals(second.time, BirthdayAlarmScheduler.nextReminder(birthday, morning).time);
        assertNull(BirthdayAlarmScheduler.validDelivery(birthday, 0, 2026, delivered, first.time));
    }

    @Test
    public void snoozeKeepsItsInstantAcrossMidnightAndAZoneChange() {
        Birthday birthday = birthday(31, 12, 1);
        ZonedDateTime click = ZonedDateTime.parse("2026-12-31T23:35+03:00[Europe/Istanbul]");
        birthday.setSnoozedUntil(click.plusHours(1).toInstant().toEpochMilli());
        birthday.setSnoozedYear(2026);
        ZonedDateTime changedZone = click.withZoneSameInstant(java.time.ZoneId.of("Europe/Berlin"));

        assertEquals(click.plusHours(1).toInstant(),
                BirthdayAlarmScheduler.pendingSnooze(birthday, changedZone).toInstant());
        assertEquals(1, BirthdayAlarmScheduler.pendingSnooze(birthday, click).getDayOfMonth());
        birthday.setCelebratedYear(2026);
        assertNull(BirthdayAlarmScheduler.pendingSnooze(birthday, click));
    }

    @Test
    public void handlesLeapDayAndSkipsTheCelebratedOccurrence() {
        Birthday birthday = birthday(29, 2, 1);
        ZonedDateTime now = ZonedDateTime.parse("2027-02-28T08:00+03:00[Europe/Istanbul]");

        assertEquals(LocalDate.of(2027, 2, 28), BirthdayAlarmScheduler.nextReminder(birthday, now).birthdayDate);
        birthday.setCelebratedYear(2027);
        assertEquals(LocalDate.of(2028, 2, 29), BirthdayAlarmScheduler.nextReminder(birthday, now).birthdayDate);
    }

    @Test
    public void schedulesJanuaryBirthdayInDecemberAndChecksStaleDelivery() {
        Birthday birthday = birthday(1, 1, 1 << 1);
        ZonedDateTime now = ZonedDateTime.parse("2026-12-31T08:00+03:00[Europe/Istanbul]");
        BirthdayAlarmScheduler.Reminder reminder = BirthdayAlarmScheduler.nextReminder(birthday, now);
        long millis = reminder.time.toInstant().toEpochMilli();

        assertEquals(LocalDate.of(2026, 12, 31), reminder.time.toLocalDate());
        assertEquals(LocalDate.of(2027, 1, 1), BirthdayAlarmScheduler.validDelivery(
                birthday, 1, 2027, millis, reminder.time.plusMinutes(30)));
        assertNull(BirthdayAlarmScheduler.validDelivery(birthday, 1, 2027, millis, now));
        birthday.setNotifyHour(10);
        assertNull(BirthdayAlarmScheduler.validDelivery(birthday, 1, 2027, millis, reminder.time.plusHours(2)));
        birthday.setArchived(true);
        assertNull(BirthdayAlarmScheduler.nextReminder(birthday, now));
    }

    @Test
    public void resolvesASkippedHourDuringDaylightSaving() {
        Birthday birthday = birthday(29, 3, 1);
        birthday.setNotifyHour(2);
        birthday.setNotifyMinute(30);
        ZonedDateTime now = ZonedDateTime.parse("2026-03-29T01:30+01:00[Europe/Berlin]");

        BirthdayAlarmScheduler.Reminder reminder = BirthdayAlarmScheduler.nextReminder(birthday, now);
        assertEquals(3, reminder.time.getHour());
        assertEquals(30, reminder.time.getMinute());
    }

    private Birthday birthday(int day, int month, int mask) {
        Birthday birthday = new Birthday("Ada", day, month, 0, null, null, true, 1);
        birthday.changeReminderDays(mask);
        return birthday;
    }
}
