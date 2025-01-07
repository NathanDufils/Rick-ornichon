package main.java;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
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
    private double baseFrequency = 2000.0;
    private final TraitementImage traitementImage;
    private String selectedFilePath;

    private final HBox thumbnailBox;
    private final List<String> imagePaths;
    private int currentImageIndex = 0;
    private Thread playbackThread;

    public InterfaceController() {
        this.imageView = new ImageView();
        this.imageView.setPreserveRatio(true);
        this.imageView.setFitHeight(200);
        this.imageView.setFitWidth(-1);

        this.statusLabel = new Label("État : Inactif");

        this.frequencySlider = new Slider(500, 5000, 2000);
        this.frequencyInput = new TextField(String.valueOf((int) baseFrequency));

        this.traitementImage = new TraitementImage();
        this.selectedFilePath = null;

        this.thumbnailBox = new HBox(10);
        this.thumbnailBox.setAlignment(Pos.CENTER);
        this.thumbnailBox.setPadding(new Insets(10));
        this.imagePaths = new ArrayList<>();

        // Configurer le curseur
        frequencySlider.setShowTickLabels(true);
        frequencySlider.setShowTickMarks(true);
        frequencySlider.setMajorTickUnit(500);
        frequencySlider.setBlockIncrement(100);

        // Synchroniser le curseur et l'input
        frequencySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            baseFrequency = newValue.doubleValue();
            frequencyInput.setText(String.valueOf((int) baseFrequency));
        });

        frequencyInput.setOnAction(e -> updateFrequencyFromInput());
    }

    public Scene createInterface(Stage primaryStage) {
        // Buttons
        Button loadImageButton = new Button("Charger un Fichier");
        Button playButton = new Button("Lire");
        Button pauseButton = new Button("Pause");
        Button resumeButton = new Button("Reprendre");

        // Top section for file loading
        VBox topBox = new VBox(loadImageButton);
        topBox.setAlignment(Pos.CENTER);

        // Control buttons
        HBox controlsBox = new HBox(10, playButton, pauseButton, resumeButton);
        controlsBox.setAlignment(Pos.CENTER);

        // Frequency controls
        HBox frequencyBox = new HBox(10, new Label("Fréquence : "), frequencyInput, frequencySlider);
        frequencyBox.setAlignment(Pos.CENTER);

        // Combine controls and status into infoBox
        VBox infoBox = new VBox(10, controlsBox, frequencyBox, statusLabel);
        infoBox.setAlignment(Pos.CENTER);

        // Combine infoBox and thumbnailBox
        VBox bottomBox = new VBox(10, thumbnailBox, infoBox);
        bottomBox.setAlignment(Pos.CENTER);

        // Button actions
        loadImageButton.setOnAction(e -> loadFile(primaryStage));
        playButton.setOnAction(e -> playAudio());
        pauseButton.setOnAction(e -> pauseAudio());
        resumeButton.setOnAction(e -> resumeAudio());

        // Layout setup
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
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        fileChooser.setInitialDirectory(new File("src/main/resources"));

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
        if (selectedFiles != null) {
            imagePaths.clear();
            thumbnailBox.getChildren().clear();

            for (int index = 0; index < selectedFiles.size(); index++) {
                File file = selectedFiles.get(index);
                imagePaths.add(file.getAbsolutePath());

                Image thumbnail = new Image("file:" + file.getAbsolutePath(), 100, 100, true, true);
                ImageView thumbnailView = new ImageView(thumbnail);

                int finalIndex = index;
                thumbnailView.setOnMouseClicked(e -> {
                    currentImageIndex = finalIndex;
                    selectedFilePath = imagePaths.get(currentImageIndex);
                    imageView.setImage(new Image("file:" + selectedFilePath));
                    statusLabel.setText("État : Image sélectionnée");
                });

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
        cycleThroughImages();
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
                frequencyInput.setText(String.valueOf((int) baseFrequency)); // Rétablir la valeur précédente
            }
        } catch (NumberFormatException e) {
            frequencyInput.setText(String.valueOf((int) baseFrequency)); // Rétablir la valeur précédente
        }
    }

    private void cycleThroughImages() {
        if (imagePaths.isEmpty()) {
            statusLabel.setText("État : Aucun fichier sélectionné !");
            return;
        }

        stopCurrentAudio();

        playbackThread = new Thread(() -> {
            try {
                while (true) {
                    selectedFilePath = imagePaths.get(currentImageIndex);

                    Platform.runLater(() -> {
                        imageView.setImage(new Image("file:" + selectedFilePath));
                        statusLabel.setText("État : Lecture - Image " + (currentImageIndex + 1));
                    });

                    traitementImage.transformerEtGenererAudio(selectedFilePath, baseFrequency);
                    loadAudioClip();
                    if (audioClip != null) {
                        audioClip.start();
                        audioClip.loop(2);
                        Thread.sleep(3000);
                    }

                    currentImageIndex = (currentImageIndex + 1) % imagePaths.size();
                }
            } catch (InterruptedException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("État : Pause");
                });
            }
        });
        playbackThread.start();
    }
}