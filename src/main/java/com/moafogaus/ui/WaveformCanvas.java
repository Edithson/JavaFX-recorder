package com.moafogaus.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class WaveformCanvas extends Canvas {

    private final int MAX_SAMPLES = 120;
    private final double[] samples = new double[MAX_SAMPLES];
    private int writeIndex = 0;
    private boolean active = false;
    private boolean paused = false;
    private AnimationTimer timer;

    public WaveformCanvas() {
        widthProperty().addListener((obs, oldVal, newVal) -> redraw());
        heightProperty().addListener((obs, oldVal, newVal) -> redraw());

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                redraw();
            }
        };
        timer.start();
    }

    public void addSample(double normalizedAmplitude) {
        if (paused) return; // Freeze samples on pause
        double value = Math.max(0.02, Math.min(1.0, normalizedAmplitude));
        samples[writeIndex] = value;
        writeIndex = (writeIndex + 1) % MAX_SAMPLES;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            this.paused = false;
        }
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void clear() {
        for (int i = 0; i < MAX_SAMPLES; i++) {
            samples[i] = 0.02;
        }
        active = false;
        paused = false;
        redraw();
    }

    public void redraw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double centerY = h / 2.0;

        // Draw grid lines
        gc.setStroke(Color.web("#2A2A3D", 0.6));
        gc.setLineWidth(1.0);
        gc.strokeLine(0, centerY, w, centerY);
        gc.strokeLine(0, centerY - h * 0.25, w, centerY - h * 0.25);
        gc.strokeLine(0, centerY + h * 0.25, w, centerY + h * 0.25);

        // Define gradients depending on state
        LinearGradient gradient;
        if (paused) {
            // Warm Amber / Orange gradient when paused
            gradient = new LinearGradient(
                    0, 0, w, 0, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#FAB387")),
                    new Stop(1.0, Color.web("#F9E2AF"))
            );
        } else {
            // Cool Blue / Pink gradient when active recording
            gradient = new LinearGradient(
                    0, 0, w, 0, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#89B4FA")),
                    new Stop(0.5, Color.web("#74C7EC")),
                    new Stop(1.0, Color.web("#F38BA8"))
            );
        }

        gc.setLineWidth(2.5);
        gc.setFill(gradient);

        double stepX = w / (double) (MAX_SAMPLES - 1);

        // Render bars
        for (int i = 0; i < MAX_SAMPLES; i++) {
            int index = (writeIndex + i) % MAX_SAMPLES;
            double amp = samples[index];
            double barHeight = amp * (h * 0.85);

            double x = i * stepX;
            double yTop = centerY - (barHeight / 2.0);

            if (active || paused) {
                gc.setGlobalAlpha(paused ? 0.65 : 0.90);
                gc.fillRoundRect(x, yTop, Math.max(2.0, stepX - 1.5), barHeight, 3, 3);
            } else {
                gc.setGlobalAlpha(0.20);
                gc.fillRoundRect(x, centerY - 1, Math.max(2.0, stepX - 1.5), 2, 1, 1);
            }
        }

        // Distinct visual overlay when PAUSED
        if (paused) {
            gc.setGlobalAlpha(0.25);
            gc.setFill(Color.web("#1E1E2E"));
            gc.fillRect(0, 0, w, h);

            gc.setGlobalAlpha(0.95);
            gc.setFill(Color.web("#FAB387"));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
            gc.fillText("⏸ EN PAUSE - FORMATION FOGÉE", w / 2.0 - 130, centerY + 6);
        }

        gc.setGlobalAlpha(1.0);
    }
}
