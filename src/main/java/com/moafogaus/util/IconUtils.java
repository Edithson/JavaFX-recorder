package com.moafogaus.util;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.paint.Color;

public class IconUtils {

    public static FontIcon createIcon(Feather iconCode, String colorHex, int size) {
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconColor(Color.web(colorHex));
        icon.setIconSize(size);
        return icon;
    }

    public static FontIcon createMicrophoneIcon(String colorHex, int size) {
        return createIcon(Feather.MIC, colorHex, size);
    }

    public static FontIcon createFolderIcon(String colorHex, int size) {
        return createIcon(Feather.FOLDER, colorHex, size);
    }

    public static FontIcon createPlayIcon(String colorHex, int size) {
        return createIcon(Feather.PLAY, colorHex, size);
    }

    public static FontIcon createPauseIcon(String colorHex, int size) {
        return createIcon(Feather.PAUSE, colorHex, size);
    }

    public static FontIcon createStopIcon(String colorHex, int size) {
        return createIcon(Feather.SQUARE, colorHex, size);
    }

    public static FontIcon createTrashIcon(String colorHex, int size) {
        return createIcon(Feather.TRASH_2, colorHex, size);
    }
}
