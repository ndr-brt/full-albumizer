package ndr.brt;

import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.probeContentType;
import static java.util.stream.Collectors.toList;

public class AudioConcatenator {

    private static final Function<String, String> escapeQuotes = p -> p.replace("\'", "\'\\\'\'");
    private static final Function<String, String> prepareRow = p -> "file '".concat(p).concat("'");

    private final FFmpegExecutor executor;
    private Path folder;

    public AudioConcatenator(FFmpegExecutor executor) {
        this.executor = executor;
    }

    public static AudioConcatenator audioConcatenator(FFmpegExecutor executor) {
        return new AudioConcatenator(executor);
    }

    public AudioConcatenator folder(Path folder) {
        this.folder = folder;
        return this;
    }

    public Path concatenate() {
        try {
            Path songsFile = folder.resolve("songs");
            Path audioOutput = folder.resolve("audioOutput.mp3");

            List<String> songs = Files.walk(folder)
                    .filter(audioFiles)
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .sorted()
                    .map(escapeQuotes)
                    .map(prepareRow)
                    .collect(toList());

            Files.write(songsFile, songs);

            FFmpegBuilder concatAudio = new FFmpegBuilder()
                    .addExtraArgs("-loglevel", "panic")
                    .addExtraArgs("-f", "concat")
                    .addExtraArgs("-safe", "0")
                    .addInput(songsFile.toString())
                    .addOutput(audioOutput.toString())
                    .addExtraArgs("-c", "copy")
                    .done();

            executor.createJob(concatAudio).run();

            delete(songsFile);

            return audioOutput;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Predicate<? super Path> audioFiles = path -> {
        try {
            return probeContentType(path).startsWith("audio");
        } catch (IOException e) {
            return false;
        }
    };

}
