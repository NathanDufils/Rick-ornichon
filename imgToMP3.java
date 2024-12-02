import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class imgToMP3 {

    static {
        // Load the OpenCV native library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        // Path to the input image
        String inputPath = "C:\\Users\\tombe\\OneDrive\\Documents\\Cours\\S5\\Multimedia\\Rick-ornichon\\Pictures\\rick-ornichon.png"; // Replace with your image path
        String outputPath = "C:\\Users\\tombe\\OneDrive\\Documents\\Cours\\S5\\Multimedia\\Rick-ornichon\\Pictures\\pre-traitement.png"; // Path to save the processed image

        // Read the input image
        Mat image = Imgcodecs.imread(inputPath);
        if (image.empty()) {
            System.err.println("Failed to load image!");
            return;
        }

        // Convert the image to grayscale
        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(image, grayscaleImage, Imgproc.COLOR_BGR2GRAY);

        // Resize the image to 64x64
        Mat resizedImage = new Mat();
        Imgproc.resize(grayscaleImage, resizedImage, new Size(64, 64));

        // Reduce intensity to 16 levels of gray (4 bits)
        Mat reducedGrayscaleImage = new Mat(resizedImage.size(), resizedImage.type());
        resizedImage.convertTo(reducedGrayscaleImage, CvType.CV_8U, 1.0 / 16);
        reducedGrayscaleImage.convertTo(reducedGrayscaleImage, CvType.CV_8U, 16);

        // Save the processed image
        Imgcodecs.imwrite(outputPath, reducedGrayscaleImage);

        System.out.println("Processing complete. Image saved at " + outputPath);
    }
}