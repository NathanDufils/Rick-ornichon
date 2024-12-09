import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.sound.sampled.*;
import java.io.*;
public class ImgToSound {

    // Chargement de la bibliothèque OpenCV
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }


    public static void generateWavFile(String filePath, Double[] frequencies, int durationMs) {
        int SAMPLE_RATE = 44100; // Taux d'échantillonnage audio standard (44,1 kHz)
        try {
            // Créer un flux de sortie pour stocker les données audio
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            for (double frequency : frequencies) {
                // Calcul de la taille du buffer pour chaque fréquence
                int bufferSize = (int) (durationMs * SAMPLE_RATE / 1000);
                byte[] buffer = new byte[bufferSize];

                // Génération de l'onde sinusoïdale
                for (int i = 0; i < buffer.length; i++) {
                    double angle = 2.0 * Math.PI * i / (SAMPLE_RATE / frequency);
                    buffer[i] = (byte) (Math.sin(angle) * 127);
                }

                // Ajouter le buffer au flux
                byteArrayOutputStream.write(buffer);
            }

            // Obtenir toutes les données générées
            byte[] audioData = byteArrayOutputStream.toByteArray();

            // Créer un format audio
            AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, 8, 1, true, true);

            // Créer un flux audio encapsulant les données générées
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioInputStream = new AudioInputStream(bais, audioFormat, audioData.length);

            // Écrire le fichier WAV
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File(filePath));

            System.out.println("Fichier WAV généré : " + filePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String imagePath = "Pictures/shapes.png"; // Remplacez par le chemin de votre image
        Mat image = Imgcodecs.imread(imagePath);
        Double[] frequenceTab = new Double[64];

        if (image.empty()) {
            System.out.println("Erreur lors du chargement de l'image.");
            return;
        }

        // Convertir l'image en niveaux de gris
        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(image, grayscaleImage, Imgproc.COLOR_BGR2GRAY);

        Mat resizedImage = new Mat();
        Imgproc.resize(grayscaleImage, resizedImage, new Size(64, 64));

        // Calculer les fréquences basées sur les niveaux de gris
        for (int col = 0; col < resizedImage.cols(); col++) {
            System.out.print("Colonne " + (col + 1) + ": ");
            double frequency = 0;
            // Parcourir chaque pixel de la colonne du bas vers le haut
            for (int rowIdx = resizedImage.rows() - 1; rowIdx >= 0; rowIdx--) {
                byte[] pixelData = new byte[1];
                resizedImage.get(rowIdx, col, pixelData);
                frequency += (pixelData[0] & 0xFF) * Math.sin(2*Math.PI*((double) rowIdx /64)*((double) (col + 1) /(resizedImage.cols())));
            }
            frequenceTab[col] = frequency;
            System.out.println(frequency);
        }

        // Générer une onde continue pour toutes les fréquences
        int durationPerToneMillis = 15; // Durée de chaque tonalité en millisecondes
        generateWavFile("output.wav", frequenceTab, durationPerToneMillis);
    }
}
