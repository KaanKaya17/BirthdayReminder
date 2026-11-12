package com.example.birthdayreminder;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.birthdayreminder.data.ReminderSlot;
import com.example.birthdayreminder.databinding.DialogReminderSlotBinding;
import com.example.birthdayreminder.util.BirthdayUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalTime;

public class ReminderEditorDialog extends DialogFragment {
    public static final String RESULT = "reminder_slot";
    private static final String INDEX = "index";
    private static final String DAYS = "days";
    private static final String TIME = "time";

    public static ReminderEditorDialog create(int index, ReminderSlot slot) {
        ReminderEditorDialog dialog = new ReminderEditorDialog();
        dialog.setArguments(values(index, slot.daysBefore, slot.time));
        return dialog;
    }

    public static int index(Bundle result) {
        return result.getInt(INDEX);
    }

    public static ReminderSlot slot(Bundle result) {
        return new ReminderSlot(result.getInt(DAYS), LocalTime.parse(result.getString(TIME)));
    }

    private static Bundle values(int index, int days, LocalTime time) {
        Bundle values = new Bundle();
        values.putInt(INDEX, index);
        values.putInt(DAYS, days);
        values.putString(TIME, time.toString());
        return values;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = requireArguments();
        ReminderSlot initial = slot(args);
        int index = index(args);
        DialogReminderSlotBinding binding = DialogReminderSlotBinding.inflate(getLayoutInflater());
        String[] days = ReminderSlot.DAY_OFFSETS.stream()
                .map(offset -> getString(BirthdayUtils.reminderDayLabel(offset)))
                .toArray(String[]::new);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, days);
        binding.reminderDay.setAdapter(adapter);
        binding.reminderDay.setSelection(ReminderSlot.DAY_OFFSETS.indexOf(initial.daysBefore));
        binding.reminderClock.setIs24HourView(true);
        binding.reminderClock.setHour(initial.time.getHour());
        binding.reminderClock.setMinute(initial.time.getMinute());

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(index < 0 ? R.string.add_reminder_slot : R.string.edit_reminder_slot)
                .setView(binding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    binding.reminderClock.clearFocus();
                    int offset = ReminderSlot.DAY_OFFSETS.get(binding.reminderDay.getSelectedItemPosition());
                    LocalTime time = LocalTime.of(binding.reminderClock.getHour(), binding.reminderClock.getMinute());
                    getParentFragmentManager().setFragmentResult(RESULT, values(index, offset, time));
                })
                .create();
    }
}
