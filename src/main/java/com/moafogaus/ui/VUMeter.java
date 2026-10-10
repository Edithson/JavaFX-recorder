package com.moafogaus.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

public class VUMeter extends Canvas {

    private double targetLevel = 0.0;
    private double currentLevel = 0.0;
    private double peakLevel = 0.0;
    private boolean paused = false;

    public VUMeter() {
        widthProperty().addListener((obs, oldVal, newVal) -> redraw());
        heightProperty().addListener((obs, oldVal, newVal) -> redraw());

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateAndDraw();
            }
        };
        timer.start();
    }

    public void setLevel(double level) {
        if (paused) return;
        this.targetLevel = Math.max(0.0, Math.min(1.0, level));
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        if (paused) {
            this.targetLevel = 0.0;
        }
    }

    private void updateAndDraw() {
        if (!paused) {
            if (targetLevel > currentLevel) {
                currentLevel += (targetLevel - currentLevel) * 0.4;
            } else {
                currentLevel += (targetLevel - currentLevel) * 0.1;
            }

            if (currentLevel > peakLevel) {
                peakLevel = currentLevel;
            } else {
                peakLevel = Math.max(0.0, peakLevel - 0.02);
            }
        } else {
            currentLevel = Math.max(0.0, currentLevel - 0.05);
            peakLevel = Math.max(0.0, peakLevel - 0.05);
        }

        redraw();
    }

    public void redraw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        gc.setFill(Color.web("#181825"));
        gc.fillRoundRect(0, 0, w, h, 6, 6);

        if (paused) {
            gc.setFill(Color.web("#FAB387", 0.6));
            gc.fillRoundRect(0, 0, w, h, 6, 6);
            return;
        }

        double fillWidth = w * currentLevel;
        if (fillWidth > 0) {
            LinearGradient levelGrad = new LinearGradient(
                    0, 0, w, 0, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#A6E3A1")),
                    new Stop(0.7, Color.web("#F9E2AF")),
                    new Stop(1.0, Color.web("#F38BA8"))
            );
            gc.setFill(levelGrad);
            gc.fillRoundRect(0, 0, fillWidth, h, 6, 6);
        }

        if (peakLevel > 0.02) {
            double peakX = Math.min(w - 3, w * peakLevel);
            gc.setFill(Color.web("#CDD6F4"));
            gc.fillRect(peakX, 0, 2, h);
        }
    }
}
