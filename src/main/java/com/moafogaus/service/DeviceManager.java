package com.moafogaus.service;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

public class DeviceManager {

    public List<Mixer.Info> getAvailableInputDevices() {
        List<Mixer.Info> inputDevices = new ArrayList<>();
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();

        AudioFormat defaultFormat = new AudioFormat(44100.0f, 16, 2, true, false);
        DataLine.Info targetLineInfo = new DataLine.Info(TargetDataLine.class, defaultFormat);

        for (Mixer.Info mixerInfo : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(targetLineInfo) || hasTargetDataLines(mixer)) {
                    inputDevices.add(mixerInfo);
                }
            } catch (Exception ignored) {
            }
        }
        return inputDevices;
    }

    private boolean hasTargetDataLines(Mixer mixer) {
        Line.Info[] targetLines = mixer.getTargetLineInfo();
        for (Line.Info lineInfo : targetLines) {
            if (TargetDataLine.class.isAssignableFrom(lineInfo.getLineClass())) {
                return true;
            }
        }
        return false;
    }
}
