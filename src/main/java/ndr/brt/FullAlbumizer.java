package ndr.brt;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import static java.nio.file.Files.probeContentType;
import static java.util.Arrays.asList;
import static ndr.brt.AudioConcatenator.audioConcatenator;

public class FullAlbumizer {

    private static final Predicate<? super Path> imageFiles = path -> {
        try {
            return probeContentType(path).startsWith("image");
        } catch (IOException e) {
            return false;
        }
    };

    public static void main(String[] args) throws IOException {
        FFmpeg ffmpeg = new FFmpeg(sh("which ffmpeg"));
        FFprobe ffprobe = new FFprobe(sh("which ffprobe"));

        Path folder = Paths.get("/home/andrea/Music/Rituals, The - 2009 - Celebrate Life/");
        Path videoOutput = folder.resolve("videoOutput.mp4");

        Path image = Files.walk(folder)
                .filter(imageFiles)
                .map(Path::toAbsolutePath)
                .findFirst()
                .get();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

        Path audioOutput = audioConcatenator(executor).folder(folder).concatenate();

        FFmpegBuilder audioVideo = new FFmpegBuilder()
                .addInput(audioOutput.toString())
                .addInput(image.toString())
                .addOutput(videoOutput.toString())
                .addExtraArgs("-vcodec", "mjpeg")
                .addExtraArgs("-acodec", "copy")
                .done();

        executor.createJob(audioVideo).run();
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
