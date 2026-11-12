package com.example.birthdayreminder.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.birthdayreminder.data.Birthday;
import com.example.birthdayreminder.data.BirthdayRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class BirthdayViewModel extends AndroidViewModel {
    private static final AtomicBoolean alarmsInitialized = new AtomicBoolean();

    private final BirthdayRepository repository;
    private final MutableLiveData<BackupState> backupState = new MutableLiveData<>();
    private final MutableLiveData<SaveState> saveState = new MutableLiveData<>();

    public BirthdayViewModel(@NonNull Application application) {
        super(application);
        repository = new BirthdayRepository(application);
    }

    public LiveData<List<Birthday>> getAllBirthdays() {
        return repository.getAllBirthdays();
    }

    public LiveData<Birthday> observeBirthday(int id) {
        return repository.observeBirthday(id);
    }

    public LiveData<SaveState> getSaveState() {
        return saveState;
    }

    public void save(Birthday birthday) {
        if (saveState.getValue() == SaveState.SAVING) return;
        saveState.setValue(SaveState.SAVING);
        repository.save(birthday, () -> saveState.postValue(SaveState.SAVED),
                () -> saveState.postValue(SaveState.FAILED));
    }

    public void refreshAlarmsOnLaunch() {
        if (alarmsInitialized.compareAndSet(false, true)) {
            repository.refreshAlarms(() -> {});
        }
    }

    public void setArchived(int id, boolean archived, Consumer<Boolean> onComplete) {
        Handler main = new Handler(Looper.getMainLooper());
        repository.setArchived(id, archived, success -> main.post(() -> onComplete.accept(success)));
    }

    public void setCelebrated(int id, boolean celebrated, Consumer<Boolean> onComplete) {
        Handler main = new Handler(Looper.getMainLooper());
        repository.setCelebrated(id, celebrated, success -> main.post(() -> onComplete.accept(success)));
    }

    public LiveData<BackupState> getBackupState() {
        return backupState;
    }

    public void runBackup(Uri uri, boolean importing) {
        if (uri == null || backupState.getValue() != null) return;
        backupState.setValue(new BackupState(importing, true, 0, null));
        if (importing) {
            repository.previewBackup(uri, preview -> backupState.postValue(
                    new BackupState(true, false, preview == null ? -1 : preview.newCount, preview)));
        } else {
            repository.exportBackup(uri, count -> backupState.postValue(new BackupState(false, false, count, null)));
        }
    }

    public void confirmRestore() {
        BackupState current = backupState.getValue();
        if (current == null || current.preview == null) return;
        backupState.setValue(new BackupState(true, true, 0, null));
        repository.importBackup(current.preview, count -> backupState.postValue(new BackupState(true, false, count, null)));
    }

    public void clearBackupResult() {
        backupState.setValue(null);
    }

    public enum SaveState { SAVING, SAVED, FAILED }

    public static class BackupState {
        public final boolean importing;
        public final boolean running;
        public final int count;
        public final BirthdayRepository.BackupPreview preview;

        BackupState(boolean importing, boolean running, int count, BirthdayRepository.BackupPreview preview) {
            this.importing = importing;
            this.running = running;
            this.count = count;
            this.preview = preview;
        }
    }
}
