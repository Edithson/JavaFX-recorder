package com.moafogaus.service;

import com.moafogaus.model.AudioRecording;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.awt.Desktop;
import java.io.File;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RecordingManager {

    private File storageDir;
    private final ObservableList<AudioRecording> recordings = FXCollections.observableArrayList();

    public RecordingManager() {
        this.storageDir = new File(System.getProperty("user.home"), "VocalisRecordings");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        loadExistingRecordings();
    }

    public ObservableList<AudioRecording> getRecordings() {
        return recordings;
    }

    public File getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(File newDir) {
        if (newDir != null && newDir.exists() && newDir.isDirectory()) {
            this.storageDir = newDir;
            loadExistingRecordings();
        }
    }

    public File createNewRecordingFile() {
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "Enregistrement_" + timestamp + ".wav";
        return new File(storageDir, filename);
    }

    public void loadExistingRecordings() {
        recordings.clear();
        File[] files = storageDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));
        if (files == null) return;

        for (File file : files) {
            double durationSec = getWavDurationFast(file);
            LocalDateTime date = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(file.lastModified()),
                    java.time.ZoneId.systemDefault()
            );
            AudioRecording rec = new AudioRecording(file, file.getName(), durationSec, file.length(), date);
            recordings.add(rec);
        }

        // Sort by date descending
        recordings.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));
    }

    public void addRecording(AudioRecording recording) {
        recordings.add(0, recording);
    }

    public boolean deleteRecording(AudioRecording recording) {
        if (recording == null || recording.getFile() == null) return false;

        boolean deleted = recording.getFile().delete();
        if (deleted || !recording.getFile().exists()) {
            recordings.remove(recording);
            return true;
        }
        return false;
    }

    public void openStorageFolder() {
        try {
            if (Desktop.isDesktopSupported() && storageDir.exists()) {
                Desktop.getDesktop().open(storageDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fast O(1) duration extraction from WAV header without opening stream pipelines.
     */
    private double getWavDurationFast(File file) {
        long len = file.length();
        if (len <= 44) return 0.0;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // Byte rate is located at offset 28 in standard WAV RIFF header (4 bytes, little-endian)
            raf.seek(28);
            int byte0 = raf.readUnsignedByte();
            int byte1 = raf.readUnsignedByte();
            int byte2 = raf.readUnsignedByte();
            int byte3 = raf.readUnsignedByte();
            int byteRate = byte0 | (byte1 << 8) | (byte2 << 16) | (byte3 << 24);

            if (byteRate > 0) {
                return (double) (len - 44) / byteRate;
            }
        } catch (Exception ignored) {
        }

        // Default fallback for 44.1kHz 16-bit stereo = 176400 bytes/sec
        return (double) (len - 44) / 176400.0;
    }
}
