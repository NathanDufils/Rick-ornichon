package main.java;

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

public class InterfaceController {

    private final ImageView imageView;
    private Clip audioClip;
    private boolean isPaused = false;
    private final Label statusLabel;
    private final Slider frequencySlider;
    private final TextField frequencyInput;
    private double baseFrequency = 2000.0;
    private final TraitementImage traitementImage;
    private String selectedFilePath;

    public InterfaceController() {
        this.imageView = new ImageView();
        this.imageView.setPreserveRatio(true);
        this.imageView.setFitWidth(400);
        this.imageView.setFitHeight(400);

        this.statusLabel = new Label("État : Inactif");

        this.frequencySlider = new Slider(500, 5000, 2000);
        this.frequencyInput = new TextField(String.valueOf((int) baseFrequency));

        this.traitementImage = new TraitementImage();
        this.selectedFilePath = null;

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
        Button loadImageButton = new Button("Charger un Fichier");
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

        loadImageButton.setOnAction(e -> loadFile(primaryStage));
        playButton.setOnAction(e -> playAudio());
        pauseButton.setOnAction(e -> pauseAudio());
        resumeButton.setOnAction(e -> resumeAudio());

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(new StackPane(imageView));
        root.setBottom(infoBox);
        root.setPadding(new Insets(15));

        return new Scene(root, 800, 600);
    }

    private void loadFile(Stage stage) {
        stopCurrentAudio();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un Fichier");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        fileChooser.setInitialDirectory(new File("src/main/resources/images"));

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            selectedFilePath = selectedFile.getAbsolutePath();
            Image image = new Image("file:" + selectedFilePath);
            imageView.setImage(image);
            statusLabel.setText("État : Prêt");
        }
    }

    private void playAudio() {
        if (selectedFilePath == null) {
            statusLabel.setText("État : Aucun fichier sélectionné !");
            return;
        }

        stopCurrentAudio();

        traitementImage.transformerEtGenererAudio(selectedFilePath, baseFrequency);
        loadAudioClip();

        if (audioClip != null) {
            audioClip.setFramePosition(0);
            audioClip.loop(Clip.LOOP_CONTINUOUSLY);
            statusLabel.setText("État : Lecture");
        }
    }

    private void pauseAudio() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
            isPaused = true;
            statusLabel.setText("État : Pause");
        }
    }

    private void resumeAudio() {
        if (audioClip != null && isPaused) {
            audioClip.start();
            audioClip.loop(Clip.LOOP_CONTINUOUSLY);
            isPaused = false;
            statusLabel.setText("État : Lecture");
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
        isPaused = false;
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
}