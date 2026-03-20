package com.example.birthdayreminder.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "birthdays")
public class Birthday {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private int day;
    private int month; // 1-12
    private int year;  // birth year (0 if unknown)
    private String notes;
    private String photoUri;
    private boolean notifyEnabled;
    private long createdAt;

    public Birthday(String name, int day, int month, int year, String notes,
                    String photoUri, boolean notifyEnabled, long createdAt) {
        this.name = name;
        this.day = day;
        this.month = month;
        this.year = year;
        this.notes = notes;
        this.photoUri = photoUri;
        this.notifyEnabled = notifyEnabled;
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
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDay(int day) { this.day = day; }
    public void setMonth(int month) { this.month = month; }
    public void setYear(int year) { this.year = year; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }
    public void setNotifyEnabled(boolean notifyEnabled) { this.notifyEnabled = notifyEnabled; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
