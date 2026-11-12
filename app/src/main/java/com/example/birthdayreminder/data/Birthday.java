package com.example.birthdayreminder.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "birthdays")
public class Birthday {

    public static final int ALLOWED_REMINDER_MASK = (1 << 0) | (1 << 1) | (1 << 2) | (1 << 7);

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private int day;
    private int month; // 1-12
    private int year;  // birth year (0 if unknown)
    private String notes;
    private String photoUri;
    private boolean notifyEnabled;
    private int notifyDaysBefore;  // 0=same day, 1=1 day before, 2=2 days before, 7=1 week before
    private int notifyHour;        // 0-23
    private int notifyMinute;      // 0-59
    private long createdAt;
    @ColumnInfo(defaultValue = "0")
    private boolean archived;
    @ColumnInfo(defaultValue = "0")
    private int extraReminderDays;
    @ColumnInfo(defaultValue = "0")
    private int celebratedYear;
    private String reminderSchedule;
    @ColumnInfo(defaultValue = "0")
    private long snoozedUntil;
    @ColumnInfo(defaultValue = "0")
    private int snoozedYear;
    @ColumnInfo(defaultValue = "0")
    private long lastRegularDelivery;

    public Birthday(String name, int day, int month, int year, String notes,
                    String photoUri, boolean notifyEnabled, long createdAt) {
        this.name = name;
        this.day = day;
        this.month = month;
        this.year = year;
        this.notes = notes;
        this.photoUri = photoUri;
        this.notifyEnabled = notifyEnabled;
        this.notifyDaysBefore = 1;  // default: 1 day before
        this.notifyHour = 9;        // default: 09:00
        this.notifyMinute = 0;
        this.createdAt = createdAt;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public int getDay() { return day; }
    public int getMonth() { return month; }
    public int getYear() { return year; }
    public String getNotes() { return notes; }
    public String getPhotoUri() { return photoUri; }
    public boolean isNotifyEnabled() { return notifyEnabled; }
    public int getNotifyDaysBefore() { return notifyDaysBefore; }
    public int getNotifyHour() { return notifyHour; }
    public int getNotifyMinute() { return notifyMinute; }
    public long getCreatedAt() { return createdAt; }
    public boolean isArchived() { return archived; }
    public int getExtraReminderDays() { return extraReminderDays; }
    public int getCelebratedYear() { return celebratedYear; }
    public String getReminderSchedule() { return reminderSchedule; }
    public long getSnoozedUntil() { return snoozedUntil; }
    public int getSnoozedYear() { return snoozedYear; }
    public long getLastRegularDelivery() { return lastRegularDelivery; }

    public List<ReminderSlot> reminderSlots() {
        if (reminderSchedule != null) return ReminderSlot.decode(reminderSchedule);
        List<ReminderSlot> slots = new ArrayList<>();
        LocalTime time = LocalTime.of(notifyHour, notifyMinute);
        for (int offset : ReminderSlot.DAY_OFFSETS) {
            if ((reminderMask() & (1 << offset)) != 0) slots.add(new ReminderSlot(offset, time));
        }
        return slots;
    }

    public int reminderMask() { return (1 << notifyDaysBefore) | extraReminderDays; }

    public void changeReminderDays(int mask) {
        if (mask == 0 || (mask & ~ALLOWED_REMINDER_MASK) != 0) {
            throw new IllegalArgumentException("Invalid reminder days");
        }
        if ((mask & (1 << notifyDaysBefore)) == 0) {
            notifyDaysBefore = Integer.numberOfTrailingZeros(mask);
        }
        extraReminderDays = mask & ~(1 << notifyDaysBefore);
    }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDay(int day) { this.day = day; }
    public void setMonth(int month) { this.month = month; }
    public void setYear(int year) { this.year = year; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }
    public void setNotifyEnabled(boolean notifyEnabled) { this.notifyEnabled = notifyEnabled; }
    public void setNotifyDaysBefore(int notifyDaysBefore) { this.notifyDaysBefore = notifyDaysBefore; }
    public void setNotifyHour(int notifyHour) { this.notifyHour = notifyHour; }
    public void setNotifyMinute(int notifyMinute) { this.notifyMinute = notifyMinute; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public void setExtraReminderDays(int extraReminderDays) { this.extraReminderDays = extraReminderDays; }
    public void setCelebratedYear(int celebratedYear) { this.celebratedYear = celebratedYear; }
    public void setReminderSchedule(String reminderSchedule) { this.reminderSchedule = reminderSchedule; }
    public void setSnoozedUntil(long snoozedUntil) { this.snoozedUntil = snoozedUntil; }
    public void setSnoozedYear(int snoozedYear) { this.snoozedYear = snoozedYear; }
    public void setLastRegularDelivery(long lastRegularDelivery) { this.lastRegularDelivery = lastRegularDelivery; }
}
