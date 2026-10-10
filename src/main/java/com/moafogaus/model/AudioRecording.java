package com.moafogaus.model;

import javafx.beans.property.*;
import java.io.File;
import java.time.LocalDateTime;

public class AudioRecording {
    private final StringProperty name = new SimpleStringProperty();
    private final ObjectProperty<File> file = new SimpleObjectProperty<>();
    private final DoubleProperty durationSeconds = new SimpleDoubleProperty();
    private final LongProperty sizeBytes = new SimpleLongProperty();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();

    public AudioRecording(File file, double durationSeconds) {
        this.file.set(file);
        this.name.set(file != null ? file.getName() : "sans_titre.wav");
        this.durationSeconds.set(durationSeconds);
        this.sizeBytes.set(file != null && file.exists() ? file.length() : 0);
        this.createdAt.set(LocalDateTime.now());
    }

    public AudioRecording(File file, String name, double durationSeconds, long sizeBytes, LocalDateTime createdAt) {
        this.file.set(file);
        this.name.set(name);
        this.durationSeconds.set(durationSeconds);
        this.sizeBytes.set(sizeBytes);
        this.createdAt.set(createdAt != null ? createdAt : LocalDateTime.now());
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public File getFile() {
        return file.get();
    }

    public ObjectProperty<File> fileProperty() {
        return file;
    }

    public void setFile(File file) {
        this.file.set(file);
    }

    public double getDurationSeconds() {
        return durationSeconds.get();
    }

    public DoubleProperty durationSecondsProperty() {
        return durationSeconds;
    }

    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds.set(durationSeconds);
    }

    public long getSizeBytes() {
        return sizeBytes.get();
    }

    public LongProperty sizeBytesProperty() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes.set(sizeBytes);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt.get();
    }

    public ObjectProperty<LocalDateTime> createdAtProperty() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt.set(createdAt);
    }

    @Override
    public String toString() {
        return getName();
    }
}
