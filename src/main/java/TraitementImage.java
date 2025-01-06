package main.java;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;

public class TraitementImage {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public void transformerEtGenererAudio(String imagePath, double f0) {
        try {
            Mat image = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_GRAYSCALE);

            if (image.empty()) {
                return;
            }

            Mat resizedImage = new Mat();
            Imgproc.resize(image, resizedImage, new Size(64, 64));

            byte[] audioData = genererAudioDepuisImage(resizedImage, f0);

            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioInputStream = new AudioInputStream(bais, format, audioData.length / 2);
            File outputFile = new File("src/main/resources/audio/output.wav");
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] genererAudioDepuisImage(Mat image, double f0) {
        int n = 64;
        double samplingRate = 44100.0;
        double durationPerColumn = 1.0 / n;
        int samplesPerColumn = (int) (samplingRate * durationPerColumn);

        byte[] audioData = new byte[n * samplesPerColumn * 2];
        int sampleIndex = 0;

        // Définir une plage fixe de fréquences
        double minFrequency = f0 / 2;  // Par exemple, moitié de f0 pour le grave
        double maxFrequency = f0 * 2; // Par exemple, double de f0 pour l'aigu
        double[] frequencies = new double[n];

        // Calcul des fréquences selon une progression logarithmique
        for (int i = 0; i < n; i++) {
            frequencies[i] = minFrequency * Math.pow(maxFrequency / minFrequency, (double) i / (n - 1));
        }

        // Normalisation des niveaux de gris
        double maxGrayLevel = 255.0; // Les niveaux de gris vont de 0 à 255

        for (int col = 0; col < n; col++) {
            double[] grayLevels = new double[n];
            for (int row = 0; row < n; row++) {
                grayLevels[row] = image.get(row, col)[0] / maxGrayLevel; // Normalisation
            }

            for (int sample = 0; sample < samplesPerColumn; sample++) {
                double t = sample / samplingRate;
                double s = 0;

                // Génération du son avec pondération
                for (int i = 0; i < n; i++) {
                    s += grayLevels[i] * Math.sin(2 * Math.PI * frequencies[i] * t);
                }

                // Appliquer une amplitude globale réduite pour éviter la saturation
                s *= 5000;

                // Limiter les valeurs PCM
                short pcmValue = (short) Math.clamp(s, Short.MIN_VALUE, Short.MAX_VALUE);
                audioData[sampleIndex++] = (byte) (pcmValue & 0xff);
                audioData[sampleIndex++] = (byte) ((pcmValue >> 8) & 0xff);
            }
        }

        return audioData;
    }
}