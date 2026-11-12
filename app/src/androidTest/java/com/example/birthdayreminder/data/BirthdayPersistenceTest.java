package com.example.birthdayreminder.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class BirthdayPersistenceTest {
    private static final String TEST_DATABASE = "birthday_persistence_test";
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private BirthdayDatabase database;

    @After
    public void removeTestDatabase() {
        if (database != null) database.close();
        context.deleteDatabase(TEST_DATABASE);
    }

    @Test
    public void migratesVersionOneWithoutLosingStoredFields() {
        createLegacyDatabase(1);
        database = BirthdayDatabase.open(context, TEST_DATABASE);
        assertLegacyFields(database.birthdayDao().getById(42), 1, 9, 0);
    }

    @Test
    public void migratesVersionTwoWithoutResettingReminderPreferences() {
        createLegacyDatabase(2);
        database = BirthdayDatabase.open(context, TEST_DATABASE);
        assertLegacyFields(database.birthdayDao().getById(42), 7, 19, 35);
    }

    @Test
    public void migratesVersionThreeKeepingArchivedRowsAndTheirOriginalAlarm() {
        createLegacyDatabase(3);
        database = BirthdayDatabase.open(context, TEST_DATABASE);
        Birthday birthday = database.birthdayDao().getById(42);
        assertTrue(birthday.isArchived());
        assertEquals(1 << 7, birthday.reminderMask());
        assertEquals(0, birthday.getCelebratedYear());
        database.birthdayDao().setArchived(42, false);
        assertLegacyFields(database.birthdayDao().getById(42), 7, 19, 35);
    }

    @Test
    public void migratesVersionFourKeepingAllPreviouslySelectedReminderDays() {
        createLegacyDatabase(4);
        database = BirthdayDatabase.open(context, TEST_DATABASE);
        Birthday birthday = database.birthdayDao().getById(42);
        assertTrue(birthday.isArchived());
        assertEquals("7/19:35,0/19:35", ReminderSlot.encode(birthday.reminderSlots()));
        assertEquals(2026, birthday.getCelebratedYear());
        assertEquals(0L, birthday.getSnoozedUntil());
    }

    @Test
    public void keepsSeparateDeliveryAndSnoozeStateForMatchingBirthdays() {
        database = BirthdayDatabase.open(context, TEST_DATABASE);
        BirthdayDao dao = database.birthdayDao();
        int first = (int) dao.insert(new Birthday("Ada", 15, 5, 0, null, null, true, 1));
        int second = (int) dao.insert(new Birthday("Ece", 15, 5, 0, null, null, true, 2));
        assertEquals(1, dao.recordRegularDelivery(first, 1000));
        assertEquals(1, dao.recordRegularDelivery(second, 1000));
        assertEquals(0, dao.recordRegularDelivery(first, 1000));
        dao.setSnooze(first, 3601000, 2026);
        assertEquals(3601000, dao.getById(first).getSnoozedUntil());
        assertEquals(0, dao.getById(second).getSnoozedUntil());
        dao.setArchived(first, true);
        assertEquals(0, dao.getById(first).getSnoozedUntil());
        assertEquals(1000, dao.getById(second).getLastRegularDelivery());
    }

    @Test
    public void archiveAndRestoreKeepTheOriginalRecordAndReminderSettings() {
        createLegacyDatabase(2);
        database = BirthdayDatabase.open(context, TEST_DATABASE);
        database.birthdayDao().setArchived(42, true);
        database.close();

        database = BirthdayDatabase.open(context, TEST_DATABASE);
        BirthdayDao dao = database.birthdayDao();
        assertTrue(dao.getById(42).isArchived());
        dao.setArchived(42, false);
        assertLegacyFields(dao.getById(42), 7, 19, 35);
    }

    @Test
    public void backupRoundTripAddsArchivedAndActiveRowsWithoutDuplicatingExistingRecords() throws Exception {
        File backup = File.createTempFile("birthdays", ".json", context.getCacheDir());
        Uri uri = Uri.fromFile(backup);
        database = BirthdayDatabase.open(context, TEST_DATABASE);
        try {
            Birthday existing = new Birthday("Çağrı", 29, 2, 2000, "Yeni not", null, true, 22L);
            Birthday archived = new Birthday("İpek", 14, 8, 0, "Çiçek\nve kart", "content://photo/7", true, 23L);
            archived.setArchived(true);
            archived.setNotifyDaysBefore(7);
            archived.setNotifyHour(18);
            archived.setNotifyMinute(45);
            archived.changeReminderDays((1 << 7) | 1);
            archived.setCelebratedYear(2026);
            archived.setReminderSchedule("7/18:45,0/10:30,0/20:00");
            int existingId = (int) database.birthdayDao().insert(existing);
            database.birthdayDao().setArchived(existingId, true);

            Birthday active = new Birthday("Deniz", 2, 1, 1999, null, null, false, 24L);
            BirthdayBackup.write(context.getContentResolver(), uri, List.of(existing, archived, active));

            List<Birthday> incoming = BirthdayBackup.read(context.getContentResolver(), uri);
            assertEquals(2, BirthdayBackup.newRecords(database.birthdayDao().getAllBirthdaysSync(), incoming).size());
            assertEquals(1, database.birthdayDao().getAllBirthdaysSync().size());
            List<Birthday> added = BirthdayBackup.merge(database, incoming);
            assertEquals(2, added.size());
            Birthday restored = added.get(0);
            assertTrue(restored.isArchived());
            assertEquals("İpek", restored.getName());
            assertEquals("Çiçek\nve kart", restored.getNotes());
            assertEquals("content://photo/7", restored.getPhotoUri());
            assertEquals(0, restored.getYear());
            assertTrue(restored.isNotifyEnabled());
            assertEquals(7, restored.getNotifyDaysBefore());
            assertEquals(18, restored.getNotifyHour());
            assertEquals(45, restored.getNotifyMinute());
            assertEquals(23L, restored.getCreatedAt());
            assertEquals((1 << 7) | 1, restored.reminderMask());
            assertEquals(2026, restored.getCelebratedYear());
            assertEquals("7/18:45,0/10:30,0/20:00", ReminderSlot.encode(restored.reminderSlots()));
            assertFalse(added.get(1).isArchived());
            assertTrue(database.birthdayDao().getById(existingId).isArchived());

            assertTrue(BirthdayBackup.merge(
                    database, BirthdayBackup.read(context.getContentResolver(), uri)).isEmpty());
            assertEquals(3, database.birthdayDao().getAllBirthdaysSync().size());
        } finally {
            Files.deleteIfExists(backup.toPath());
        }
    }

    @Test
    public void readsThePreviousBackupFormatIntoACleanDatabase() throws Exception {
        File backup = File.createTempFile("birthdays-v1", ".json", context.getCacheDir());
        database = BirthdayDatabase.open(context, TEST_DATABASE);
        try {
            String previousBackup = "{\"format\":\"birthday-reminder\",\"version\":1,\"birthdays\":["
                    + "{\"name\":\"Ada\",\"day\":29,\"month\":2,\"year\":0,\"notes\":\"Çiçek\","
                    + "\"photoUri\":null,\"notifyEnabled\":true,\"notifyDaysBefore\":2,\"notifyHour\":18,"
                    + "\"notifyMinute\":30,\"createdAt\":1234,\"archived\":true}]}";
            Files.write(backup.toPath(), previousBackup.getBytes(StandardCharsets.UTF_8));
            List<Birthday> added = BirthdayBackup.merge(database,
                    BirthdayBackup.read(context.getContentResolver(), Uri.fromFile(backup)));
            assertEquals(1, added.size());
            Birthday restored = database.birthdayDao().getById(added.get(0).getId());
            assertEquals("Çiçek", restored.getNotes());
            assertEquals(0, restored.getYear());
            assertEquals(1 << 2, restored.reminderMask());
            assertEquals(18, restored.getNotifyHour());
            assertTrue(restored.isArchived());
        } finally {
            Files.deleteIfExists(backup.toPath());
        }
    }

    @Test
    public void invalidBackupLeavesDatabaseIntact() throws Exception {
        createLegacyDatabase(2);
        File backup = File.createTempFile("birthdays", ".json", context.getCacheDir());
        database = BirthdayDatabase.open(context, TEST_DATABASE);
        try {
            Uri uri = Uri.fromFile(backup);
            Birthday invalid = new Birthday("Geçersiz", 31, 2, 2000, null, null, false, 1L);
            BirthdayBackup.write(context.getContentResolver(), uri, List.of(invalid));
            try {
                BirthdayBackup.merge(database, BirthdayBackup.read(context.getContentResolver(), uri));
                fail("Invalid backup was accepted");
            } catch (JSONException expected) {
                assertEquals(1, database.birthdayDao().getAllBirthdaysSync().size());
                assertLegacyFields(database.birthdayDao().getById(42), 7, 19, 35);
            }
        } finally {
            Files.deleteIfExists(backup.toPath());
        }
    }

    private void createLegacyDatabase(int version) {
        try (SQLiteDatabase database = context.openOrCreateDatabase(TEST_DATABASE, Context.MODE_PRIVATE, null)) {
            String reminders = version >= 2
                    ? ", notifyDaysBefore INTEGER NOT NULL, notifyHour INTEGER NOT NULL, notifyMinute INTEGER NOT NULL"
                    : "";
            database.execSQL("CREATE TABLE birthdays (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "name TEXT, day INTEGER NOT NULL, month INTEGER NOT NULL, year INTEGER NOT NULL, "
                    + "notes TEXT, photoUri TEXT, notifyEnabled INTEGER NOT NULL, createdAt INTEGER NOT NULL"
                    + reminders + (version >= 3 ? ", archived INTEGER NOT NULL DEFAULT 0" : "")
                    + (version >= 4 ? ", extraReminderDays INTEGER NOT NULL DEFAULT 0,"
                    + " celebratedYear INTEGER NOT NULL DEFAULT 0" : "") + ")");
            database.execSQL("INSERT INTO birthdays VALUES "
                    + "(42, 'Çağrı', 29, 2, 2000, 'Hediye fikri', 'content://photo/42', 1, 1234"
                    + (version >= 2 ? ", 7, 19, 35" : "") + (version >= 3 ? ", 1" : "")
                    + (version >= 4 ? ", 1, 2026" : "") + ")");
            database.setVersion(version);
        }
    }

    private void assertLegacyFields(Birthday birthday, int daysBefore, int hour, int minute) {
        assertEquals(42, birthday.getId());
        assertEquals("Çağrı", birthday.getName());
        assertEquals(29, birthday.getDay());
        assertEquals(2, birthday.getMonth());
        assertEquals(2000, birthday.getYear());
        assertEquals("Hediye fikri", birthday.getNotes());
        assertEquals("content://photo/42", birthday.getPhotoUri());
        assertTrue(birthday.isNotifyEnabled());
        assertEquals(1234L, birthday.getCreatedAt());
        assertEquals(daysBefore, birthday.getNotifyDaysBefore());
        assertEquals(hour, birthday.getNotifyHour());
        assertEquals(minute, birthday.getNotifyMinute());
        assertFalse(birthday.isArchived());
    }
}
