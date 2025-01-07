package main.java;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InterfaceController {

    private final ImageView imageView;
    private Clip audioClip;
    private final Label statusLabel;
    private final Slider frequencySlider;
    private final TextField frequencyInput;
    private double baseFrequency = 1000.0;
    private final TraitementImage traitementImage;
    private String selectedFilePath;

    private final HBox thumbnailBox;
    private final ScrollPane thumbnailScrollPane;
    private final List<String> imagePaths;
    private int currentImageIndex = 0;
    private Thread playbackThread;

    public InterfaceController() {
        this.imageView = new ImageView();
        this.imageView.setPreserveRatio(true);
        this.imageView.setFitHeight(200);
        this.imageView.setFitWidth(-1);

        this.statusLabel = new Label("État : Inactif");

        this.frequencySlider = new Slider(500, 5000, baseFrequency);
        this.frequencyInput = new TextField(String.valueOf((int) baseFrequency));

        this.traitementImage = new TraitementImage();
        this.selectedFilePath = null;

        this.thumbnailBox = new HBox(10);
        this.thumbnailBox.setAlignment(Pos.CENTER);
        this.thumbnailBox.setPadding(new Insets(10));
        this.imagePaths = new ArrayList<>();

        this.thumbnailScrollPane = new ScrollPane(thumbnailBox);
        this.thumbnailScrollPane.setFitToHeight(true);
        this.thumbnailScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        this.thumbnailScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.thumbnailScrollPane.setPrefHeight(120);

        frequencySlider.setShowTickLabels(true);
        frequencySlider.setShowTickMarks(true);
        frequencySlider.setMajorTickUnit(500);
        frequencySlider.setBlockIncrement(100);

        frequencySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            baseFrequency = newValue.doubleValue();
            frequencyInput.setText(String.valueOf((int) baseFrequency));
        });

        frequencyInput.setOnAction(e -> updateFrequencyFromInput());
    }

    public Scene createInterface(Stage primaryStage) {
        // Buttons
        Button loadImageButton = new Button("Charger des fichiers");
        Button playButton = new Button("Lire");
        Button pauseButton = new Button("Pause");
        Button resumeButton = new Button("Reprendre");

        VBox topBox = new VBox(loadImageButton);
        topBox.setAlignment(Pos.CENTER);

        HBox controlsBox = new HBox(10, playButton, pauseButton, resumeButton);
        controlsBox.setAlignment(Pos.CENTER);

        HBox frequencyBox = new HBox(10, new Label("Fréquence : "), frequencyInput, frequencySlider);
        frequencyBox.setAlignment(Pos.CENTER);

        VBox infoBox = new VBox(10, controlsBox, frequencyBox, statusLabel);
        infoBox.setAlignment(Pos.CENTER);

        VBox bottomBox = new VBox(10, thumbnailScrollPane, infoBox);
        bottomBox.setAlignment(Pos.CENTER);

        loadImageButton.setOnAction(e -> loadFile(primaryStage));
        playButton.setOnAction(e -> playAudio());
        pauseButton.setOnAction(e -> pauseAudio());
        resumeButton.setOnAction(e -> resumeAudio());

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(new StackPane(imageView));
        root.setBottom(bottomBox);
        root.setPadding(new Insets(15));

        return new Scene(root, 800, 600);
    }

    private void loadFile(Stage stage) {
        stopCurrentAudio();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner des Fichiers");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images et Vidéos", "*.png", "*.jpg", "*.jpeg", "*.mp4", "*.avi")
        );

        fileChooser.setInitialDirectory(new File("src/main/resources"));

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            imagePaths.clear();
            thumbnailBox.getChildren().clear();

            for (File file : selectedFiles) {
                String filePath = file.getAbsolutePath();

                if (isVideoFile(filePath)) {
                    handleVideoFile(filePath);
                } else {
                    imagePaths.add(filePath);

                    Image thumbnail = new Image("file:" + filePath, 100, 100, true, true);
                    ImageView thumbnailView = new ImageView(thumbnail);
                    thumbnailBox.getChildren().add(thumbnailView);
                }
            }

            if (!imagePaths.isEmpty()) {
                selectedFilePath = imagePaths.getFirst();
                imageView.setImage(new Image("file:" + selectedFilePath));
                statusLabel.setText("État : Prêt");
            }
        }
    }

    private boolean isVideoFile(String filePath) {
        String lowerCasePath = filePath.toLowerCase();
        return lowerCasePath.endsWith(".mp4");
    }

    private void handleVideoFile(String filePath) {
        // Clear the exported_frames directory
        File framesDir = new File("exported_frames");

        VideoFrameExtractor.extractFrames(filePath, "exported_frames");

        loadImagesFromDirectory(framesDir);
    }

    private void loadImagesFromDirectory(File directory) {
        imagePaths.clear();
        thumbnailBox.getChildren().clear();

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"));
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                imagePaths.add(file.getAbsolutePath());

                Image thumbnail = new Image("file:" + file.getAbsolutePath(), 100, 100, true, true);
                ImageView thumbnailView = new ImageView(thumbnail);

                thumbnailBox.getChildren().add(thumbnailView);
            }

            if (!imagePaths.isEmpty()) {
                currentImageIndex = 0;
                selectedFilePath = imagePaths.get(currentImageIndex);
                imageView.setImage(new Image("file:" + selectedFilePath));
                statusLabel.setText("État : Prêt");
            }
        }
    }

    private void playAudio() {
        if (playbackThread != null && playbackThread.isAlive()) {
            return;
        }
        cycleFromCurrentIndex();
    }

    private void pauseAudio() {
        if (playbackThread != null) {
            playbackThread.interrupt();
            playbackThread = null;
            statusLabel.setText("État : Pause");
        }
    }

    private void resumeAudio() {
        if (playbackThread == null) {
            playAudio();
        }
    }

    private void loadAudioClip() {
        try {
            File audioFile = new File("src/main/resources/audio/output.wav");
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            audioClip = AudioSystem.getClip();
            audioClip.open(audioInputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopCurrentAudio() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
            audioClip.close();
        }
        audioClip = null;
    }

    private void updateFrequencyFromInput() {
        try {
            int newFrequency = Integer.parseInt(frequencyInput.getText());
            if (newFrequency >= 500 && newFrequency <= 5000) {
                baseFrequency = newFrequency;
                frequencySlider.setValue(baseFrequency);
            } else {
                frequencyInput.setText(String.valueOf((int) baseFrequency));
            }
        } catch (NumberFormatException e) {
            frequencyInput.setText(String.valueOf((int) baseFrequency));
        }
    }

    private void cycleFromCurrentIndex() {
        playbackThread = new Thread(() -> {
            try {
                while (true) {
                    String currentFilePath;
                    synchronized (this) {
                        currentFilePath = imagePaths.get(currentImageIndex);
                    }

                    Platform.runLater(() -> {
                        imageView.setImage(new Image("file:" + currentFilePath));
                        statusLabel.setText("État : Lecture - Image " + (currentImageIndex + 1));
                        highlightCurrentThumbnail();
                    });

                    traitementImage.transformerEtGenererAudio(currentFilePath, baseFrequency);
                    loadAudioClip();
                    if (audioClip != null) {
                        audioClip.start();
                        audioClip.loop(2);
                        Thread.sleep(3000);
                        SoundGenerator.playSineWave(baseFrequency, 100);
                    }

                    synchronized (this) {
                        currentImageIndex = (currentImageIndex + 1) % imagePaths.size();
                    }
                }
            } catch (InterruptedException e) {
                Platform.runLater(() -> statusLabel.setText("État : Pause"));
            }
        });
        playbackThread.start();
    }

    private void highlightCurrentThumbnail() {
        for (int i = 0; i < thumbnailBox.getChildren().size(); i++) {
            ImageView thumbnail = (ImageView) thumbnailBox.getChildren().get(i);
            if (i == currentImageIndex) {
                thumbnail.setStyle("-fx-border-color: red; -fx-border-width: 3px;");
            } else {
                thumbnail.setStyle(""); // Reset style for other thumbnails
            }
        }
    }
}