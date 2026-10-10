package com.moafogaus.model;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;

public class AudioSettings {
    private Mixer.Info selectedMixer;
    private float sampleRate = 44100.0f;
    private int sampleSizeInBits = 16;
    private int channels = 2; // 1 = Mono, 2 = Stereo
    private boolean signed = true;
    private boolean bigEndian = false;

    public AudioSettings() {}

    public AudioSettings(Mixer.Info selectedMixer, float sampleRate, int channels) {
        this.selectedMixer = selectedMixer;
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    public AudioFormat getAudioFormat() {
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public Mixer.Info getSelectedMixer() {
        return selectedMixer;
    }

    public void setSelectedMixer(Mixer.Info selectedMixer) {
        this.selectedMixer = selectedMixer;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public int getSampleSizeInBits() {
        return sampleSizeInBits;
    }

    public void setSampleSizeInBits(int sampleSizeInBits) {
        this.sampleSizeInBits = sampleSizeInBits;
    }
}
