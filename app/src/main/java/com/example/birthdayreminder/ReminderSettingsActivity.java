package com.example.birthdayreminder;

import android.Manifest;
import android.app.LocaleManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.birthdayreminder.data.BirthdayPreferences;
import com.example.birthdayreminder.data.BirthdayRepository;
import com.example.birthdayreminder.databinding.ActivityReminderSettingsBinding;
import com.example.birthdayreminder.notification.BirthdayAlarmScheduler;
import com.example.birthdayreminder.notification.NotificationHelper;
import com.example.birthdayreminder.widget.BirthdayWidget;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.LocalTime;

public class ReminderSettingsActivity extends AppCompatActivity {
    private static final String TIME_PICKER = "default_reminder_time";
    private static final String[] LANGUAGE_TAGS = {"", "en", "tr"};
    private ActivityReminderSettingsBinding binding;
    private BirthdayPreferences preferences;
    private Boolean hadExactPermission;

    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> refresh());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        binding = ActivityReminderSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferences = new BirthdayPreferences(this);
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnLanguage.setOnClickListener(v -> chooseLanguage());
        binding.btnDefaultTime.setOnClickListener(v -> showTimePicker());
        binding.btnPermission.setOnClickListener(v -> permission.launch(Manifest.permission.POST_NOTIFICATIONS));
        binding.btnSystemSettings.setOnClickListener(v -> openSystemNotifications());
        binding.btnExact.setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.fromParts("package", getPackageName(), null))));
        binding.btnTest.setOnClickListener(v -> {
            if (NotificationHelper.isAvailable(this)) {
                NotificationHelper.showTest(this);
                Toast.makeText(this, R.string.test_notification_sent, Toast.LENGTH_SHORT).show();
            }
            refresh();
        });
        AppWidgetManager widgets = AppWidgetManager.getInstance(this);
        binding.widgetCard.setVisibility(widgets.isRequestPinAppWidgetSupported() ? View.VISIBLE : View.GONE);
        binding.btnPinWidget.setOnClickListener(v ->
                widgets.requestPinAppWidget(new ComponentName(this, BirthdayWidget.class), null, null));
        if (getSupportFragmentManager().findFragmentByTag(TIME_PICKER) instanceof MaterialTimePicker) {
            bindTimePicker((MaterialTimePicker) getSupportFragmentManager().findFragmentByTag(TIME_PICKER));
        }
    }

    private CharSequence[] languageChoices() {
        return new CharSequence[] {getText(R.string.system_language),
                getText(R.string.language_english), getText(R.string.language_turkish)};
    }

    private int languageSelection() {
        LocaleList locales = getSystemService(LocaleManager.class).getApplicationLocales();
        if (locales.isEmpty()) return 0;
        for (int i = 1; i < LANGUAGE_TAGS.length; i++) {
            if (LANGUAGE_TAGS[i].equals(locales.get(0).getLanguage())) return i;
        }
        return 0;
    }

    private void chooseLanguage() {
        int selection = languageSelection();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_language)
                .setSingleChoiceItems(languageChoices(), selection, (dialog, choice) -> {
                    dialog.dismiss();
                    if (choice != selection) {
                        binding.btnLanguage.setText(languageChoices()[choice]);
                        getSystemService(LocaleManager.class)
                                .setApplicationLocales(LocaleList.forLanguageTags(LANGUAGE_TAGS[choice]));
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openSystemNotifications() {
        Intent settings = new Intent(NotificationHelper.hasPermission(this)
                ? Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS : Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                .putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_ID);
        startActivity(settings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean notifications = NotificationHelper.isAvailable(this);
        boolean exact = BirthdayAlarmScheduler.canScheduleExactly(this);
        binding.btnLanguage.setText(languageChoices()[languageSelection()]);
        binding.btnDefaultTime.setText(getString(R.string.reminder_time_value, preferences.getDefaultTime()));
        binding.tvPermissionStatus.setText(notifications ? R.string.notifications_enabled : R.string.notifications_disabled);
        binding.btnPermission.setVisibility(NotificationHelper.hasPermission(this) ? View.GONE : View.VISIBLE);
        binding.btnTest.setEnabled(notifications);
        binding.tvExactStatus.setText(exact ? R.string.alarm_exact : R.string.alarm_approximate);
        binding.btnExact.setVisibility(exact ? View.GONE : View.VISIBLE);
        if (hadExactPermission != null && hadExactPermission != exact) {
            new BirthdayRepository(getApplication()).refreshAlarms(() -> {});
        }
        hadExactPermission = exact;
    }

    private void showTimePicker() {
        LocalTime current = preferences.getDefaultTime();
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(current.getHour())
                .setMinute(current.getMinute())
                .setTitleText(getString(R.string.default_reminder_time))
                .build();
        bindTimePicker(picker);
        picker.show(getSupportFragmentManager(), TIME_PICKER);
    }

    private void bindTimePicker(MaterialTimePicker picker) {
        picker.addOnPositiveButtonClickListener(v -> {
            preferences.setDefaultTime(LocalTime.of(picker.getHour(), picker.getMinute()));
            refresh();
        });
    }
}
