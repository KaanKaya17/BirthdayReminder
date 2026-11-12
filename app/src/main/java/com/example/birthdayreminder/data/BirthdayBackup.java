package com.example.birthdayreminder.data;

import android.content.ContentResolver;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BirthdayBackup {
    private static final String FORMAT = "birthday-reminder";
    private static final int VERSION = 3;
    private static final int MAX_BYTES = 32 * 1024 * 1024;

    private BirthdayBackup() {}

    static void write(ContentResolver resolver, Uri uri, List<Birthday> birthdays)
            throws IOException, JSONException {
        JSONArray entries = new JSONArray();
        for (Birthday birthday : birthdays) {
            entries.put(encodeFields(birthday)
                    .put("notifyHour", birthday.getNotifyHour())
                    .put("notifyMinute", birthday.getNotifyMinute())
                    .put("notifyDaysBefore", birthday.getNotifyDaysBefore())
                    .put("extraReminderDays", birthday.getExtraReminderDays())
                    .put("reminderSchedule", nullable(birthday.getReminderSchedule()))
                    .put("archived", birthday.isArchived())
                    .put("celebratedYear", birthday.getCelebratedYear()));
        }
        byte[] content = new JSONObject()
                .put("format", FORMAT)
                .put("version", VERSION)
                .put("birthdays", entries)
                .toString(2)
                .getBytes(StandardCharsets.UTF_8);
        if (content.length > MAX_BYTES) {
            throw new IOException("Backup exceeds the supported size");
        }
        try (OutputStream output = resolver.openOutputStream(uri, "wt")) {
            if (output == null) {
                throw new IOException("Unable to open backup destination");
            }
            output.write(content);
        }
    }

    static List<Birthday> read(ContentResolver resolver, Uri uri)
            throws IOException, JSONException {
        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) {
                throw new IOException("Unable to open backup");
            }
            byte[] content = input.readNBytes(MAX_BYTES + 1);
            if (content.length > MAX_BYTES) {
                throw new IOException("Backup exceeds the supported size");
            }
            JSONTokener parser = new JSONTokener(new String(content, StandardCharsets.UTF_8));
            Object root = parser.nextValue();
            if (!(root instanceof JSONObject) || parser.nextClean() != '\0') {
                throw new JSONException("Invalid backup document");
            }
            JSONObject document = (JSONObject) root;
            int version = readInt(document, "version");
            if (!FORMAT.equals(document.get("format")) || version < 1 || version > VERSION) {
                throw new JSONException("Unsupported backup format");
            }

            JSONArray entries = document.getJSONArray("birthdays");
            List<Birthday> birthdays = new ArrayList<>();
            for (int i = 0; i < entries.length(); i++) {
                birthdays.add(decode(entries.getJSONObject(i), version));
            }
            return birthdays;
        }
    }

    static List<Birthday> merge(BirthdayDatabase database, List<Birthday> incoming)
            throws Exception {
        return database.runInTransaction(() -> {
            BirthdayDao dao = database.birthdayDao();
            List<Birthday> added = newRecords(dao.getAllBirthdaysSync(), incoming);
            for (Birthday birthday : added) {
                birthday.setId((int) dao.insert(birthday));
            }
            return added;
        });
    }

    static List<Birthday> newRecords(List<Birthday> existing, List<Birthday> incoming) throws JSONException {
        Map<String, Integer> existingCounts = new HashMap<>();
        for (Birthday birthday : existing) {
            existingCounts.merge(fingerprint(birthday), 1, Integer::sum);
        }
        List<Birthday> added = new ArrayList<>();
        for (Birthday birthday : incoming) {
            // A local match keeps its current archive and celebration state.
            String fields = fingerprint(birthday);
            int matches = existingCounts.getOrDefault(fields, 0);
            if (matches > 0) {
                existingCounts.put(fields, matches - 1);
            } else {
                added.add(birthday);
            }
        }
        return added;
    }

    private static String fingerprint(Birthday birthday) throws JSONException {
        return encodeFields(birthday)
                .put("reminderSchedule", ReminderSlot.encode(birthday.reminderSlots())).toString();
    }

    private static JSONObject encodeFields(Birthday birthday) throws JSONException {
        return new JSONObject()
                .put("name", nullable(birthday.getName()))
                .put("day", birthday.getDay())
                .put("month", birthday.getMonth())
                .put("year", birthday.getYear())
                .put("notes", nullable(birthday.getNotes()))
                .put("photoUri", nullable(birthday.getPhotoUri()))
                .put("notifyEnabled", birthday.isNotifyEnabled())
                .put("createdAt", birthday.getCreatedAt());
    }

    private static Birthday decode(JSONObject entry, int version) throws JSONException {
        int day = readInt(entry, "day");
        int month = readInt(entry, "month");
        int year = readInt(entry, "year");
        try {
            LocalDate.of(year == 0 ? 2000 : year, month, day);
        } catch (DateTimeException e) {
            throw new JSONException("Invalid birthday date");
        }

        int daysBefore = readInt(entry, "notifyDaysBefore");
        int hour = readInt(entry, "notifyHour");
        int minute = readInt(entry, "notifyMinute");
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59
                || (daysBefore != 0 && daysBefore != 1 && daysBefore != 2 && daysBefore != 7)) {
            throw new JSONException("Invalid reminder time");
        }

        Birthday birthday = new Birthday(readNullableText(entry, "name"), day, month, year,
                readNullableText(entry, "notes"), readNullableText(entry, "photoUri"),
                readBoolean(entry, "notifyEnabled"), readLong(entry, "createdAt"));
        birthday.setNotifyDaysBefore(daysBefore);
        birthday.setNotifyHour(hour);
        birthday.setNotifyMinute(minute);
        birthday.setArchived(readBoolean(entry, "archived"));
        if (version >= 2) {
            int extraDays = readInt(entry, "extraReminderDays");
            int celebratedYear = readInt(entry, "celebratedYear");
            if ((extraDays & ~Birthday.ALLOWED_REMINDER_MASK) != 0
                    || (extraDays & (1 << daysBefore)) != 0 || celebratedYear < 0) {
                throw new JSONException("Invalid reminder or celebration state");
            }
            birthday.setExtraReminderDays(extraDays);
            birthday.setCelebratedYear(celebratedYear);
        }
        if (version >= 3) {
            String schedule = readNullableText(entry, "reminderSchedule");
            if (schedule != null) {
                try {
                    if (ReminderSlot.decode(schedule).isEmpty() && birthday.isNotifyEnabled()) {
                        throw new JSONException("Missing reminder schedule");
                    }
                } catch (IllegalArgumentException | DateTimeException e) {
                    throw new JSONException("Invalid reminder schedule");
                }
                birthday.setReminderSchedule(schedule);
            }
        }
        return birthday;
    }

    private static Object nullable(String value) {
        return value == null ? JSONObject.NULL : value;
    }

    private static String readNullableText(JSONObject entry, String key) throws JSONException {
        Object value = entry.get(key);
        if (value == JSONObject.NULL) return null;
        if (value instanceof String) return (String) value;
        throw new JSONException("Expected text for " + key);
    }

    private static boolean readBoolean(JSONObject entry, String key) throws JSONException {
        Object value = entry.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        throw new JSONException("Expected boolean for " + key);
    }

    private static long readLong(JSONObject entry, String key) throws JSONException {
        Object value = entry.get(key);
        if (value instanceof Integer || value instanceof Long) return ((Number) value).longValue();
        throw new JSONException("Expected integer for " + key);
    }

    private static int readInt(JSONObject entry, String key) throws JSONException {
        long value = readLong(entry, key);
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) return (int) value;
        throw new JSONException("Integer out of range for " + key);
    }
}
