package com.moafogaus.service;

import com.moafogaus.model.AudioSettings;
import com.moafogaus.model.RecordingState;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioRecorderService {

    private final ObjectProperty<RecordingState> state = new SimpleObjectProperty<>(RecordingState.IDLE);
    private AudioSettings settings = new AudioSettings();
    private TargetDataLine line;
    private Thread recordingThread;
    private File outputFile;

    private AmplitudeListener amplitudeListener;
    private TimerListener timerListener;

    private double elapsedSeconds = 0;
    private long totalBytesRecorded = 0;

    public interface AmplitudeListener {
        void onAmplitude(double normalizedAmplitude);
    }

    public interface TimerListener {
        void onTimerUpdate(double elapsedSeconds);
    }

    public ObjectProperty<RecordingState> stateProperty() {
        return state;
    }

    public RecordingState getState() {
        return state.get();
    }

    public void setAudioSettings(AudioSettings settings) {
        this.settings = settings;
    }

    public AudioSettings getAudioSettings() {
        return settings;
    }

    public void setAmplitudeListener(AmplitudeListener listener) {
        this.amplitudeListener = listener;
    }

    public void setTimerListener(TimerListener listener) {
        this.timerListener = listener;
    }

    public synchronized void startRecording(File file) throws Exception {
        if (state.get() != RecordingState.IDLE) {
            return;
        }

        this.outputFile = file;
        this.elapsedSeconds = 0;
        this.totalBytesRecorded = 0;

        AudioFormat format = settings.getAudioFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (settings.getSelectedMixer() != null) {
            Mixer mixer = AudioSystem.getMixer(settings.getSelectedMixer());
            line = (TargetDataLine) mixer.getLine(info);
        } else {
            line = (TargetDataLine) AudioSystem.getLine(info);
        }

        line.open(format);
        line.start();

        state.set(RecordingState.RECORDING);

        recordingThread = new Thread(this::recordLoop, "AudioRecorderThread");
        recordingThread.setDaemon(true);
        recordingThread.start();
    }

    public synchronized void pauseRecording() {
        if (state.get() == RecordingState.RECORDING) {
            state.set(RecordingState.PAUSED);
        }
    }

    public synchronized void resumeRecording() {
        if (state.get() == RecordingState.PAUSED) {
            state.set(RecordingState.RECORDING);
        }
    }

    public synchronized void stopRecording() {
        if (state.get() == RecordingState.IDLE) {
            return;
        }
        state.set(RecordingState.IDLE);
        if (line != null) {
            line.stop();
            line.close();
        }

        // Wait for recordingThread to finalize WAV header write
        if (recordingThread != null && recordingThread.isAlive()) {
            try {
                recordingThread.join(2500);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void recordLoop() {
        AudioFormat format = settings.getAudioFormat();
        int bufferSize = (int) (format.getSampleRate() * format.getFrameSize() * 0.05);
        byte[] buffer = new byte[bufferSize];

        File tempFile = new File(outputFile.getAbsolutePath() + ".tmp");

        try (FileOutputStream fos = new FileOutputStream(tempFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            while (state.get() != RecordingState.IDLE) {
                if (state.get() == RecordingState.PAUSED) {
                    try {
                        Thread.sleep(80);
                    } catch (InterruptedException e) {
                        break;
                    }
                    if (amplitudeListener != null) {
                        amplitudeListener.onAmplitude(0.0);
                    }
                    continue;
                }

                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    bos.write(buffer, 0, bytesRead);
                    totalBytesRecorded += bytesRead;

                    double maxAmp = calculateRMS(buffer, bytesRead, format);
                    if (amplitudeListener != null) {
                        amplitudeListener.onAmplitude(maxAmp);
                    }

                    elapsedSeconds = (double) totalBytesRecorded / format.getFrameRate() / format.getFrameSize();
                    if (timerListener != null) {
                        timerListener.onTimerUpdate(elapsedSeconds);
                    }
                }
            }

            bos.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Finalize WAV file header
        try {
            writeWavFile(tempFile, outputFile, format, totalBytesRecorded);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double calculateRMS(byte[] buffer, int bytesRead, AudioFormat format) {
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        if (bytesPerSample <= 0) return 0.0;

        int sampleCount = bytesRead / bytesPerSample;
        double sumSquare = 0;

        ByteBuffer bb = ByteBuffer.wrap(buffer, 0, bytesRead);
        bb.order(format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < sampleCount; i++) {
            double sample = 0;
            if (bytesPerSample == 2) {
                sample = bb.getShort();
                sample = sample / 32768.0;
            } else if (bytesPerSample == 1) {
                sample = (bb.get() & 0xFF) - 128;
                sample = sample / 128.0;
            }
            sumSquare += sample * sample;
        }

        double rms = Math.sqrt(sumSquare / Math.max(1, sampleCount));
        return Math.min(1.0, rms * 3.0);
    }

    private void writeWavFile(File pcmFile, File wavFile, AudioFormat format, long pcmDataLength) throws IOException {
        long totalDataLen = pcmDataLength + 36;
        int channels = format.getChannels();
        long longSampleRate = (long) format.getSampleRate();
        int bitsPerSample = format.getSampleSizeInBits();
        long byteRate = longSampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(wavFile)));
             InputStream in = new BufferedInputStream(new FileInputStream(pcmFile))) {

            out.writeBytes("RIFF");
            out.writeInt(Integer.reverseBytes((int) totalDataLen));
            out.writeBytes("WAVE");

            out.writeBytes("fmt ");
            out.writeInt(Integer.reverseBytes(16));
            out.writeShort(Short.reverseBytes((short) 1));
            out.writeShort(Short.reverseBytes((short) channels));
            out.writeInt(Integer.reverseBytes((int) longSampleRate));
            out.writeInt(Integer.reverseBytes((int) byteRate));
            out.writeShort(Short.reverseBytes((short) blockAlign));
            out.writeShort(Short.reverseBytes((short) bitsPerSample));

            out.writeBytes("data");
            out.writeInt(Integer.reverseBytes((int) pcmDataLength));

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}
