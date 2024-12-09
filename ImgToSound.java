import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.sound.sampled.*;
import java.util.ArrayList;

public class ImageResizeApp {

    // Chargement de la bibliothèque OpenCV
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static byte[] generateContinuousTones(Double[] frequencies, int durationPerToneMillis, int sampleRate) {
        int totalDurationInSamples = (int) ((frequencies.length * durationPerToneMillis / 1000.0) * sampleRate);
        byte[] samples = new byte[totalDurationInSamples];

        double amplitude = 127; // Amplitude du signal (max 127 pour un signal audio 8 bits)

        for (int toneIndex = 0; toneIndex < frequencies.length; toneIndex++) {
            double frequency = frequencies[toneIndex];
            if (frequency < 20 || frequency > 20000) {
                frequency = 440; // Par défaut, utilisez un La (440 Hz) si fréquence hors limites
            }

            int toneStartSample = toneIndex * (int) ((durationPerToneMillis / 1000.0) * sampleRate);

            for (int i = 0; i < (durationPerToneMillis / 1000.0) * sampleRate; i++) {
                double time = i / (double) sampleRate;
                int sampleIndex = toneStartSample + i;

                if (sampleIndex >= samples.length) break; // Éviter les débordements
                samples[sampleIndex] += (byte) (amplitude * Math.sin(2 * Math.PI * frequency * time));
            }
        }

        // Normaliser pour éviter le clipping
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (byte) Math.max(Math.min(samples[i], amplitude), -amplitude);
        }

        return samples;
    }


    public static void playSound(byte[] soundData, int sampleRate) {
        try {
            // Créer un format audio
            AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, true); // 8 bits, mono, signed, big-endian
            DataLine.Info info = new DataLine.Info(Clip.class, format);

            // Obtenir un clip audio
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(format, soundData, 0, soundData.length);

            // Jouer le clip
            clip.start();
            clip.drain();
            clip.close();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static void playMultipleFrequencies(Double[] frequencies, int durationMs) {
        float sampleRate = 44100; // Taux d'échantillonnage audio standard (44.1 kHz)
        try {
            // Configuration du format audio
            AudioFormat audioFormat = new AudioFormat(sampleRate, 8, 1, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

            // Ouvrir une seule fois la ligne audio
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();

            // Parcourir les fréquences et générer le son
            for (double frequency : frequencies) {
                // Générer le buffer pour cette fréquence
                int bufferSize = (int) (durationMs * sampleRate / 1000);
                byte[] buffer = new byte[bufferSize];
                for (int i = 0; i < buffer.length; i++) {
                    double angle = 2.0 * Math.PI * i / (sampleRate / frequency);
                    buffer[i] = (byte) (Math.sin(angle) * 127);
                }
                // Écrire le buffer dans la ligne audio
                line.write(buffer, 0, buffer.length);
            }

            // Terminer et fermer la ligne audio
            line.drain();
            line.close();
        } catch (LineUnavailableException e) {
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
        playMultipleFrequencies(frequenceTab, durationPerToneMillis);
    }
}
