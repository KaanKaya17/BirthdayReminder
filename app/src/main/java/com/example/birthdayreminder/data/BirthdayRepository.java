package com.example.birthdayreminder.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BirthdayRepository {

    private final BirthdayDao birthdayDao;
    private final LiveData<List<Birthday>> allBirthdays;
    private final ExecutorService executorService;

    public BirthdayRepository(Application application) {
        BirthdayDatabase db = BirthdayDatabase.getDatabase(application);
        birthdayDao = db.birthdayDao();
        allBirthdays = birthdayDao.getAllBirthdays();
        executorService = Executors.newFixedThreadPool(2);
    }

    public LiveData<List<Birthday>> getAllBirthdays() {
        return allBirthdays;
    }

    public List<Birthday> getAllBirthdaysSync() {
        return birthdayDao.getAllBirthdaysSync();
    }

    public LiveData<List<Birthday>> searchByName(String query) {
        return birthdayDao.searchByName(query);
    }

    public Birthday getById(int id) {
        return birthdayDao.getById(id);
    }

    public void insert(Birthday birthday, OnInsertCallback callback) {
        executorService.execute(() -> {
            long id = birthdayDao.insert(birthday);
            if (callback != null) {
                callback.onInserted((int) id);
            }
        });
    }

    public void update(Birthday birthday) {
        executorService.execute(() -> birthdayDao.update(birthday));
    }

    public void delete(Birthday birthday) {
        executorService.execute(() -> birthdayDao.delete(birthday));
    }

    public void deleteById(int id) {
        executorService.execute(() -> birthdayDao.deleteById(id));
    }

    public interface OnInsertCallback {
        void onInserted(int id);
    }
}
