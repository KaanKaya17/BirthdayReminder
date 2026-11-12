package com.example.birthdayreminder.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Birthday.class}, version = 5, exportSchema = false)
public abstract class BirthdayDatabase extends RoomDatabase {

    public static final String NAME = "birthday_database";

    public abstract BirthdayDao birthdayDao();

    private static volatile BirthdayDatabase INSTANCE;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE birthdays ADD COLUMN notifyDaysBefore INTEGER NOT NULL DEFAULT 1");
            database.execSQL("ALTER TABLE birthdays ADD COLUMN notifyHour INTEGER NOT NULL DEFAULT 9");
            database.execSQL("ALTER TABLE birthdays ADD COLUMN notifyMinute INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE birthdays ADD COLUMN archived INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE birthdays ADD COLUMN extraReminderDays INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE birthdays ADD COLUMN celebratedYear INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE birthdays ADD COLUMN reminderSchedule TEXT");
            database.execSQL("ALTER TABLE birthdays ADD COLUMN snoozedUntil INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE birthdays ADD COLUMN snoozedYear INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE birthdays ADD COLUMN lastRegularDelivery INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static BirthdayDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (BirthdayDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = open(context.getApplicationContext(), NAME);
                }
            }
        }
        return INSTANCE;
    }

    static BirthdayDatabase open(Context context, String name) {
        return Room.databaseBuilder(context, BirthdayDatabase.class, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build();
    }
}
