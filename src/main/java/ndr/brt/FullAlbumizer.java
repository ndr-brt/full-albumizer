package ndr.brt;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

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

    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(new Options(), args);

        albumize(commandLine.getArgs()[0]);
    }

    private static void albumize(String path) throws IOException {
        FFmpeg ffmpeg = new FFmpeg(sh("which ffmpeg"));
        FFprobe ffprobe = new FFprobe(sh("which ffprobe"));

        Path folder = Paths.get(path);
        Path videoOutput = folder.resolve("album.mp4");

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

        Path audioOutput = audioConcatenator(executor)
                .folder(folder)
                .concatenate();

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
