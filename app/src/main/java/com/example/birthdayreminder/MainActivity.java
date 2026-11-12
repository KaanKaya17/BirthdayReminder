package com.example.birthdayreminder;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.birthdayreminder.adapter.BirthdayAdapter;
import com.example.birthdayreminder.data.Birthday;
import com.example.birthdayreminder.data.BirthdayPreferences;
import com.example.birthdayreminder.databinding.ActivityMainBinding;
import com.example.birthdayreminder.databinding.DialogBirthdayBackupBinding;
import com.example.birthdayreminder.notification.NotificationHelper;
import com.example.birthdayreminder.util.BirthdayUtils;
import com.example.birthdayreminder.viewmodel.BirthdayViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final int UPCOMING_DAYS = 30;
    private static final DateTimeFormatter BACKUP_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", TURKISH);

    private ActivityMainBinding binding;
    private BirthdayViewModel viewModel;
    private BirthdayAdapter birthdayAdapter;
    private List<Birthday> birthdays = List.of();
    private boolean loaded;
    private AlertDialog backupPreviewDialog;

    private final ActivityResultLauncher<String> exportBackup = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> viewModel.runBackup(uri, false));
    private final ActivityResultLauncher<String[]> importBackup = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> viewModel.runBackup(uri, true));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(BirthdayViewModel.class);
        birthdayAdapter = new BirthdayAdapter(this::openDetails);
        binding.birthdayRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.birthdayRecyclerView.setAdapter(birthdayAdapter);
        if (savedInstanceState != null) {
            binding.filterGroup.check(savedInstanceState.getInt("filter", R.id.filterAll));
        }
        binding.filterGroup.setOnCheckedStateChangeListener((group, ids) -> updateDashboard());
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateDashboard();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        binding.fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AddEditBirthdayActivity.class)));
        binding.btnMore.setOnClickListener(v -> showMenu());
        binding.btnNotifications.setOnClickListener(v -> openReminderSettings());
        setupSwipeToArchive();

        viewModel.getAllBirthdays().observe(this, records -> {
            birthdays = records;
            loaded = true;
            updateDashboard();
        });
        viewModel.getBackupState().observe(this, this::showBackupState);
        viewModel.refreshAlarmsOnLaunch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboard();
    }

    @Override
    protected void onDestroy() {
        if (backupPreviewDialog != null) backupPreviewDialog.dismiss();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putInt("filter", binding.filterGroup.getCheckedChipId());
        super.onSaveInstanceState(state);
    }

    private void updateDashboard() {
        if (!loaded) return;
        List<Birthday> sorted = new ArrayList<>(birthdays);
        LocalDate today = LocalDate.now();
        LocalDate upcomingThrough = today.plusDays(UPCOMING_DAYS);
        Map<Integer, LocalDate> occurrences = new HashMap<>();
        for (Birthday birthday : birthdays) {
            occurrences.put(birthday.getId(), BirthdayUtils.nextBirthday(birthday.getDay(), birthday.getMonth(), today));
        }
        sorted.sort(Comparator.comparing(b -> occurrences.get(b.getId())));

        int filter = binding.filterGroup.getCheckedChipId();
        CharSequence search = binding.searchEditText.getText();
        String query = search == null ? "" : search.toString().trim().toLowerCase(TURKISH);
        List<Birthday> visible = new ArrayList<>();
        Birthday next = null;
        int activeCount = 0;
        int upcomingCount = 0;
        boolean hasReminders = false;
        for (Birthday birthday : sorted) {
            boolean upcoming = !occurrences.get(birthday.getId()).isAfter(upcomingThrough);
            if (!birthday.isArchived()) {
                if (next == null) next = birthday;
                activeCount++;
                if (upcoming) upcomingCount++;
                hasReminders |= birthday.isNotifyEnabled();
            }
            if (birthday.isArchived() == (filter == R.id.filterArchive)
                    && (filter != R.id.filterUpcoming || upcoming)
                    && (query.isEmpty() || contains(birthday.getName(), query)
                        || contains(birthday.getNotes(), query))) {
                visible.add(birthday);
            }
        }
        showNextBirthday(next, today);
        binding.dashboardCounts.setText(getString(R.string.dashboard_counts, activeCount, upcomingCount));
        binding.btnNotifications.setVisibility(hasReminders && !NotificationHelper.isAvailable(this)
                ? View.VISIBLE : View.GONE);
        birthdayAdapter.setBirthdays(visible);
        binding.birthdayRecyclerView.setVisibility(visible.isEmpty() ? View.GONE : View.VISIBLE);
        binding.emptyState.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
        if (visible.isEmpty()) {
            updateEmptyState(filter, !query.isEmpty());
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(TURKISH).contains(query);
    }

    private void showNextBirthday(Birthday birthday, LocalDate today) {
        binding.nextBirthdayCard.setVisibility(birthday == null ? View.GONE : View.VISIBLE);
        if (birthday == null) return;
        binding.nextName.setText(birthday.getName());
        binding.nextBirthdayCard.setOnClickListener(v -> openDetails(birthday));
        LocalDate occurrence = BirthdayUtils.nextBirthday(birthday.getDay(), birthday.getMonth(), today);
        long days = ChronoUnit.DAYS.between(today, occurrence);
        binding.nextDate.setText(getString(R.string.next_birthday_date,
                BirthdayUtils.formatDate(occurrence.getDayOfMonth(), occurrence.getMonthValue(), 0),
                BirthdayUtils.formatDaysRemaining(days)));
    }

    private void updateEmptyState(int filter, boolean searching) {
        int title = R.string.no_birthdays_yet;
        int hint = R.string.no_birthdays_hint;
        if (searching) {
            title = R.string.no_search_results;
            hint = R.string.no_search_hint;
        } else if (filter == R.id.filterArchive) {
            title = R.string.archive_empty;
            hint = R.string.archive_hint;
        } else if (filter == R.id.filterUpcoming) {
            title = R.string.no_upcoming;
            hint = R.string.no_upcoming_hint;
        } else if (!birthdays.isEmpty()) {
            title = R.string.no_active_birthdays;
            hint = R.string.restore_hint;
        }
        binding.emptyTitle.setText(title);
        binding.emptyHint.setText(hint);
    }

    private void openDetails(Birthday birthday) {
        Intent intent = new Intent(this, BirthdayDetailActivity.class);
        intent.putExtra("birthday_id", birthday.getId());
        startActivity(intent);
    }

    private void setupSwipeToArchive() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START) {
            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder holder) {
                return binding.filterGroup.getCheckedChipId() == R.id.filterArchive
                        ? 0 : super.getSwipeDirs(recyclerView, holder);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder holder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder holder, int direction) {
                int position = holder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                int id = birthdayAdapter.getBirthdayAt(position).getId();
                viewModel.setArchived(id, true, success -> {
                    if (isDestroyed() || isFinishing()) return;
                    if (success) {
                        Snackbar.make(binding.getRoot(), R.string.birthday_archived, Snackbar.LENGTH_LONG)
                                .setAnchorView(binding.fabAdd)
                                .setAction(R.string.undo, v -> viewModel.setArchived(id, false, restored -> {
                                    if (!restored && !isDestroyed()) showMessage(R.string.archive_error);
                                }))
                                .show();
                    } else {
                        updateDashboard();
                        showMessage(R.string.archive_error);
                    }
                });
            }
        }).attachToRecyclerView(binding.birthdayRecyclerView);
    }

    private void showMessage(int message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                .setAnchorView(binding.fabAdd)
                .show();
    }

    private void showMenu() {
        DialogBirthdayBackupBinding menu = DialogBirthdayBackupBinding.inflate(getLayoutInflater());
        long lastBackup = new BirthdayPreferences(this).getLastBackupTime();
        menu.lastBackupTime.setText(lastBackup == 0 ? getString(R.string.last_backup_none)
                : getString(R.string.last_backup_at,
                        BACKUP_DATE.format(Instant.ofEpochMilli(lastBackup).atZone(ZoneId.systemDefault()))));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dashboard_more)
                .setView(menu.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .show();
        menu.btnExport.setOnClickListener(v -> {
            dialog.dismiss();
            exportBackup.launch("dogum-gunleri-" + LocalDate.now() + ".json");
        });
        menu.btnImport.setOnClickListener(v -> {
            dialog.dismiss();
            importBackup.launch(new String[] {"*/*"});
        });
        menu.btnReminderSettings.setOnClickListener(v -> {
            dialog.dismiss();
            openReminderSettings();
        });
    }

    private void openReminderSettings() {
        startActivity(new Intent(this, ReminderSettingsActivity.class));
    }

    private void showBackupState(BirthdayViewModel.BackupState state) {
        binding.btnMore.setEnabled(state == null || (!state.running && state.preview == null));
        if (state == null) return;
        if (state.preview != null && state.count > 0) {
            backupPreviewDialog = new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.backup_preview_title)
                    .setMessage(getString(R.string.backup_preview_body, state.count))
                    .setPositiveButton(R.string.restore, (dialog, which) -> viewModel.confirmRestore())
                    .setNegativeButton(R.string.cancel, (dialog, which) -> viewModel.clearBackupResult())
                    .setOnCancelListener(dialog -> viewModel.clearBackupResult())
                    .show();
            return;
        }
        String message;
        if (state.running) {
            message = getString(R.string.backup_running);
        } else if (state.count < 0) {
            message = getString(state.importing ? R.string.backup_import_error : R.string.backup_export_error);
        } else if (state.importing) {
            message = state.count == 0 ? getString(R.string.backup_no_new)
                    : getString(R.string.backup_imported, state.count);
        } else {
            message = getString(R.string.backup_exported, state.count);
        }
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                .setAnchorView(binding.fabAdd)
                .show();
        if (!state.running) viewModel.clearBackupResult();
    }
}
