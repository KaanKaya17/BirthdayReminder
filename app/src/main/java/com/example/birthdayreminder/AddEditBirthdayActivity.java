package com.example.birthdayreminder;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.birthdayreminder.data.Birthday;
import com.example.birthdayreminder.databinding.ActivityAddEditBirthdayBinding;
import com.example.birthdayreminder.notification.BirthdayAlarmScheduler;
import com.example.birthdayreminder.util.BirthdayUtils;
import com.example.birthdayreminder.viewmodel.BirthdayViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.Executors;

public class AddEditBirthdayActivity extends AppCompatActivity {

    private ActivityAddEditBirthdayBinding binding;
    private BirthdayViewModel viewModel;

    private int selectedDay = 0, selectedMonth = 0, selectedYear = 0;
    private int editBirthdayId = -1;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditBirthdayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViewModel();
        checkEditMode();
        setupListeners();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(BirthdayViewModel.class);
    }

    private void checkEditMode() {
        editBirthdayId = getIntent().getIntExtra("birthday_id", -1);
        if (editBirthdayId != -1) {
            isEditMode = true;
            binding.tvTitle.setText(R.string.edit_birthday);
            loadBirthdayData();
        }
    }

    private void loadBirthdayData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Birthday birthday = viewModel.getById(editBirthdayId);
            if (birthday != null) {
                runOnUiThread(() -> {
                    binding.etName.setText(birthday.getName());
                    selectedDay = birthday.getDay();
                    selectedMonth = birthday.getMonth();
                    selectedYear = birthday.getYear();
                    binding.etDate.setText(BirthdayUtils.formatDate(selectedDay, selectedMonth, selectedYear));
                    binding.etNotes.setText(birthday.getNotes());
                    binding.switchNotification.setChecked(birthday.isNotifyEnabled());
                    updateAvatar(birthday.getName());
                });
            }
        });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.etDate.setOnClickListener(v -> showDatePicker());

        binding.btnSave.setOnClickListener(v -> saveBirthday());

        // Live avatar update as user types name
        binding.etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAvatar(s.toString());
                binding.nameInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateAvatar(String name) {
        if (name == null || name.trim().isEmpty()) {
            binding.tvAvatarInitial.setText("?");
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(getColor(R.color.later_badge));
            binding.avatarBackground.setBackground(drawable);
        } else {
            binding.tvAvatarInitial.setText(BirthdayUtils.getInitials(name));
            int color = BirthdayUtils.getAvatarColor(name);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);
            binding.avatarBackground.setBackground(drawable);
        }
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_date))
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            LocalDate date = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDate();
            selectedDay = date.getDayOfMonth();
            selectedMonth = date.getMonthValue();
            selectedYear = date.getYear();
            binding.etDate.setText(BirthdayUtils.formatDate(selectedDay, selectedMonth, selectedYear));
            binding.dateInputLayout.setError(null);
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void saveBirthday() {
        String name = binding.etName.getText() != null ? binding.etName.getText().toString().trim() : "";
        String notes = binding.etNotes.getText() != null ? binding.etNotes.getText().toString().trim() : "";
        boolean notify = binding.switchNotification.isChecked();

        // Validation
        if (name.isEmpty()) {
            binding.nameInputLayout.setError(getString(R.string.name_required));
            return;
        }
        if (selectedDay == 0) {
            binding.dateInputLayout.setError(getString(R.string.date_required));
            return;
        }

        if (isEditMode) {
            Executors.newSingleThreadExecutor().execute(() -> {
                Birthday birthday = viewModel.getById(editBirthdayId);
                if (birthday != null) {
                    birthday.setName(name);
                    birthday.setDay(selectedDay);
                    birthday.setMonth(selectedMonth);
                    birthday.setYear(selectedYear);
                    birthday.setNotes(notes);
                    birthday.setNotifyEnabled(notify);
                    viewModel.update(birthday);

                    // Schedule or cancel alarm
                    if (notify) {
                        BirthdayAlarmScheduler.scheduleAlarm(this, birthday);
                    } else {
                        BirthdayAlarmScheduler.cancelAlarm(this, birthday.getId());
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.birthday_updated, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }
            });
        } else {
            Birthday birthday = new Birthday(name, selectedDay, selectedMonth, selectedYear,
                    notes, null, notify, System.currentTimeMillis());

            viewModel.insert(birthday, id -> {
                birthday.setId(id);
                if (notify) {
                    BirthdayAlarmScheduler.scheduleAlarm(this, birthday);
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.birthday_saved, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            });
        }
    }
}
