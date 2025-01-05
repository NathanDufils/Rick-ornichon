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
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        String imagePath = "Pictures/shapes.png";
        Mat image = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_GRAYSCALE);

        Mat resizedImage = new Mat();
        Imgproc.resize(image, resizedImage, new Size(64, 64));

        int N = 64;
        double f0 = 2000.0;
        double samplingRate = 44100;
        double durationPerColumn = 1.0 / N;
        int samplesPerColumn = (int) (samplingRate * durationPerColumn);

        byte[] audioData = new byte[N * samplesPerColumn * 2]; // Signal PCM 16 bits
        int sampleIndex = 0;

        for (int col = 0; col < N; col++) {
            double[] grayLevels = new double[N];
            boolean hasSignal = false;

            for (int row = 0; row < N; row++) {
                grayLevels[row] = resizedImage.get(row, col)[0];
                if (grayLevels[row] > 0) {
                    hasSignal = true;
                }
            }

            if (!hasSignal) {
                sampleIndex += samplesPerColumn * 2; // Ajouter des silences
                continue;
            }

            for (int sample = 0; sample < samplesPerColumn; sample++) {
                double t = sample / samplingRate;
                double s = 0;

                for (int i = 0; i < N; i++) {
                    double fi = f0 * (i + 1);
                    s += grayLevels[i] * Math.sin(2 * Math.PI * fi * t) * 5;
                }

                short pcmValue = (short) Math.max(-32768, Math.min(32767, s));
                audioData[sampleIndex++] = (byte) (pcmValue & 0xff);
                audioData[sampleIndex++] = (byte) ((pcmValue >> 8) & 0xff);
            }
        }

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
