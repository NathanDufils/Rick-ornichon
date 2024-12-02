import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.sound.sampled.*;
import java.awt.image.BufferedImage;

public class ImgToSound extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static final int SAMPLE_RATE = 44100;  // Fréquence d'échantillonnage (Hz)
    private static final int DURATION_PER_COLUMN = SAMPLE_RATE / 64;
    private static final int FREQ_MIN = 200;
    private static final int FREQ_MAX = 15000;

    @Override
    public void start(Stage primaryStage) {
        // Chemin de l'image
        String imagePath = "Pictures/shapes.png";

        // Charger et traiter l'image
        Mat originalImage = Imgcodecs.imread(imagePath);
        if (originalImage.empty()) {
            System.err.println("Impossible de charger l'image : " + imagePath);
            return;
        }

        Mat processedImage = processImage(originalImage);

        // Afficher l'image dans une fenêtre JavaFX
        ImageView imageView = new ImageView(SwingFXUtils.toFXImage(matToBufferedImage(processedImage), null));
        StackPane root = new StackPane(imageView);

        Scene scene = new Scene(root, 400, 400);
        primaryStage.setTitle("Image Traité - 64x64");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Boucle infinie pour rejouer le son
        new Thread(() -> {
            while (true) {
                // Générer le son pour chaque colonne
                byte[] soundData = generateSound(processedImage);

                // Jouer le son
                playSound(soundData);

                // Pause de 1 seconde
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.err.println("Erreur pendant l'attente : " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    private Mat processImage(Mat image) {
        Mat grayImage = new Mat();
        Mat resizedImage = new Mat();

        // Convertir en niveaux de gris
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Redimensionner à 64x64
        Imgproc.resize(grayImage, resizedImage, new Size(64, 64));

        // Réduction des niveaux de gris à 16 niveaux (0 à 15)
        Core.divide(resizedImage, new Scalar(16), resizedImage);
        return resizedImage;
    }

    private byte[] generateSound(Mat image) {
        int width = image.cols();
        int height = image.rows();

        byte[] soundData = new byte[width * DURATION_PER_COLUMN];
        double[] frequencies = generateFrequencies(height);

        for (int col = 0; col < width; col++) {
            double[] columnSound = new double[DURATION_PER_COLUMN];

            for (int row = 0; row < height; row++) {
                double grayLevel = image.get(row, col)[0];
                if (grayLevel == 0) continue;

                double amplitude = grayLevel / 15.0; // Normaliser entre 0 et 1
                double frequency = frequencies[row];

                for (int t = 0; t < DURATION_PER_COLUMN; t++) {
                    double time = (double) t / SAMPLE_RATE;
                    columnSound[t] += amplitude * Math.sin(2 * Math.PI * frequency * time);
                }
            }

            // Convertir en échantillons audio
            for (int t = 0; t < DURATION_PER_COLUMN; t++) {
                int sampleIndex = col * DURATION_PER_COLUMN + t;
                soundData[sampleIndex] = (byte) (Math.min(127, Math.max(-128, columnSound[t] * 127)));
            }
        }

        return soundData;
    }

    private double[] generateFrequencies(int size) {
        double[] frequencies = new double[size];
        double step = (FREQ_MAX - FREQ_MIN) / (double) size;

        for (int i = 0; i < size; i++) {
            frequencies[i] = FREQ_MIN + i * step;
        }

        return frequencies;
    }

    private void playSound(byte[] soundData) {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, true);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);

            line.open(format);
            line.start();
            line.write(soundData, 0, soundData.length);
            line.drain();
            line.close();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        int type = (mat.channels() > 1) ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        mat.get(0, 0, ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
    }

    public static void main(String[] args) {
        launch(args);
    }
}