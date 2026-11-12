package com.example.birthdayreminder;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.birthdayreminder.data.Birthday;
import com.example.birthdayreminder.data.ReminderSlot;
import com.example.birthdayreminder.databinding.ActivityBirthdayDetailBinding;
import com.example.birthdayreminder.notification.BirthdayAlarmScheduler;
import com.example.birthdayreminder.notification.NotificationHelper;
import com.example.birthdayreminder.util.BirthdayUtils;
import com.example.birthdayreminder.viewmodel.BirthdayViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

public class BirthdayDetailActivity extends AppCompatActivity {
    private ActivityBirthdayDetailBinding binding;
    private BirthdayViewModel viewModel;
    private Birthday birthday;
    private int birthdayId;
    private boolean composePending;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        binding = ActivityBirthdayDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        birthdayId = getIntent().getIntExtra("birthday_id", 0);
        if (birthdayId == 0) {
            finish();
            return;
        }
        composePending = state == null ? NotificationHelper.ACTION_COMPOSE.equals(getIntent().getAction())
                : state.getBoolean("compose_pending");
        viewModel = new ViewModelProvider(this).get(BirthdayViewModel.class);
        viewModel.refreshAlarmsOnLaunch();
        setupActions();
        viewModel.observeBirthday(birthdayId).observe(this, record -> {
            if (record == null) {
                finish();
                return;
            }
            birthday = record;
            showBirthday();
            if (composePending) {
                composePending = false;
                composeMessage();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (birthday != null) showBirthday();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("compose_pending", composePending);
        super.onSaveInstanceState(state);
    }

    private void setupActions() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnEdit.setEnabled(false);
        binding.btnArchive.setEnabled(false);
        binding.btnCelebrate.setVisibility(View.GONE);
        binding.btnMessage.setEnabled(false);
        binding.btnEdit.setOnClickListener(v -> startActivity(new Intent(this, AddEditBirthdayActivity.class)
                .putExtra("birthday_id", birthdayId)));
        binding.btnArchive.setOnClickListener(v -> {
            if (birthday.isArchived()) {
                changeArchiveState(false);
            } else {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.archive_confirm_title)
                        .setMessage(getString(R.string.archive_confirm_message, birthday.getName()))
                        .setPositiveButton(R.string.archive, (dialog, which) -> changeArchiveState(true))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
        binding.btnCelebrate.setOnClickListener(v -> {
            binding.btnCelebrate.setEnabled(false);
            viewModel.setCelebrated(birthdayId, !isCelebrated(), success -> {
                if (isDestroyed() || isFinishing()) return;
                binding.btnCelebrate.setEnabled(true);
                if (!success) Toast.makeText(this, R.string.celebration_error, Toast.LENGTH_SHORT).show();
            });
        });
        binding.btnMessage.setOnClickListener(v -> composeMessage());
        binding.btnReminderSettings.setOnClickListener(v ->
                startActivity(new Intent(this, ReminderSettingsActivity.class)));
    }

    private void showBirthday() {
        binding.tvName.setText(birthday.getName());
        binding.tvDate.setText(BirthdayUtils.formatDate(birthday.getDay(), birthday.getMonth(), birthday.getYear()));
        binding.tvInitials.setText(BirthdayUtils.getInitials(birthday.getName()));
        GradientDrawable avatar = new GradientDrawable();
        avatar.setShape(GradientDrawable.OVAL);
        avatar.setColor(BirthdayUtils.getAvatarColor(birthday.getName()));
        binding.tvInitials.setBackground(avatar);

        long days = BirthdayUtils.daysUntilNextBirthday(birthday.getDay(), birthday.getMonth());
        if (isCelebrated()) {
            binding.tvBirthdayStatus.setText(getString(R.string.birthday_celebrated_year, birthday.getCelebratedYear()));
        } else if (days == 0) {
            binding.tvBirthdayStatus.setText(R.string.today_birthday);
        } else if (days == 1) {
            binding.tvBirthdayStatus.setText(R.string.tomorrow_birthday);
        } else {
            binding.tvBirthdayStatus.setText(getString(R.string.birthday_in_days, days));
        }
        String zodiac = getString(BirthdayUtils.getZodiacStringId(birthday.getDay(), birthday.getMonth()));
        int age = BirthdayUtils.calculateNextAge(birthday.getDay(), birthday.getMonth(), birthday.getYear());
        binding.tvAgeZodiac.setVisibility(View.VISIBLE);
        binding.tvAgeZodiac.setText(age < 0 ? zodiac : getString(R.string.detail_age_zodiac,
                getString(days == 0 ? R.string.age_today : R.string.age_format, age), zodiac));
        binding.tvNotes.setText(birthday.getNotes() == null || birthday.getNotes().isBlank()
                ? getString(R.string.no_notes) : birthday.getNotes());

        binding.btnEdit.setEnabled(true);
        binding.btnArchive.setEnabled(true);
        binding.btnArchive.setImageResource(birthday.isArchived() ? R.drawable.ic_restore : R.drawable.ic_archive);
        CharSequence archiveAction = getText(birthday.isArchived() ? R.string.restore_birthday : R.string.archive);
        binding.btnArchive.setContentDescription(archiveAction);
        binding.btnArchive.setTooltipText(archiveAction);
        binding.btnCelebrate.setVisibility(birthday.isArchived() ? View.GONE : View.VISIBLE);
        binding.btnCelebrate.setText(isCelebrated() ? R.string.undo_celebrated : R.string.mark_celebrated);
        binding.btnMessage.setEnabled(birthday.getName() != null && !birthday.getName().isBlank());
        showReminder();
    }

    private boolean isCelebrated() {
        return birthday.getCelebratedYear() == BirthdayUtils.nextBirthday(
                birthday.getDay(), birthday.getMonth(), LocalDate.now()).getYear();
    }

    private void showReminder() {
        ZonedDateTime now = ZonedDateTime.now();
        BirthdayAlarmScheduler.Reminder reminder = BirthdayAlarmScheduler.nextReminder(birthday, now);
        ZonedDateTime snooze = BirthdayAlarmScheduler.pendingSnooze(birthday, now);
        binding.tvSnoozedReminder.setVisibility(snooze == null ? View.GONE : View.VISIBLE);
        if (snooze != null) {
            binding.tvSnoozedReminder.setText(getString(R.string.snoozed_reminder_at, formatReminder(snooze)));
        }
        binding.tvReminderPlan.setVisibility(reminder == null ? View.GONE : View.VISIBLE);
        binding.tvReminderStatus.setVisibility(View.GONE);
        binding.btnReminderSettings.setVisibility(View.GONE);
        if (reminder == null) {
            binding.tvReminder.setText(birthday.isArchived() ? R.string.reminder_archived : R.string.reminder_off);
            return;
        }
        binding.tvReminder.setText(formatReminder(reminder.time));
        binding.tvReminderPlan.setText(birthday.reminderSlots().stream().sorted(ReminderSlot.ORDER)
                .map(slot -> BirthdayUtils.reminderSlotLabel(this, slot))
                .collect(Collectors.joining("\n")));
        boolean enabled = NotificationHelper.isAvailable(this);
        if (!enabled || !BirthdayAlarmScheduler.canScheduleExactly(this)) {
            binding.tvReminderStatus.setText(enabled ? R.string.alarm_approximate : R.string.notifications_disabled);
            binding.tvReminderStatus.setVisibility(View.VISIBLE);
            binding.btnReminderSettings.setVisibility(View.VISIBLE);
        }
    }

    private String formatReminder(ZonedDateTime time) {
        LocalDate date = time.toLocalDate();
        return getString(R.string.next_reminder_at,
                BirthdayUtils.formatDate(date.getDayOfMonth(), date.getMonthValue(), date.getYear()),
                time.toLocalTime().withSecond(0).withNano(0));
    }

    private void composeMessage() {
        if (!binding.btnMessage.isEnabled()) return;
        Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, getString(R.string.birthday_greeting, birthday.getName()));
        startActivity(Intent.createChooser(share, getText(R.string.compose_chooser)));
    }

    private void changeArchiveState(boolean archived) {
        binding.btnArchive.setEnabled(false);
        viewModel.setArchived(birthdayId, archived, success -> {
            if (isDestroyed() || isFinishing()) return;
            if (success) {
                Toast.makeText(this, archived ? R.string.birthday_archived : R.string.birthday_restored,
                        Toast.LENGTH_SHORT).show();
                finish();
            } else {
                binding.btnArchive.setEnabled(true);
                Toast.makeText(this, R.string.archive_error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
