package com.example.birthdayreminder.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Birthday.class}, version = 1, exportSchema = false)
public abstract class BirthdayDatabase extends RoomDatabase {

    public abstract BirthdayDao birthdayDao();

    private static volatile BirthdayDatabase INSTANCE;

    public static BirthdayDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (BirthdayDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            BirthdayDatabase.class,
                            "birthday_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
