package com.example.birthdayreminder.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
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

    @Query("SELECT * FROM birthdays WHERE name LIKE '%' || :query || '%' ORDER BY month ASC, day ASC")
    LiveData<List<Birthday>> searchByName(String query);

    @Query("SELECT * FROM birthdays WHERE id = :id")
    Birthday getById(int id);

    @Insert
    long insert(Birthday birthday);

    @Update
    void update(Birthday birthday);

    @Delete
    void delete(Birthday birthday);

    @Query("DELETE FROM birthdays WHERE id = :id")
    void deleteById(int id);
}
