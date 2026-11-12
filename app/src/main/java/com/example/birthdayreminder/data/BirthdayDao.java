package com.example.birthdayreminder.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BirthdayDao {

    @Query("SELECT * FROM birthdays ORDER BY month ASC, day ASC")
    LiveData<List<Birthday>> getAllBirthdays();

    @Query("SELECT * FROM birthdays ORDER BY month ASC, day ASC")
    List<Birthday> getAllBirthdaysSync();

    @Query("SELECT * FROM birthdays WHERE id = :id")
    Birthday getById(int id);

    @Query("SELECT * FROM birthdays WHERE id = :id")
    LiveData<Birthday> observeBirthday(int id);

    @Insert
    long insert(Birthday birthday);

    @Update
    void update(Birthday birthday);

    @Query("UPDATE birthdays SET archived = :archived, snoozedUntil = CASE WHEN :archived THEN 0 ELSE snoozedUntil END, "
            + "snoozedYear = CASE WHEN :archived THEN 0 ELSE snoozedYear END WHERE id = :id")
    int setArchived(int id, boolean archived);

    @Query("UPDATE birthdays SET celebratedYear = :year, snoozedUntil = CASE WHEN snoozedYear = :year THEN 0 ELSE snoozedUntil END, "
            + "snoozedYear = CASE WHEN snoozedYear = :year THEN 0 ELSE snoozedYear END WHERE id = :id")
    int setCelebratedYear(int id, int year);

    @Query("UPDATE birthdays SET snoozedUntil = :time, snoozedYear = :year WHERE id = :id")
    int setSnooze(int id, long time, int year);

    @Query("UPDATE birthdays SET lastRegularDelivery = :time WHERE id = :id AND lastRegularDelivery < :time")
    int recordRegularDelivery(int id, long time);
}
