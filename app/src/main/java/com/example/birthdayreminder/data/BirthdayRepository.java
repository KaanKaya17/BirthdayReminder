package com.example.birthdayreminder.data;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.LiveData;

import com.example.birthdayreminder.notification.BirthdayAlarmScheduler;
import com.example.birthdayreminder.notification.NotificationHelper;
import com.example.birthdayreminder.util.BirthdayUtils;
import com.example.birthdayreminder.widget.BirthdayWidget;

import java.time.LocalDate;
import java.time.Instant;
import java.time.MonthDay;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class BirthdayRepository {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final Application application;
    private final BirthdayDatabase database;
    private final BirthdayDao birthdayDao;
    private final LiveData<List<Birthday>> allBirthdays;

    public BirthdayRepository(Application application) {
        this.application = application;
        database = BirthdayDatabase.getDatabase(application);
        birthdayDao = database.birthdayDao();
        allBirthdays = birthdayDao.getAllBirthdays();
    }

    public LiveData<List<Birthday>> getAllBirthdays() {
        return allBirthdays;
    }

    public LiveData<Birthday> observeBirthday(int id) {
        return birthdayDao.observeBirthday(id);
    }

    public void save(Birthday values, Runnable onSuccess, Runnable onError) {
        IO.execute(() -> {
            Birthday saved;
            try {
                saved = database.runInTransaction(() -> {
                    if (values.getId() == 0) {
                        values.setId((int) birthdayDao.insert(values));
                        return values;
                    }

                    Birthday existing = birthdayDao.getById(values.getId());
                    if (existing == null) {
                        throw new IllegalStateException("Birthday no longer exists");
                    }
                    if (!values.isNotifyEnabled() || existing.getDay() != values.getDay()
                            || existing.getMonth() != values.getMonth()) {
                        existing.setSnoozedUntil(0);
                        existing.setSnoozedYear(0);
                    }
                    existing.setName(values.getName());
                    existing.setDay(values.getDay());
                    existing.setMonth(values.getMonth());
                    existing.setYear(values.getYear());
                    existing.setNotes(values.getNotes());
                    existing.setNotifyEnabled(values.isNotifyEnabled());
                    existing.setNotifyDaysBefore(values.getNotifyDaysBefore());
                    existing.setNotifyHour(values.getNotifyHour());
                    existing.setNotifyMinute(values.getNotifyMinute());
                    existing.setExtraReminderDays(values.getExtraReminderDays());
                    existing.setReminderSchedule(values.getReminderSchedule());
                    birthdayDao.update(existing);
                    return existing;
                });
            } catch (RuntimeException e) {
                onError.run();
                return;
            }
            BirthdayAlarmScheduler.updateAlarm(application, saved);
            BirthdayWidget.refresh(application);
            onSuccess.run();
        });
    }

    public void setArchived(int id, boolean archived, Consumer<Boolean> onComplete) {
        IO.execute(() -> {
            Birthday birthday;
            try {
                birthday = database.runInTransaction(() -> birthdayDao.setArchived(id, archived) == 1
                        ? birthdayDao.getById(id) : null);
            } catch (RuntimeException e) {
                onComplete.accept(false);
                return;
            }
            if (birthday != null) BirthdayAlarmScheduler.updateAlarm(application, birthday);
            BirthdayWidget.refresh(application);
            onComplete.accept(birthday != null);
        });
    }

    public void exportBackup(Uri destination, BackupCallback callback) {
        IO.execute(() -> {
            try {
                List<Birthday> birthdays = birthdayDao.getAllBirthdaysSync();
                BirthdayBackup.write(application.getContentResolver(), destination, birthdays);
                new BirthdayPreferences(application).markBackupComplete();
                callback.onComplete(birthdays.size());
            } catch (Exception e) {
                callback.onComplete(-1);
            }
        });
    }

    public void previewBackup(Uri source, Consumer<BackupPreview> callback) {
        IO.execute(() -> {
            BackupPreview preview;
            try {
                List<Birthday> incoming = BirthdayBackup.read(application.getContentResolver(), source);
                int count = BirthdayBackup.newRecords(birthdayDao.getAllBirthdaysSync(), incoming).size();
                preview = new BackupPreview(incoming, count);
            } catch (Exception e) {
                preview = null;
            }
            callback.accept(preview);
        });
    }

    public void importBackup(BackupPreview preview, BackupCallback callback) {
        IO.execute(() -> {
            List<Birthday> added;
            try {
                added = BirthdayBackup.merge(database, preview.records);
            } catch (Exception e) {
                callback.onComplete(-1);
                return;
            }
            for (Birthday birthday : added) {
                BirthdayAlarmScheduler.updateAlarm(application, birthday);
            }
            BirthdayWidget.refresh(application);
            callback.onComplete(added.size());
        });
    }

    public void refreshAlarms(Runnable onComplete) {
        IO.execute(() -> {
            try {
                for (Birthday birthday : birthdayDao.getAllBirthdaysSync()) {
                    BirthdayAlarmScheduler.updateAlarm(application, birthday);
                }
                BirthdayWidget.refresh(application);
            } finally {
                onComplete.run();
            }
        });
    }

    public void deliverAlarm(int id, int offset, int year, long time, Runnable onComplete) {
        IO.execute(() -> {
            try {
                Birthday birthday = birthdayDao.getById(id);
                if (birthday == null || (time > 0 && birthday.getLastRegularDelivery() >= time)) return;
                ZonedDateTime now = ZonedDateTime.now();
                LocalDate occurrence = BirthdayAlarmScheduler.validDelivery(birthday, offset, year, time, now);
                if (occurrence == null) {
                    BirthdayAlarmScheduler.updateRegularAlarm(application, birthday);
                    return;
                }
                deliverRegular(birthday, occurrence, time, now);

                // A single wake-up can deliver coincident reminders, including while the device is idle.
                ZonedDateTime beforeAlarm = Instant.ofEpochMilli(time - 1).atZone(now.getZone());
                for (Birthday other : birthdayDao.getAllBirthdaysSync()) {
                    if (other.getId() == id) continue;
                    BirthdayAlarmScheduler.Reminder reminder = BirthdayAlarmScheduler.nextReminder(other, beforeAlarm);
                    if (reminder != null && reminder.time.toInstant().toEpochMilli() == time
                            && !now.toLocalDate().isAfter(reminder.birthdayDate)) {
                        deliverRegular(other, reminder.birthdayDate, time, now);
                    }
                }
            } finally {
                onComplete.run();
            }
        });
    }

    private void deliverRegular(Birthday birthday, LocalDate occurrence, long time, ZonedDateTime now) {
        if (birthdayDao.recordRegularDelivery(birthday.getId(), time) != 1) return;
        birthday.setLastRegularDelivery(time);
        BirthdayAlarmScheduler.updateRegularAlarm(application, birthday);
        if (birthday.getName() != null) {
            int days = (int) ChronoUnit.DAYS.between(now.toLocalDate(), occurrence);
            NotificationHelper.showBirthdayNotification(
                    application, birthday.getId(), birthday.getName(), days, occurrence.getYear());
        }
    }

    public void snooze(int id, int year, Runnable onComplete) {
        IO.execute(() -> {
            try {
                Birthday birthday = birthdayDao.getById(id);
                ZonedDateTime now = ZonedDateTime.now();
                if (birthday == null || !BirthdayAlarmScheduler.canSnooze(birthday, year, now.toLocalDate())) return;
                long time = now.toInstant().plus(1, ChronoUnit.HOURS).toEpochMilli();
                if (birthdayDao.setSnooze(id, time, year) != 1) return;
                birthday.setSnoozedUntil(time);
                birthday.setSnoozedYear(year);
                BirthdayAlarmScheduler.updateSnoozeAlarm(application, birthday);
                NotificationHelper.cancel(application, id);
            } finally {
                onComplete.run();
            }
        });
    }

    public void deliverSnooze(int id, int year, long time, Runnable onComplete) {
        IO.execute(() -> {
            try {
                Birthday birthday = birthdayDao.getById(id);
                ZonedDateTime now = ZonedDateTime.now();
                if (birthday == null || birthday.getSnoozedUntil() != time
                        || birthday.getSnoozedYear() != year || time > now.toInstant().toEpochMilli()
                        || BirthdayAlarmScheduler.pendingSnooze(birthday, now) == null) return;
                if (birthdayDao.setSnooze(id, 0, 0) != 1) return;
                birthday.setSnoozedUntil(0);
                birthday.setSnoozedYear(0);
                BirthdayAlarmScheduler.updateSnoozeAlarm(application, birthday);
                if (birthday.getName() != null) {
                    NotificationHelper.showSnoozedBirthday(application, id, birthday.getName(), year);
                }
            } finally {
                onComplete.run();
            }
        });
    }

    public void markFromNotification(int id, int year, Runnable onComplete) {
        IO.execute(() -> {
            try {
                Birthday birthday = birthdayDao.getById(id);
                LocalDate today = LocalDate.now();
                if (birthday == null || birthday.isArchived()
                        || year < today.getYear() - 1 || year > today.getYear() + 1) return;
                LocalDate occurrence = MonthDay.of(birthday.getMonth(), birthday.getDay()).atYear(year);
                if (today.isBefore(occurrence.minusDays(7)) || today.isAfter(occurrence.plusDays(7))
                        || year < birthday.getCelebratedYear()) return;
                storeCelebration(birthday, year);
            } finally {
                onComplete.run();
            }
        });
    }

    public void setCelebrated(int id, boolean celebrated, Consumer<Boolean> onComplete) {
        IO.execute(() -> {
            boolean success = false;
            try {
                Birthday birthday = birthdayDao.getById(id);
                if (birthday != null && !birthday.isArchived()) {
                    int year = BirthdayUtils.nextBirthday(birthday.getDay(), birthday.getMonth(), LocalDate.now()).getYear();
                    success = storeCelebration(birthday, celebrated ? year : 0);
                }
            } catch (RuntimeException ignored) {
                // The existing value remains in Room if the write fails.
            }
            onComplete.accept(success);
        });
    }

    private boolean storeCelebration(Birthday birthday, int year) {
        if (birthdayDao.setCelebratedYear(birthday.getId(), year) != 1) return false;
        birthday.setCelebratedYear(year);
        if (birthday.getSnoozedYear() == year) {
            birthday.setSnoozedUntil(0);
            birthday.setSnoozedYear(0);
        }
        if (year != 0) NotificationHelper.cancel(application, birthday.getId());
        BirthdayAlarmScheduler.updateAlarm(application, birthday);
        BirthdayWidget.refresh(application);
        return true;
    }

    public static final class BackupPreview {
        private final List<Birthday> records;
        public final int newCount;

        private BackupPreview(List<Birthday> records, int newCount) {
            this.records = records;
            this.newCount = newCount;
        }
    }

    public interface BackupCallback {
        void onComplete(int count);
    }
}
