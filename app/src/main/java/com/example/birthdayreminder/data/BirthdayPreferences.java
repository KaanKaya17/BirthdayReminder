package com.example.birthdayreminder.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalTime;

public final class BirthdayPreferences {
    private static final String DEFAULT_MINUTE_OF_DAY = "default_minute_of_day";
    private static final String LAST_BACKUP = "last_backup";
    private final SharedPreferences preferences;

    public BirthdayPreferences(Context context) {
        preferences = context.getSharedPreferences("birthday_preferences", Context.MODE_PRIVATE);
    }

    public LocalTime getDefaultTime() {
        return LocalTime.ofSecondOfDay(preferences.getInt(DEFAULT_MINUTE_OF_DAY, 9 * 60) * 60L);
    }

    public void setDefaultTime(LocalTime time) {
        preferences.edit().putInt(DEFAULT_MINUTE_OF_DAY, time.getHour() * 60 + time.getMinute()).apply();
    }

    public long getLastBackupTime() {
        return preferences.getLong(LAST_BACKUP, 0);
    }

    void markBackupComplete() {
        preferences.edit().putLong(LAST_BACKUP, System.currentTimeMillis()).apply();
    }
}
