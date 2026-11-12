package com.example.birthdayreminder.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.example.birthdayreminder.BirthdayDetailActivity;
import com.example.birthdayreminder.MainActivity;
import com.example.birthdayreminder.R;
import com.example.birthdayreminder.data.Birthday;
import com.example.birthdayreminder.data.BirthdayDatabase;
import com.example.birthdayreminder.util.BirthdayUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BirthdayWidget extends AppWidgetProvider {
    private static final int[] ROWS = {R.id.widgetFirst, R.id.widgetSecond, R.id.widgetThird};

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        PendingResult pending = goAsync();
        refreshAsync(context, pending::finish);
    }

    public static void refreshAsync(Context context, Runnable onComplete) {
        Context app = context.getApplicationContext();
        BirthdayDatabase.getDatabase(app).getQueryExecutor().execute(() -> {
            try {
                refresh(app);
            } finally {
                onComplete.run();
            }
        });
    }

    public static void refresh(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, BirthdayWidget.class);
        if (manager.getAppWidgetIds(provider).length == 0) return;
        Context display = BirthdayUtils.applicationLanguageContext(context);

        LocalDate today = LocalDate.now();
        List<Birthday> upcoming = BirthdayDatabase.getDatabase(context).birthdayDao().getAllBirthdaysSync()
                .stream()
                .filter(birthday -> !birthday.isArchived())
                .sorted(Comparator.comparing(b -> BirthdayUtils.nextBirthday(b.getDay(), b.getMonth(), today)))
                .limit(ROWS.length)
                .collect(Collectors.toList());

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.birthday_widget);
        views.setTextViewText(R.id.widgetTitle, display.getText(R.string.widget_title));
        views.setTextViewText(R.id.widgetEmpty, display.getText(R.string.widget_empty));
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingActivity(context,
                new Intent(context, MainActivity.class), 0));
        views.setViewVisibility(R.id.widgetEmpty, upcoming.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < ROWS.length; i++) {
            int row = ROWS[i];
            views.setViewVisibility(row, i < upcoming.size() ? View.VISIBLE : View.GONE);
            if (i >= upcoming.size()) continue;

            Birthday birthday = upcoming.get(i);
            LocalDate occurrence = BirthdayUtils.nextBirthday(birthday.getDay(), birthday.getMonth(), today);
            String date = BirthdayUtils.formatDate(display, occurrence.getDayOfMonth(), occurrence.getMonthValue(), 0);
            views.setTextViewText(row, display.getString(R.string.widget_row, birthday.getName(), date));
            views.setOnClickPendingIntent(row, pendingActivity(context,
                    new Intent(context, BirthdayDetailActivity.class).putExtra("birthday_id", birthday.getId()),
                    birthday.getId()));
        }
        manager.updateAppWidget(provider, views);
    }

    private static PendingIntent pendingActivity(Context context, Intent intent, int id) {
        return PendingIntent.getActivity(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
