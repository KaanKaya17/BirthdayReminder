package com.example.birthdayreminder.data;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ReminderSlot {
    public static final List<Integer> DAY_OFFSETS = List.of(0, 1, 2, 7);
    public static final Comparator<ReminderSlot> ORDER = Comparator
            .comparingInt((ReminderSlot slot) -> slot.daysBefore).reversed()
            .thenComparing(slot -> slot.time);
    public final int daysBefore;
    public final LocalTime time;

    public ReminderSlot(int daysBefore, LocalTime time) {
        if (!DAY_OFFSETS.contains(daysBefore) || time.getSecond() != 0 || time.getNano() != 0) {
            throw new IllegalArgumentException("Invalid reminder day or time");
        }
        this.daysBefore = daysBefore;
        this.time = time;
    }

    public static String encode(List<ReminderSlot> slots) {
        return slots.stream().distinct()
                .sorted(ORDER)
                .map(slot -> slot.daysBefore + "/" + slot.time)
                .collect(Collectors.joining(","));
    }

    public static List<ReminderSlot> decode(String encoded) {
        List<ReminderSlot> slots = new ArrayList<>();
        if (encoded.isEmpty()) return slots;
        Set<ReminderSlot> seen = new HashSet<>();
        for (String entry : encoded.split(",", -1)) {
            String[] fields = entry.split("/", -1);
            if (fields.length != 2) throw new IllegalArgumentException("Invalid reminder slot");
            ReminderSlot slot = new ReminderSlot(Integer.parseInt(fields[0]), LocalTime.parse(fields[1]));
            if (!seen.add(slot)) throw new IllegalArgumentException("Duplicate reminder slot");
            slots.add(slot);
        }
        return slots;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ReminderSlot)) return false;
        ReminderSlot slot = (ReminderSlot) other;
        return daysBefore == slot.daysBefore && time.equals(slot.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(daysBefore, time);
    }
}
