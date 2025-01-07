package main.java;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.io.File;

public class VideoFrameExtractor {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Load OpenCV library
    }

    public static void main(String[] args) {
        String videoPath = "src/main/resources/videos/animation_noir_blanc.mp4"; // Path to the input video
        String outputFolder = "src/main/resources/images/black_white_frames"; // Path to save the extracted frames

        // Create output directory if it doesn't exist
        File folder = new File(outputFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        VideoCapture videoCapture = new VideoCapture(videoPath);
        if (!videoCapture.isOpened()) {
            System.out.println("Error: Unable to open video file.");
            return;
        }

        Mat frame = new Mat();
        int frameCount = 0;

        System.out.println("Extracting frames...");
        while (videoCapture.read(frame)) {
            String outputFilePath = outputFolder + File.separator + "frame_" + String.format("%04d", frameCount) + ".png";
            Imgcodecs.imwrite(outputFilePath, frame);
            System.out.println("Saved: " + outputFilePath);
            frameCount++;
        }

        videoCapture.release();
        System.out.println("Extraction complete. Total frames extracted: " + frameCount);
    }
}