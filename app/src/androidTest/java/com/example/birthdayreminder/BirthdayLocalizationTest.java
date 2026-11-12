package com.example.birthdayreminder;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.birthdayreminder.util.BirthdayUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class BirthdayLocalizationTest {
    @Test
    public void englishUsesEnglishWithATurkishSystemFallback() {
        Context english = localizedContext("en-US,tr-TR");

        assertEquals("Birthday Reminder", english.getString(R.string.app_name));
        assertEquals("May 17, 2000", BirthdayUtils.formatDate(english, 17, 5, 2000));
        assertEquals("May 17", BirthdayUtils.formatDate(english, 17, 5, 0));
        assertEquals("2 days", BirthdayUtils.formatDaysRemaining(english, 2));
        assertEquals("In 1 hour", english.getString(R.string.snooze_one_hour));
        assertEquals("Upcoming birthdays", english.getString(R.string.widget_title));
    }

    @Test
    public void turkishUsesTurkishWithAnEnglishSystemFallback() {
        Context turkish = localizedContext("tr-TR,en-US");

        assertEquals("Doğum Günü Hatırlatıcı", turkish.getString(R.string.app_name));
        assertEquals("17 Mayıs 2000", BirthdayUtils.formatDate(turkish, 17, 5, 2000));
        assertEquals("17 Mayıs", BirthdayUtils.formatDate(turkish, 17, 5, 0));
        assertEquals("2 gün", BirthdayUtils.formatDaysRemaining(turkish, 2));
        assertEquals("1 saat sonra", turkish.getString(R.string.snooze_one_hour));
        assertEquals("Yaklaşan doğum günleri", turkish.getString(R.string.widget_title));
    }

    private Context localizedContext(String languages) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocales(LocaleList.forLanguageTags(languages));
        return context.createConfigurationContext(configuration);
    }
}
