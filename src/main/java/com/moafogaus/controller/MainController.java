package com.moafogaus.controller;

import com.moafogaus.model.AudioRecording;
import com.moafogaus.model.AudioSettings;
import com.moafogaus.model.PlaybackState;
import com.moafogaus.model.RecordingState;
import com.moafogaus.service.AudioPlayerService;
import com.moafogaus.service.AudioRecorderService;
import com.moafogaus.service.DeviceManager;
import com.moafogaus.service.RecordingManager;
import com.moafogaus.ui.VUMeter;
import com.moafogaus.ui.WaveformCanvas;
import com.moafogaus.util.FormatUtils;
import com.moafogaus.util.IconUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import javax.sound.sampled.Mixer;
import java.io.File;
import java.util.List;
import java.util.Optional;

public class MainController {

    // Header Controls & Brand
    @FXML private Label brandIconLabel;
    @FXML private Button openFolderHeaderButton;
    @FXML private ComboBox<Mixer.Info> deviceComboBox;
    @FXML private Label folderPathLabel;
    @FXML private Button changeFolderButton;

    // Recording Controls & Visuals
    @FXML private Label timerLabel;
    @FXML private Label recordingStatusLabel;
    @FXML private Button recordButton;
    @FXML private Button stopButton;
    @FXML private WaveformCanvas waveformCanvas;
    @FXML private VUMeter vuMeter;

    // Recording List
    @FXML private ListView<AudioRecording> recordingsListView;

    // Services
    private final DeviceManager deviceManager = new DeviceManager();
    private final AudioRecorderService recorderService = new AudioRecorderService();
    private final AudioPlayerService playerService = new AudioPlayerService();
    private final RecordingManager recordingManager = new RecordingManager();

    private File currentRecordingFile;

    @FXML
    public void initialize() {
        setupBrandAndIcons();
        setupDeviceComboBox();
        setupOutputFolderDisplay();
        setupRecordingsListView();
        setupAudioRecorderListeners();
        setupAudioPlayerListeners();
    }

    private void setupBrandAndIcons() {
        if (brandIconLabel != null) {
            brandIconLabel.setGraphic(IconUtils.createMicrophoneIcon("#3B82F6", 24));
            brandIconLabel.setText("");
        }
        if (openFolderHeaderButton != null) {
            openFolderHeaderButton.setGraphic(IconUtils.createFolderIcon("#94A3B8", 16));
            openFolderHeaderButton.setText("");
        }
    }

