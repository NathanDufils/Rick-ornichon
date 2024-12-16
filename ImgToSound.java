import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class ImgToSound {
    public static void main(String[] args) {
        // Charger la bibliothèque OpenCV
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Charger l'image en niveaux de gris
        String imagePath = "Pictures/shapes.png"; // Remplacez par le chemin de votre image
        Mat image = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_GRAYSCALE);

        // Redimensionner l'image à 64x64
        Mat resizedImage = new Mat();
        Imgproc.resize(image, resizedImage, new Size(64, 64));

        // Définir les paramètres
        int N = 64; // Taille de l'image (64x64)
        double f0 = 2000.0; // Fréquence de base en Hz
        double samplingRate = 44100; // Taux d'échantillonnage standard pour le son
        double durationPerColumn = 1.0 / N; // Durée pour chaque colonne
        int samplesPerColumn = (int) (samplingRate * durationPerColumn);

        // Signal audio final
        byte[] audioData = new byte[N * samplesPerColumn * 2]; // Signal PCM 16 bits
        int sampleIndex = 0;

        // Parcourir chaque colonne
        for (int col = 0; col < N; col++) {
            // Récupérer les niveaux de gris pour la colonne
            double[] grayLevels = new double[N];
            boolean hasSignal = false;

            for (int row = 0; row < N; row++) {
                grayLevels[row] = resizedImage.get(row, col)[0];
                if (grayLevels[row] > 0) {
                    hasSignal = true;
                }
            }

            // Ignorer les colonnes noires (sans signal)
            if (!hasSignal) {
                sampleIndex += samplesPerColumn * 2; // Ajouter des silences
                continue;
            }

            // Générer le signal pour cette colonne
            for (int sample = 0; sample < samplesPerColumn; sample++) {
                double t = sample / samplingRate;
                double s = 0;

                for (int i = 0; i < N; i++) {
                    double fi = f0 * (i + 1); // Fréquence actuelle
                    s += grayLevels[i] * Math.sin(2 * Math.PI * fi * t) * 5;
                }

                System.out.println("le S: "+s);
                // Normaliser et convertir en 16 bits PCM
                short pcmValue = (short) Math.max(-32768, Math.min(32767, s));
                audioData[sampleIndex++] = (byte) (pcmValue & 0xff);
                audioData[sampleIndex++] = (byte) ((pcmValue >> 8) & 0xff);
            }
        }

        // Écrire les données audio dans un fichier WAV
        try {
            AudioFormat format = new AudioFormat((float) samplingRate, 16, 1, true, false);
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioInputStream = new AudioInputStream(bais, format, audioData.length / 2);
            File outputFile = new File("output.wav");
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
            System.out.println("Fichier audio créé : output.wav");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
