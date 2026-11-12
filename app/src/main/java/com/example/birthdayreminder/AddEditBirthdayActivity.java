package com.example.birthdayreminder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.birthdayreminder.data.Birthday;
import com.example.birthdayreminder.data.BirthdayPreferences;
import com.example.birthdayreminder.data.ReminderSlot;
import com.example.birthdayreminder.databinding.ActivityAddEditBirthdayBinding;
import com.example.birthdayreminder.databinding.ItemReminderSlotBinding;
import com.example.birthdayreminder.notification.BirthdayAlarmScheduler;
import com.example.birthdayreminder.notification.NotificationHelper;
import com.example.birthdayreminder.util.BirthdayUtils;
import com.example.birthdayreminder.viewmodel.BirthdayViewModel;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddEditBirthdayActivity extends AppCompatActivity {
    private String[] months;

    private ActivityAddEditBirthdayBinding binding;
    private BirthdayViewModel viewModel;
    private final List<ReminderSlot> slots = new ArrayList<>();
    private BirthdayPreferences preferences;
    private int birthdayId;
    private boolean ready;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        binding = ActivityAddEditBirthdayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        adaptDateFields();
        viewModel = new ViewModelProvider(this).get(BirthdayViewModel.class);
        birthdayId = getIntent().getIntExtra("birthday_id", 0);

        months = Arrays.stream(Month.values())
                .map(month -> month.getDisplayName(TextStyle.FULL_STANDALONE, BirthdayUtils.language(this)))
                .toArray(String[]::new);
        binding.etMonth.setSimpleItems(months);
        binding.etMonth.setOnItemClickListener((parent, view, position, id) -> binding.dateError.setVisibility(View.GONE));
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> save());
        preferences = new BirthdayPreferences(this);
        binding.btnAddReminder.setOnClickListener(v -> editSlot(-1));
        getSupportFragmentManager().setFragmentResultListener(ReminderEditorDialog.RESULT, this,
                (key, result) -> saveSlot(ReminderEditorDialog.index(result), ReminderEditorDialog.slot(result)));
        binding.notificationStatus.setOnClickListener(v ->
                startActivity(new Intent(this, ReminderSettingsActivity.class)));
        binding.switchNotification.setOnCheckedChangeListener((button, checked) -> showNotificationFields());

        if (birthdayId != 0) binding.tvTitle.setText(R.string.edit_birthday);
        if (state != null && state.getBoolean("ready") && state.containsKey("slots")) {
            slots.addAll(ReminderSlot.decode(state.getString("slots")));
            ready = true;
        } else if (birthdayId != 0) {
            loadBirthday();
        } else {
            slots.add(new ReminderSlot(1, preferences.getDefaultTime()));
            ready = true;
        }
        renderSlots();
        showNotificationFields();
        updateSaveButton();

        viewModel.getSaveState().observe(this, status -> {
            updateSaveButton();
            if (status == BirthdayViewModel.SaveState.SAVED) {
                Toast.makeText(this, birthdayId == 0 ? R.string.birthday_saved : R.string.birthday_updated,
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else if (status == BirthdayViewModel.SaveState.FAILED) {
                Toast.makeText(this, R.string.save_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void adaptDateFields() {
        if (getResources().getConfiguration().fontScale < 1.4f
                && getResources().getConfiguration().screenWidthDp >= 340) return;
        binding.dateFields.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < binding.dateFields.getChildCount(); i++) {
            View child = binding.dateFields.getChildAt(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child.getLayoutParams();
            params.width = LinearLayout.LayoutParams.MATCH_PARENT;
            params.weight = 0;
            params.setMarginStart(0);
            child.setLayoutParams(params);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showNotificationFields();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("ready", ready);
        state.putString("slots", ReminderSlot.encode(slots));
        state.putInt("month", selectedMonth());
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        int month = state.getInt("month");
        if (month > 0) binding.etMonth.setText(months[month - 1], false);
    }

    private int selectedMonth() {
        return Arrays.asList(months).indexOf(binding.etMonth.getText().toString()) + 1;
    }

    private void loadBirthday() {
        LiveData<Birthday> record = viewModel.observeBirthday(birthdayId);
        record.observe(this, birthday -> {
            record.removeObservers(this);
            if (birthday == null) {
                finish();
                return;
            }
            binding.etName.setText(birthday.getName());
            binding.etDay.setText(Integer.toString(birthday.getDay()));
            binding.etMonth.setText(months[birthday.getMonth() - 1], false);
            if (birthday.getYear() != 0) binding.etYear.setText(Integer.toString(birthday.getYear()));
            binding.etNotes.setText(birthday.getNotes());
            slots.addAll(birthday.reminderSlots());
            slots.sort(ReminderSlot.ORDER);
            renderSlots();
            binding.switchNotification.setChecked(birthday.isNotifyEnabled());
            ready = true;
            showNotificationFields();
            updateSaveButton();
        });
    }

    private void editSlot(int index) {
        ReminderSlot slot = index < 0 ? new ReminderSlot(0, preferences.getDefaultTime()) : slots.get(index);
        ReminderEditorDialog.create(index, slot).show(getSupportFragmentManager(), ReminderEditorDialog.RESULT);
    }

    private void saveSlot(int index, ReminderSlot slot) {
        int existing = slots.indexOf(slot);
        if (existing >= 0 && existing != index) {
            binding.reminderError.setText(R.string.duplicate_reminder_slot);
            binding.reminderError.setVisibility(View.VISIBLE);
            return;
        }
        if (index < 0) slots.add(slot);
        else slots.set(index, slot);
        slots.sort(ReminderSlot.ORDER);
        renderSlots();
    }

    private void renderSlots() {
        binding.reminderError.setVisibility(View.GONE);
        binding.reminderSlots.removeAllViews();
        for (int i = 0; i < slots.size(); i++) {
            int index = i;
            ItemReminderSlotBinding row = ItemReminderSlotBinding.inflate(getLayoutInflater(), binding.reminderSlots, false);
            String label = BirthdayUtils.reminderSlotLabel(this, slots.get(index));
            row.editSlot.setText(label);
            row.editSlot.setOnClickListener(view -> editSlot(index));
            row.removeSlot.setContentDescription(getString(R.string.remove_reminder_slot, label));
            row.removeSlot.setOnClickListener(view -> {
                slots.remove(index);
                renderSlots();
            });
            binding.reminderSlots.addView(row.getRoot());
        }
    }

    private void showNotificationFields() {
        binding.notificationSettingsLayout.setVisibility(binding.switchNotification.isChecked() ? View.VISIBLE : View.GONE);
        boolean available = NotificationHelper.isAvailable(this);
        boolean exact = BirthdayAlarmScheduler.canScheduleExactly(this);
        binding.notificationStatus.setText(available ? R.string.approximate_notification : R.string.notifications_blocked);
        binding.notificationStatus.setVisibility(available && exact ? View.GONE : View.VISIBLE);
    }

    private void updateSaveButton() {
        BirthdayViewModel.SaveState state = viewModel.getSaveState().getValue();
        binding.btnSave.setEnabled(ready && (state == null || state == BirthdayViewModel.SaveState.FAILED));
    }

    private void save() {
        String name = binding.etName.getText().toString().trim();
        if (name.isEmpty()) {
            binding.nameInputLayout.setError(getString(R.string.name_required));
            binding.etName.requestFocus();
            return;
        }
        binding.nameInputLayout.setError(null);
        int day, year;
        int month = selectedMonth();
        String yearText = binding.etYear.getText().toString();
        try {
            day = Integer.parseInt(binding.etDay.getText().toString());
            year = yearText.isEmpty() ? 0 : Integer.parseInt(yearText);
            if (year == 0 && !yearText.isEmpty()) throw new DateTimeException("Invalid year");
            LocalDate.of(year == 0 ? 2000 : year, month, day);
        } catch (DateTimeException | NumberFormatException e) {
            binding.dateError.setText(R.string.invalid_birthday_date);
            binding.dateError.setVisibility(View.VISIBLE);
            return;
        }
        binding.dateError.setVisibility(View.GONE);
        boolean enabled = binding.switchNotification.isChecked();
        if (enabled && slots.isEmpty()) {
            binding.reminderError.setText(R.string.reminder_slot_required);
            binding.reminderError.setVisibility(View.VISIBLE);
            return;
        }

        String notes = binding.etNotes.getText().toString().trim();
        Birthday birthday = new Birthday(name, day, month, year, notes, null, enabled, System.currentTimeMillis());
        birthday.setId(birthdayId);
        birthday.setReminderSchedule(ReminderSlot.encode(slots));
        if (!slots.isEmpty()) {
            ReminderSlot first = slots.get(0);
            birthday.setNotifyDaysBefore(first.daysBefore);
            birthday.setNotifyHour(first.time.getHour());
            birthday.setNotifyMinute(first.time.getMinute());
        }
        viewModel.save(birthday);
    }
}
