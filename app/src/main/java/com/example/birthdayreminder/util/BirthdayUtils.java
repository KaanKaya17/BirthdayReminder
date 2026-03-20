package com.example.birthdayreminder.util;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class BirthdayUtils {

    /**
     * Calculate the age the person will be on their NEXT birthday.
     * If year == 0 (unknown), returns -1.
     */
    public static int calculateNextAge(int day, int month, int year) {
        if (year == 0) return -1;
        LocalDate birthDate = LocalDate.of(year, month, day);
        LocalDate today = LocalDate.now();
        int age = Period.between(birthDate, today).getYears();
        // If birthday hasn't happened this year yet, next birthday age = age + 1
        // If birthday already passed this year, next birthday age = age + 1
        // (age is always "completed years")
        return age + 1;
    }

    /**
     * Calculate the current age.
     * If year == 0 (unknown), returns -1.
     */
    public static int calculateCurrentAge(int day, int month, int year) {
        if (year == 0) return -1;
        LocalDate birthDate = LocalDate.of(year, month, day);
        LocalDate today = LocalDate.now();
        return Period.between(birthDate, today).getYears();
    }

    /**
     * Days until the next birthday from today.
     */
    public static long daysUntilNextBirthday(int day, int month) {
        LocalDate today = LocalDate.now();
        LocalDate nextBirthday;
        try {
            nextBirthday = LocalDate.of(today.getYear(), month, day);
        } catch (Exception e) {
            // Handle Feb 29 — use Feb 28 in non-leap years
            nextBirthday = LocalDate.of(today.getYear(), month, Math.min(day, 28));
        }

        if (!nextBirthday.isAfter(today)) {
            // Birthday already passed this year, move to next year
            try {
                nextBirthday = LocalDate.of(today.getYear() + 1, month, day);
            } catch (Exception e) {
                nextBirthday = LocalDate.of(today.getYear() + 1, month, Math.min(day, 28));
            }
        }

        return ChronoUnit.DAYS.between(today, nextBirthday);
    }

    /**
     * Format date as "dd MMMM" (e.g., "15 Mart") or "dd MMMM yyyy" if year > 0.
     */
    public static String formatDate(int day, int month, int year) {
        if (year > 0) {
            LocalDate date = LocalDate.of(year, month, day);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault());
            return date.format(formatter);
        } else {
            LocalDate date = LocalDate.of(2000, month, day); // dummy year
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM", Locale.getDefault());
            return date.format(formatter);
        }
    }

    /**
     * Format remaining days into a friendly string.
     */
    public static String formatDaysRemaining(long days) {
        if (days == 0) return "Bugün! 🎂";
        if (days == 1) return "Yarın! 🎉";
        return days + " gün";
    }

    /**
     * Get a color based on name hash for avatar background.
     * Returns one of 10 curated colors.
     */
    public static int getAvatarColor(String name) {
        int[] colors = {
                0xFFE91E63, // Pink
                0xFF9C27B0, // Purple
                0xFF673AB7, // Deep Purple
                0xFF3F51B5, // Indigo
                0xFF2196F3, // Blue
                0xFF00BCD4, // Cyan
                0xFF009688, // Teal
                0xFF4CAF50, // Green
                0xFFFF9800, // Orange
                0xFFFF5722  // Deep Orange
        };
        int hash = Math.abs(name.hashCode());
        return colors[hash % colors.length];
    }

    /**
     * Get first letter(s) of a name for avatar display.
     */
    public static String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(Locale.getDefault());
        }
        return ("" + parts[0].charAt(0)).toUpperCase(Locale.getDefault());
    }
}
