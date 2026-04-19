package com.example.birthdayreminder.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Birthday.class}, version = 2, exportSchema = false)
public abstract class BirthdayDatabase extends RoomDatabase {

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

    public static BirthdayDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (BirthdayDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            BirthdayDatabase.class,
                            "birthday_database"
                    )
                    .addMigrations(MIGRATION_1_2)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
