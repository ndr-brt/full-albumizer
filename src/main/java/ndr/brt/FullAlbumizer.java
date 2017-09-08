package ndr.brt;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.delete;
import static java.util.Arrays.asList;
import static ndr.brt.AudioConcatenator.audioConcatenator;
import static ndr.brt.VideoMaker.videoMaker;

public class FullAlbumizer {



    public static void main(String[] args) throws IOException {
        FFmpeg ffmpeg = new FFmpeg(sh("which ffmpeg"));
        FFprobe ffprobe = new FFprobe(sh("which ffprobe"));

        Path folder = Paths.get("/home/andrea/Music/Leatherface - 2004 - Dog Disco/");
        Path videoOutput = folder.resolve("videoOutput.mp4");

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

        Path audioOutput = audioConcatenator(executor).folder(folder).concatenate();

        videoMaker(executor)
                .audio(audioOutput)
                .images(folder)
                .make(videoOutput);

        delete(audioOutput);
    }

    private static String sh(String command) {
        try (
            InputStream stream = new ProcessBuilder().command(asList("sh", "-c", command)).start().getInputStream();
            InputStreamReader inputReader = new InputStreamReader(stream);
            BufferedReader reader = new BufferedReader(inputReader)
        ) {
            return reader.readLine();
        }
         catch (Exception e) {
            throw new RuntimeException(e);
         }
    }

}
