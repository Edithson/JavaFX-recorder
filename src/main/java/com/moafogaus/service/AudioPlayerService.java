package com.moafogaus.service;

import com.moafogaus.model.AudioRecording;
import com.moafogaus.model.PlaybackState;
import javafx.application.Platform;
import javafx.beans.property.*;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

public class AudioPlayerService {

    private final ObjectProperty<PlaybackState> state = new SimpleObjectProperty<>(PlaybackState.STOPPED);
    private final ObjectProperty<AudioRecording> currentRecording = new SimpleObjectProperty<>();
    private final DoubleProperty currentTimeSeconds = new SimpleDoubleProperty(0);
    private final DoubleProperty totalDurationSeconds = new SimpleDoubleProperty(0);
    private final DoubleProperty volume = new SimpleDoubleProperty(0.9);

    private SourceDataLine line;
    private Thread playerThread;
    private volatile boolean isPaused = false;
    private volatile boolean isStopped = true;

    public AudioPlayerService() {
    }

    public ObjectProperty<PlaybackState> stateProperty() {
        return state;
    }

    public PlaybackState getState() {
        return state.get();
    }

    public ObjectProperty<AudioRecording> currentRecordingProperty() {
        return currentRecording;
    }

    public AudioRecording getCurrentRecording() {
        return currentRecording.get();
    }

    public double getCurrentTimeSeconds() {
        return currentTimeSeconds.get();
    }

    public DoubleProperty currentTimeSecondsProperty() {
        return currentTimeSeconds;
    }

    public double getTotalDurationSeconds() {
        return totalDurationSeconds.get();
    }

    public DoubleProperty totalDurationSecondsProperty() {
        return totalDurationSeconds;
    }

    public DoubleProperty volumeProperty() {
        return volume;
    }

    public double getVolume() {
        return volume.get();
    }

    public void setVolume(double volume) {
        this.volume.set(Math.max(0.0, Math.min(1.0, volume)));
    }

    public boolean isPlaying(AudioRecording recording) {
        return recording != null && currentRecording.get() == recording && state.get() == PlaybackState.PLAYING;
    }

    public boolean isPaused(AudioRecording recording) {
        return recording != null && currentRecording.get() == recording && state.get() == PlaybackState.PAUSED;
    }

    public synchronized void togglePlayPause(AudioRecording recording) {
        if (recording == null) return;

        if (currentRecording.get() == recording) {
            if (state.get() == PlaybackState.PLAYING) {
                pause();
            } else if (state.get() == PlaybackState.PAUSED) {
                play();
            } else {
                loadAndPlay(recording);
            }
        } else {
            loadAndPlay(recording);
        }
    }

    public synchronized void loadAndPlay(AudioRecording recording) {
        stop();
        if (recording == null || recording.getFile() == null) {
            return;
        }

        File absFile = recording.getFile().getAbsoluteFile();
        if (!absFile.exists()) {
            return;
        }

        currentRecording.set(recording);
        totalDurationSeconds.set(recording.getDurationSeconds());

        isStopped = false;
        isPaused = false;
        state.set(PlaybackState.PLAYING);

        playerThread = new Thread(() -> streamAudioLoop(absFile), "AudioPlayerStreamThread");
        playerThread.setDaemon(true);
        playerThread.start();
    }

    public synchronized void play() {
        if (state.get() == PlaybackState.PAUSED) {
            isPaused = false;
            state.set(PlaybackState.PLAYING);
        } else if (currentRecording.get() != null) {
            loadAndPlay(currentRecording.get());
        }
    }

    public synchronized void pause() {
        if (state.get() == PlaybackState.PLAYING) {
            isPaused = true;
            state.set(PlaybackState.PAUSED);
        }
    }

    public synchronized void stop() {
        isStopped = true;
        isPaused = false;
        if (line != null) {
            try {
                line.stop();
                line.flush();
                line.close();
            } catch (Exception ignored) {
            }
            line = null;
        }
        Platform.runLater(() -> {
            state.set(PlaybackState.STOPPED);
            currentTimeSeconds.set(0);
        });
    }

    private void streamAudioLoop(File file) {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
             AudioInputStream rawAis = AudioSystem.getAudioInputStream(bis)) {

            AudioFormat baseFormat = rawAis.getFormat();
            AudioFormat format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );

            AudioInputStream ais = AudioSystem.getAudioInputStream(format, rawAis);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            line = openCompatibleSourceDataLine(info, format);
            if (line == null) {
                System.err.println("No compatible output line found!");
                stop();
                return;
            }

            line.open(format, 8192);
            line.start();

            byte[] buffer = new byte[4096];
            long totalBytesRead = 0;
            double bytesPerSec = format.getSampleRate() * format.getFrameSize();

            while (!isStopped) {
                if (isPaused) {
                    try {
                        Thread.sleep(60);
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }

                int bytesRead = ais.read(buffer, 0, buffer.length);
                if (bytesRead <= 0) {
                    break; // End of audio stream
                }

                applyVolume(buffer, bytesRead, volume.get());

                line.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                double sec = totalBytesRead / bytesPerSec;
                Platform.runLater(() -> currentTimeSeconds.set(sec));
            }

            line.drain();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (line != null) {
                try {
                    line.stop();
                    line.close();
                } catch (Exception ignored) {
                }
                line = null;
            }
            Platform.runLater(() -> {
                state.set(PlaybackState.STOPPED);
                currentTimeSeconds.set(0);
            });
        }
    }

    private SourceDataLine openCompatibleSourceDataLine(DataLine.Info info, AudioFormat format) {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mInfo : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(mInfo);
                if (mixer.isLineSupported(info)) {
                    return (SourceDataLine) mixer.getLine(info);
                }
            } catch (Exception ignored) {
            }
        }
        try {
            return (SourceDataLine) AudioSystem.getLine(info);
        } catch (Exception ignored) {
        }
        return null;
    }

    private void applyVolume(byte[] buffer, int length, double vol) {
        if (vol >= 0.99) return;
        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            sample = (short) (sample * vol);
            buffer[i] = (byte) (sample & 0xFF);
            buffer[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
    }
}
