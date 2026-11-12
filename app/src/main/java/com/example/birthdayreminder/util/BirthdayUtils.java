package com.example.birthdayreminder.util;

import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import android.text.format.DateFormat;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import com.example.birthdayreminder.R;
import com.example.birthdayreminder.data.ReminderSlot;

public class BirthdayUtils {

    private static final int[] AVATAR_COLORS = {
            0xFFA23B68, 0xFF753E91, 0xFF594296, 0xFF445299, 0xFF24648B,
            0xFF216975, 0xFF276E5C, 0xFF486D46, 0xFF8A572A, 0xFF9B493F
    };

    public static int reminderDayLabel(int daysBefore) {
        switch (daysBefore) {
            case 0: return R.string.notify_same_day;
            case 1: return R.string.notify_1_day_before;
            case 2: return R.string.notify_2_days_before;
            case 7: return R.string.notify_1_week_before;
            default: throw new IllegalArgumentException("Invalid reminder day");
        }
    }

    public static String reminderSlotLabel(Context context, ReminderSlot slot) {
        return context.getString(R.string.reminder_slot_label,
                context.getString(reminderDayLabel(slot.daysBefore)), slot.time);
    }

    /**
     * Calculate the age the person will be on their NEXT birthday.
     * If year == 0 (unknown), returns -1.
     */
    public static int calculateNextAge(int day, int month, int year) {
        if (year == 0) return -1;
        LocalDate today = LocalDate.now();
        return nextBirthday(day, month, today).getYear() - year;
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
        return ChronoUnit.DAYS.between(today, nextBirthday(day, month, today));
    }

    public static LocalDate nextBirthday(int day, int month, LocalDate from) {
        MonthDay birthday = MonthDay.of(month, day);
        LocalDate occurrence = birthday.atYear(from.getYear());
        return occurrence.isBefore(from) ? birthday.atYear(from.getYear() + 1) : occurrence;
    }

    public static Locale language(Context context) {
        String language = context.getString(R.string.language_tag);
        LocaleList locales = context.getResources().getConfiguration().getLocales();
        for (int i = 0; i < locales.size(); i++) {
            if (language.equals(locales.get(i).getLanguage())) return locales.get(i);
        }
        return Locale.forLanguageTag(language);
    }

    public static Context applicationLanguageContext(Context context) {
        LocaleManager manager = context.getSystemService(LocaleManager.class);
        LocaleList selected = manager.getApplicationLocales();
        LocaleList locales = selected.isEmpty() ? manager.getSystemLocales() : selected;
        if (locales.equals(context.getResources().getConfiguration().getLocales())) return context;

        Configuration override = new Configuration();
        override.setLocales(locales);
        return context.createConfigurationContext(override);
    }

    public static String formatDate(Context context, int day, int month, int year) {
        Locale language = language(context);
        String pattern = DateFormat.getBestDateTimePattern(language, year > 0 ? "yMMMMd" : "MMMMd");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, language);
        if (year > 0) {
            return LocalDate.of(year, month, day).format(formatter);
        }
        return MonthDay.of(month, day).format(formatter);
    }

    /**
     * Format remaining days into a friendly string.
     */
    public static String formatDaysRemaining(Context context, long days) {
        if (days == 0) return context.getString(R.string.birthday_today_badge);
        if (days == 1) return context.getString(R.string.birthday_tomorrow_badge);
        return context.getResources().getQuantityString(R.plurals.birthday_days_badge, (int) days, days);
    }

    /**
     * Get a color based on name hash for avatar background.
     * Returns one of 10 curated colors.
     */
    public static int getAvatarColor(String name) {
        return AVATAR_COLORS[Math.floorMod(name == null ? 0 : name.hashCode(), AVATAR_COLORS.length)];
    }

    /**
     * Get first letter(s) of a name for avatar display.
     */
    public static String getInitials(Context context, String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder().appendCodePoint(parts[0].codePointAt(0));
        if (parts.length >= 2) initials.appendCodePoint(parts[1].codePointAt(0));
        return initials.toString().toUpperCase(language(context));
    }

    /**
     * Get Zodiac sign string resource ID based on birth day and month.
     */
    public static int getZodiacStringId(int day, int month) {
        if ((month == 3 && day >= 21) || (month == 4 && day <= 19)) return R.string.zodiac_aries;
        if ((month == 4 && day >= 20) || (month == 5 && day <= 20)) return R.string.zodiac_taurus;
        if ((month == 5 && day >= 21) || (month == 6 && day <= 20)) return R.string.zodiac_gemini;
        if ((month == 6 && day >= 21) || (month == 7 && day <= 22)) return R.string.zodiac_cancer;
        if ((month == 7 && day >= 23) || (month == 8 && day <= 22)) return R.string.zodiac_leo;
        if ((month == 8 && day >= 23) || (month == 9 && day <= 22)) return R.string.zodiac_virgo;
        if ((month == 9 && day >= 23) || (month == 10 && day <= 22)) return R.string.zodiac_libra;
        if ((month == 10 && day >= 23) || (month == 11 && day <= 21)) return R.string.zodiac_scorpio;
        if ((month == 11 && day >= 22) || (month == 12 && day <= 21)) return R.string.zodiac_sagittarius;
        if ((month == 12 && day >= 22) || (month == 1 && day <= 19)) return R.string.zodiac_capricorn;
        if ((month == 1 && day >= 20) || (month == 2 && day <= 18)) return R.string.zodiac_aquarius;
        if ((month == 2 && day >= 19) || (month == 3 && day <= 20)) return R.string.zodiac_pisces;
        return R.string.unknown_age; // Fallback
    }
}
