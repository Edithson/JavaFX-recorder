package com.moafogaus.service;

import com.moafogaus.model.AudioRecording;
import com.moafogaus.model.PlaybackState;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.*;

import javax.sound.sampled.*;
import java.io.File;

public class AudioPlayerService {

    private final ObjectProperty<PlaybackState> state = new SimpleObjectProperty<>(PlaybackState.STOPPED);
    private final ObjectProperty<AudioRecording> currentRecording = new SimpleObjectProperty<>();
    private final DoubleProperty currentTimeSeconds = new SimpleDoubleProperty(0);
    private final DoubleProperty totalDurationSeconds = new SimpleDoubleProperty(0);
    private final DoubleProperty volume = new SimpleDoubleProperty(0.9);

    private Clip clip;
    private AnimationTimer timer;

    public AudioPlayerService() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (clip != null && clip.isOpen()) {
                    double current = clip.getMicrosecondPosition() / 1_000_000.0;
                    currentTimeSeconds.set(current);

                    if (!clip.isRunning() && state.get() == PlaybackState.PLAYING) {
                        if (clip.getMicrosecondPosition() >= clip.getMicrosecondLength()) {
                            stop();
                        }
                    }
                }
            }
        };

        volume.addListener((obs, oldVal, newVal) -> updateVolumeControl());
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
        closeClip();
        if (recording == null || recording.getFile() == null) {
            state.set(PlaybackState.STOPPED);
            currentRecording.set(null);
            return;
        }

        File absFile = recording.getFile().getAbsoluteFile();
        if (!absFile.exists()) {
            state.set(PlaybackState.STOPPED);
            currentRecording.set(null);
            return;
        }

        currentRecording.set(recording);
        totalDurationSeconds.set(recording.getDurationSeconds());

        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(absFile);
            clip = createCompatibleClip(absFile, ais);

            if (clip == null) {
                throw new IllegalStateException("Aucun périphérique de sortie audio compatible n'a été trouvé.");
            }

            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    if (clip != null && clip.getMicrosecondPosition() >= clip.getMicrosecondLength()) {
                        Platform.runLater(this::stop);
                    }
                }
            });

            updateVolumeControl();
            clip.start();
            timer.start();
            state.set(PlaybackState.PLAYING);
        } catch (Exception e) {
            e.printStackTrace();
            closeClip();
            state.set(PlaybackState.STOPPED);
            currentRecording.set(null);
        }
    }

    private Clip createCompatibleClip(File absFile, AudioInputStream ais) {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixers) {
            try {
                AudioInputStream freshStream = AudioSystem.getAudioInputStream(absFile);
                Clip c = AudioSystem.getClip(info);
                c.open(freshStream);
                return c;
            } catch (Exception ignored) {
            }
        }
        try {
            Clip c = AudioSystem.getClip();
            c.open(ais);
            return c;
        } catch (Exception ignored) {
        }
        return null;
    }

    public synchronized void play() {
        if (clip != null && clip.isOpen() && state.get() == PlaybackState.PAUSED) {
            clip.start();
            timer.start();
            state.set(PlaybackState.PLAYING);
        } else if (currentRecording.get() != null) {
            loadAndPlay(currentRecording.get());
        }
    }

    public synchronized void pause() {
        if (clip != null && clip.isOpen() && state.get() == PlaybackState.PLAYING) {
            clip.stop();
            timer.stop();
            state.set(PlaybackState.PAUSED);
        }
    }

    public synchronized void stop() {
        closeClip();
        state.set(PlaybackState.STOPPED);
        currentTimeSeconds.set(0);
    }

    private void closeClip() {
        if (timer != null) {
            timer.stop();
        }
        if (clip != null) {
            try {
                if (clip.isRunning()) {
                    clip.stop();
                }
                clip.close();
            } catch (Exception ignored) {
            }
            clip = null;
        }
    }

    private void updateVolumeControl() {
        if (clip != null && clip.isOpen() && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            try {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                double v = volume.get();
                if (v <= 0.0) {
                    gain.setValue(gain.getMinimum());
                } else {
                    float dB = (float) (Math.log10(v) * 20.0);
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
                }
            } catch (Exception ignored) {
            }
        }
    }
}
