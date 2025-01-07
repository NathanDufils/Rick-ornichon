package main.java;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.io.File;

public class VideoFrameExtractor {

    public static void extractFrames(String videoFilePath, String outputDirPath) {
        VideoCapture videoCapture = new VideoCapture(videoFilePath);
        if (!videoCapture.isOpened()) {
            System.out.println("Unable to open video file: " + videoFilePath);
            return;
        }

        Mat frame = new Mat();
        int frameIndex = 0;
        while (videoCapture.read(frame)) {
            String frameFileName = String.format("%s/frame_%04d.png", outputDirPath, frameIndex++);
            Imgcodecs.imwrite(frameFileName, frame);
        }
        videoCapture.release();
    }
}