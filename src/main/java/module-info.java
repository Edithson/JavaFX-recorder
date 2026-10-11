module com.moafogaus {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;

    opens com.moafogaus to javafx.fxml;
    opens com.moafogaus.controller to javafx.fxml;

    exports com.moafogaus;
    exports com.moafogaus.controller;
    exports com.moafogaus.model;
    exports com.moafogaus.service;
    exports com.moafogaus.ui;
    exports com.moafogaus.util;
}
