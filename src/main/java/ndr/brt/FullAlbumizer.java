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
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.nio.file.Files.probeContentType;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class FullAlbumizer {

    private static final Function<String, String> escapeQuotes = p -> p.replace("\'", "\'\\\'\'");
    private static final Function<String, String> prepareRow = p -> "file '".concat(p).concat("'");
    private static final Predicate<? super Path> audioFiles = path -> type(path, "audio");
    private static final Predicate<? super Path> imageFiles = path -> type(path, "image");

    private static boolean type(Path path, String type) {
        try {
            return probeContentType(path).startsWith(type);
        } catch (IOException e) {
            return false;
        }
    }


    public static void main(String[] args) throws IOException {
        FFmpeg ffmpeg = new FFmpeg(sh("which ffmpeg"));
        FFprobe ffprobe = new FFprobe(sh("which ffprobe"));

        Path folder = Paths.get("/home/andrea/Music/Rituals, The - 2009 - Celebrate Life/");
        Path songsFile = folder.resolve("songs");
        Path audioOutput = folder.resolve("audioOutput.mp3");
        Path videoOutput = folder.resolve("videoOutput.mp4");

        List<String> songs = Files.walk(folder)
                .filter(audioFiles)
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .sorted()
                .map(escapeQuotes)
                .map(prepareRow)
                .collect(toList());

        Path image = Files.walk(folder)
                .filter(imageFiles)
                .map(Path::toAbsolutePath)
                .findFirst()
                .get();

        Files.write(songsFile, songs);

        FFmpegBuilder concatAudio = new FFmpegBuilder()
                .addExtraArgs("-f", "concat")
                .addExtraArgs("-safe", "0")
                .addInput(songsFile.toString())
                .addOutput(audioOutput.toString())
                .addExtraArgs("-c", "copy")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

        FFmpegBuilder audioVideo = new FFmpegBuilder()
                .addInput(audioOutput.toString())
                .addInput(image.toString())
                .addExtraArgs("-vcodec", "mjpeg")
                .addOutput(videoOutput.toString())
                .done();

        executor.createJob(concatAudio).run();
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