    private void setupDeviceComboBox() {
        List<Mixer.Info> devices = deviceManager.getAvailableInputDevices();
        deviceComboBox.getItems().clear();
        deviceComboBox.getItems().addAll(devices);

        deviceComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Mixer.Info item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Microphone système" : item.getName());
            }
        });
        deviceComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Mixer.Info item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Microphone système" : item.getName());
            }
        });

        if (!devices.isEmpty()) {
            deviceComboBox.getSelectionModel().select(0);
        }
        deviceComboBox.setOnAction(e -> updateAudioSettings());
        updateAudioSettings();
    }

    private void updateAudioSettings() {
        AudioSettings settings = new AudioSettings();
        settings.setSelectedMixer(deviceComboBox.getValue());
        settings.setSampleRate(44100.0f);
        settings.setChannels(2);
        recorderService.setAudioSettings(settings);
    }

    private void setupOutputFolderDisplay() {
        updateFolderLabelText();
    }

    private void updateFolderLabelText() {
        File dir = recordingManager.getStorageDir();
        if (folderPathLabel != null) {
            folderPathLabel.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    private void onSelectOutputFolderAction() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choisir le dossier de sauvegarde automatique");
        if (recordingManager.getStorageDir().exists()) {
            chooser.setInitialDirectory(recordingManager.getStorageDir());
        }

        File selectedDir = chooser.showDialog(folderPathLabel.getScene().getWindow());
        if (selectedDir != null) {
            recordingManager.setStorageDir(selectedDir);
            updateFolderLabelText();
        }
    }

    @FXML
    private void onOpenFolderAction() {
        recordingManager.openStorageFolder();
    }

    private void setupRecordingsListView() {
        recordingsListView.setItems(recordingManager.getRecordings());

        recordingsListView.setCellFactory(lv -> new ListCell<>() {
            private final HBox card = new HBox(10);
            private final VBox info = new VBox(3);
            private final Label title = new Label();
            private final Label details = new Label();
            private final Button btnPlayPause = new Button();
            private final Button btnStopTrack = new Button();
            private final Button btnDel = new Button();
            private final HBox rowActions = new HBox(6);

            private final javafx.beans.value.ChangeListener<Number> timeListener = (obs, oldVal, newVal) -> {
                AudioRecording item = getItem();
                if (item != null && playerService.getCurrentRecording() != null && playerService.getCurrentRecording().equals(item)) {
                    if (playerService.getState() != PlaybackState.STOPPED) {
                        String timeStr = FormatUtils.formatDuration(newVal.doubleValue()) + " / " + FormatUtils.formatDuration(item.getDurationSeconds());
                        details.setText(timeStr + "  •  " + FormatUtils.formatFileSize(item.getSizeBytes()));
                    }
                }
            };

            {
                card.setAlignment(Pos.CENTER_LEFT);
                card.getStyleClass().add("track-strip");

                title.getStyleClass().add("track-title");
                details.getStyleClass().add("track-meta");
                info.getChildren().addAll(title, details);
                HBox.setHgrow(info, Priority.ALWAYS);

                rowActions.setAlignment(Pos.CENTER_RIGHT);
                rowActions.getChildren().addAll(btnPlayPause, btnStopTrack, btnDel);

                card.getChildren().addAll(info, rowActions);

                playerService.currentTimeSecondsProperty().addListener(timeListener);
            }

            @Override
            protected void updateItem(AudioRecording item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    title.setText(item.getName());

                    boolean isCurrentActive = (playerService.getCurrentRecording() != null && playerService.getCurrentRecording().equals(item));
                    boolean isPlaying = playerService.isPlaying(item);
                    boolean isPaused = playerService.isPaused(item);
                    boolean isRecordingActive = (recorderService.getState() != RecordingState.IDLE);
                    boolean isPlaybackRunning = (playerService.getState() != PlaybackState.STOPPED);

                    if (isCurrentActive && (isPlaying || isPaused)) {
                        if (!card.getStyleClass().contains("playing-active")) {
                            card.getStyleClass().add("playing-active");
                        }
                        String timeStr = FormatUtils.formatDuration(playerService.getCurrentTimeSeconds()) + " / " + FormatUtils.formatDuration(item.getDurationSeconds());
                        details.setText(timeStr + "  •  " + FormatUtils.formatFileSize(item.getSizeBytes()));
                    } else {
                        card.getStyleClass().remove("playing-active");
                        details.setText(FormatUtils.formatDuration(item.getDurationSeconds()) + "  •  " + FormatUtils.formatFileSize(item.getSizeBytes()));
                    }

                    // Dynamic Play / Pause Button
                    btnPlayPause.setDisable(isRecordingActive);

                    if (isRecordingActive) {
                        btnPlayPause.setTooltip(new Tooltip("Lecture désactivée pendant l'enregistrement micro"));
                    }

                    if (isCurrentActive && isPlaying) {
                        btnPlayPause.setGraphic(IconUtils.createPauseIcon("#0F172A", 14));
                        btnPlayPause.setTooltip(new Tooltip("Mettre en pause"));
                        btnPlayPause.getStyleClass().setAll("button", "btn-symbol-pause");
                        btnPlayPause.setOnAction(e -> {
                            playerService.pause();
                            recordingsListView.refresh();
                        });
                    } else if (isCurrentActive && isPaused) {
                        btnPlayPause.setGraphic(IconUtils.createPlayIcon("#0F172A", 14));
                        btnPlayPause.setTooltip(new Tooltip("Reprendre l'écoute"));
                        btnPlayPause.getStyleClass().setAll("button", "btn-symbol-resume");
                        btnPlayPause.setOnAction(e -> {
                            playerService.play();
                            recordingsListView.refresh();
                        });
                    } else {
                        btnPlayPause.setGraphic(IconUtils.createPlayIcon("#FFFFFF", 14));
                        btnPlayPause.setTooltip(new Tooltip("Écouter la piste"));
                        btnPlayPause.getStyleClass().setAll("button", "btn-symbol-play");
                        btnPlayPause.setOnAction(e -> {
                            recordingsListView.getSelectionModel().select(item);
                            playerService.loadAndPlay(item);
                            recordingsListView.refresh();
                        });
                    }

                    // Permanent Instant-Stop Button (⏹)
                    boolean canStopActive = (isCurrentActive && isPlaybackRunning);
                    btnStopTrack.setGraphic(IconUtils.createStopIcon(canStopActive ? "#EF4444" : "#64748B", 14));
                    btnStopTrack.setTooltip(new Tooltip("Arrêter la lecture net"));
                    btnStopTrack.getStyleClass().setAll("button", "btn-symbol-stop");
                    btnStopTrack.setDisable(isRecordingActive || !isPlaybackRunning);
                    btnStopTrack.setOnAction(e -> {
                        playerService.stop();
                        recordingsListView.refresh();
                    });

                    // Delete Button
                    btnDel.setGraphic(IconUtils.createTrashIcon("#EF4444", 14));
                    btnDel.setTooltip(new Tooltip("Supprimer cette piste"));
                    btnDel.getStyleClass().setAll("button", "btn-symbol-delete");
                    btnDel.setDisable(isRecordingActive);
                    btnDel.setOnAction(e -> confirmAndDelete(item));

                    setGraphic(card);
                }
            }
        });
    }

    private void confirmAndDelete(AudioRecording recording) {
        if (recording == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression de piste");
        alert.setHeaderText("Supprimer " + recording.getName() + " ?");
        alert.setContentText("Cette action supprimera définitivement le fichier audio.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (playerService.getCurrentRecording() == recording) {
                playerService.stop();
            }
            recordingManager.deleteRecording(recording);
            recordingsListView.refresh();
        }
    }

    private void setupAudioRecorderListeners() {
        recorderService.setAmplitudeListener(amp -> Platform.runLater(() -> {
            waveformCanvas.addSample(amp);
            vuMeter.setLevel(amp);
        }));

        recorderService.setTimerListener(elapsedSec -> Platform.runLater(() -> {
            timerLabel.setText(FormatUtils.formatTimer(elapsedSec));
        }));

        recorderService.stateProperty().addListener((obs, oldVal, newState) -> Platform.runLater(() -> {
            switch (newState) {
                case IDLE:
                    recordButton.setText(" DÉMARRER");
                    recordButton.setGraphic(IconUtils.createMicrophoneIcon("#FFFFFF", 18));
                    recordButton.getStyleClass().removeAll("btn-rec-active", "btn-rec-paused");
                    recordingStatusLabel.setText("PRÊT POUR ENREGISTREMENT");
                    recordingStatusLabel.setStyle("-fx-text-fill: #10B981;");
                    stopButton.setDisable(true);
                    stopButton.setGraphic(IconUtils.createStopIcon("#64748B", 16));
                    deviceComboBox.setDisable(false);
                    changeFolderButton.setDisable(false);
                    waveformCanvas.setActive(false);
                    waveformCanvas.setPaused(false);
                    vuMeter.setLevel(0);
                    vuMeter.setPaused(false);
                    recordingsListView.refresh();
                    break;

                case RECORDING:
                    playerService.stop(); // Stop any audio playback when starting recording
                    recordButton.setText(" PAUSE");
                    recordButton.setGraphic(IconUtils.createPauseIcon("#0F172A", 18));
                    recordButton.getStyleClass().removeAll("btn-rec-paused");
                    if (!recordButton.getStyleClass().contains("btn-rec-active")) {
                        recordButton.getStyleClass().add("btn-rec-active");
                    }
                    recordingStatusLabel.setText("🔴 ENREGISTREMENT EN COURS...");
                    recordingStatusLabel.setStyle("-fx-text-fill: #EF4444;");
                    stopButton.setDisable(false);
                    stopButton.setGraphic(IconUtils.createStopIcon("#F1F5F9", 16));
                    deviceComboBox.setDisable(true);
                    changeFolderButton.setDisable(true);
                    waveformCanvas.setActive(true);
                    waveformCanvas.setPaused(false);
                    vuMeter.setPaused(false);
                    recordingsListView.refresh();
                    break;

                case PAUSED:
                    recordButton.setText(" REPRENDRE");
                    recordButton.setGraphic(IconUtils.createPlayIcon("#0F172A", 18));
                    recordButton.getStyleClass().removeAll("btn-rec-active");
                    if (!recordButton.getStyleClass().contains("btn-rec-paused")) {
                        recordButton.getStyleClass().add("btn-rec-paused");
                    }
                    recordingStatusLabel.setText("⏸ PAUSED - CAPTURE EN PAUSE");
                    recordingStatusLabel.setStyle("-fx-text-fill: #F59E0B;");
                    waveformCanvas.setPaused(true);
                    vuMeter.setPaused(true);
                    recordingsListView.refresh();
                    break;
            }
        }));
    }

    private void setupAudioPlayerListeners() {
        playerService.stateProperty().addListener((obs, oldVal, newState) -> Platform.runLater(() -> {
            recordingsListView.refresh();
        }));

        playerService.currentRecordingProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(() -> {
            recordingsListView.refresh();
        }));
    }

    @FXML
    private void onRecordAction() {
        if (recorderService.getState() == RecordingState.IDLE) {
            playerService.stop();
            currentRecordingFile = recordingManager.createNewRecordingFile();
            try {
                waveformCanvas.clear();
                recorderService.startRecording(currentRecordingFile);
            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Erreur Entrée Audio", "Impossible de démarrer l'enregistrement : " + e.getMessage());
            }
        } else if (recorderService.getState() == RecordingState.RECORDING) {
            recorderService.pauseRecording();
        } else if (recorderService.getState() == RecordingState.PAUSED) {
            recorderService.resumeRecording();
        }
    }

    @FXML
    private void onStopRecordAction() {
        if (recorderService.getState() != RecordingState.IDLE) {
            recorderService.stopRecording();

            Platform.runLater(() -> {
                if (currentRecordingFile != null && currentRecordingFile.exists()) {
                    double duration = playerService.getTotalDurationSeconds();
                    AudioRecording recording = new AudioRecording(currentRecordingFile, duration);
                    recordingManager.addRecording(recording);
                    recordingsListView.getSelectionModel().select(recording);
                    recordingsListView.refresh();
                }
            });
        }
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
