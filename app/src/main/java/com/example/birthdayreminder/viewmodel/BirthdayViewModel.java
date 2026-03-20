package com.example.birthdayreminder.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.birthdayreminder.data.Birthday;
import com.example.birthdayreminder.data.BirthdayRepository;

import java.util.List;

public class BirthdayViewModel extends AndroidViewModel {

    private final BirthdayRepository repository;
    private final LiveData<List<Birthday>> allBirthdays;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");

    public BirthdayViewModel(@NonNull Application application) {
        super(application);
        repository = new BirthdayRepository(application);
        allBirthdays = repository.getAllBirthdays();
    }

    public LiveData<List<Birthday>> getAllBirthdays() {
        return allBirthdays;
    }

    public LiveData<List<Birthday>> getSearchResults() {
        return Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.isEmpty()) {
                return repository.getAllBirthdays();
            }
            return repository.searchByName(query);
        });
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public void insert(Birthday birthday, BirthdayRepository.OnInsertCallback callback) {
        repository.insert(birthday, callback);
    }

    public void update(Birthday birthday) {
        repository.update(birthday);
    }

    public void delete(Birthday birthday) {
        repository.delete(birthday);
    }

    public void deleteById(int id) {
        repository.deleteById(id);
    }

    public Birthday getById(int id) {
        return repository.getById(id);
    }

    public List<Birthday> getAllBirthdaysSync() {
        return repository.getAllBirthdaysSync();
    }
}
