import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

public class ImgToSound extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static final String IMAGE_PATH = "Pictures/daftpunk.jpg";
    Mat originalImage = Imgcodecs.imread(IMAGE_PATH);

    @Override
    public void start(Stage primaryStage) {
        Mat processedImage = processImage(originalImage);
        Image image = convertMatToImage(processedImage);

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);

        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root, 1000, 1000);

        primaryStage.setTitle("ImgToSound");
        primaryStage.setScene(scene);
        primaryStage.show();

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> playSoundFromImage(processedImage)));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private Mat processImage(Mat image) {
        Mat grayImage = new Mat();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

        Mat resizedImage = new Mat();
        Imgproc.resize(grayImage, resizedImage, new Size(64, 64));

        Mat quantizedImage = new Mat(resizedImage.size(), CvType.CV_8U);
        for (int y = 0; y < resizedImage.rows(); y++) {
            for (int x = 0; x < resizedImage.cols(); x++) {
                double[] pixel = resizedImage.get(y, x);
                double grayValue = pixel[0]; // Valeur de niveau de gris (0-255)
                int quantizedValue = (int) (grayValue / 16) * 16; // Réduction à 16 niveaux
                quantizedImage.put(y, x, quantizedValue);
            }
        }

        return quantizedImage;
    }

    private Image convertMatToImage(Mat mat) {
        BufferedImage bufferedImage = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < mat.rows(); y++) {
            for (int x = 0; x < mat.cols(); x++) {
                int gray = (int) mat.get(y, x)[0];
                int pixel = (gray << 16) | (gray << 8) | gray;
                bufferedImage.setRGB(x, y, pixel);
            }
        }
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    private void playSoundFromImage(Mat image) {
        try {
            float sampleRate = 44100;
            int durationPerColumn = (int) (sampleRate / 64); // Durée d'une colonne en échantillons
            byte[] finalBuffer = new byte[64 * durationPerColumn];
            double minFreq = 0;
            double maxFreq = 1500;

            for (int x = 0; x < image.cols(); x++) {
                double[] columnWave = new double[durationPerColumn];

                // Fenêtre de Hanning pour une transition plus douce
                double[] window = new double[durationPerColumn];
                for (int i = 0; i < durationPerColumn; i++) {
                    window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (durationPerColumn - 1)));
                }

                for (int y = 0; y < image.rows(); y++) {
                    int grayLevel = (int) image.get(y, x)[0] / 16; // Niveau de gris réduit (0-15)
                    if (grayLevel == 0) continue; // Ignore les pixels noirs

                    double frequency = minFreq + ((maxFreq - minFreq) / 64) * y;
                    for (int i = 0; i < durationPerColumn; i++) {
                        double time = i / sampleRate;
                        columnWave[i] += Math.sin(2 * Math.PI * frequency * time) * (grayLevel / 15.0) * window[i];
                    }
                }

                // Normalisation et copie dans le buffer final
                for (int i = 0; i < durationPerColumn; i++) {
                    finalBuffer[x * durationPerColumn + i] = (byte) (Math.min(Math.max(columnWave[i], -1.0), 1.0) * 127);
                }
            }

            // Configurer le format audio
            AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, true);

            // Enregistrer le son dans un fichier WAV
            File wavFile = new File("output.wav");
            try (AudioInputStream audioStream = new AudioInputStream(
                    new ByteArrayInputStream(finalBuffer),
                    format,
                    finalBuffer.length)) {
                AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, wavFile);
            }

            // Lire le son généré (optionnel)
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();
            line.write(finalBuffer, 0, finalBuffer.length);
            line.drain();
            line.close();

            System.out.println("Son enregistré sous : " + wavFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}