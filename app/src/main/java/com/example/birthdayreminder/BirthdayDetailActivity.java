package com.example.birthdayreminder;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.birthdayreminder.data.Birthday;
import com.example.birthdayreminder.notification.BirthdayAlarmScheduler;
import com.example.birthdayreminder.util.BirthdayUtils;
import com.example.birthdayreminder.databinding.ActivityBirthdayDetailBinding;
import com.example.birthdayreminder.viewmodel.BirthdayViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.Executors;

public class BirthdayDetailActivity extends AppCompatActivity {

    private ActivityBirthdayDetailBinding binding;
    private BirthdayViewModel viewModel;
    private int birthdayId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBirthdayDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        birthdayId = getIntent().getIntExtra("birthday_id", -1);
        if (birthdayId == -1) {
            finish();
            return;
        }

        setupViewModel();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBirthdayData();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(BirthdayViewModel.class);
    }

    private void loadBirthdayData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Birthday birthday = viewModel.getById(birthdayId);
            if (birthday != null) {
                runOnUiThread(() -> displayBirthday(birthday));
            } else {
                runOnUiThread(this::finish);
            }
        });
    }

    private void displayBirthday(Birthday birthday) {
        binding.tvName.setText(birthday.getName());
        binding.tvDate.setText(BirthdayUtils.formatDate(birthday.getDay(), birthday.getMonth(), birthday.getYear()));
        binding.tvInitials.setText(BirthdayUtils.getInitials(birthday.getName()));

        // Avatar color
        int color = BirthdayUtils.getAvatarColor(birthday.getName());
        GradientDrawable avatarDrawable = new GradientDrawable();
        avatarDrawable.setShape(GradientDrawable.OVAL);
        avatarDrawable.setColor(color);
        binding.avatarBackground.setBackground(avatarDrawable);

        // Age
        int currentAge = BirthdayUtils.calculateCurrentAge(birthday.getDay(), birthday.getMonth(), birthday.getYear());
        int nextAge = BirthdayUtils.calculateNextAge(birthday.getDay(), birthday.getMonth(), birthday.getYear());
        if (nextAge >= 0) {
            binding.tvAge.setText(String.valueOf(nextAge));
            binding.tvAgeLabel.setText(getString(R.string.age_format, nextAge));
        } else {
            binding.tvAge.setText("?");
            binding.tvAgeLabel.setText(getString(R.string.unknown_age));
        }

        // Days remaining
        long daysRemaining = BirthdayUtils.daysUntilNextBirthday(birthday.getDay(), birthday.getMonth());
        if (daysRemaining == 0) {
            binding.tvDaysCount.setText("🎂");
            binding.tvDaysLabel.setText(getString(R.string.today_birthday));
        } else if (daysRemaining == 1) {
            binding.tvDaysCount.setText("🎉");
            binding.tvDaysLabel.setText(getString(R.string.tomorrow_birthday));
        } else {
            binding.tvDaysCount.setText(String.valueOf(daysRemaining));
            binding.tvDaysLabel.setText(getString(R.string.days_remaining));
        }

        // Notes
        if (birthday.getNotes() != null && !birthday.getNotes().isEmpty()) {
            binding.notesCard.setVisibility(View.VISIBLE);
            binding.tvNotes.setText(birthday.getNotes());
        } else {
            binding.notesCard.setVisibility(View.VISIBLE);
            binding.tvNotes.setText(getString(R.string.no_notes));
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditBirthdayActivity.class);
            intent.putExtra("birthday_id", birthdayId);
            startActivity(intent);
        });

        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void showDeleteConfirmation() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Birthday birthday = viewModel.getById(birthdayId);
            if (birthday != null) {
                runOnUiThread(() -> {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.delete_confirm_title)
                            .setMessage(getString(R.string.delete_confirm_message, birthday.getName()))
                            .setPositiveButton(R.string.delete, (dialog, which) -> {
                                BirthdayAlarmScheduler.cancelAlarm(this, birthdayId);
                                viewModel.deleteById(birthdayId);
                                finish();
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                });
            }
        });
    }
}
