package ndr.brt;

import me.tongfei.progressbar.ProgressBar;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.probeContentType;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static ndr.brt.FileType.audio;
import static ndr.brt.GetSize.getSize;

class AudioConcatenator {

    private static final Function<String, String> escapeQuotes = p -> p.replace("\'", "\'\\\'\'");
    private static final Function<String, String> prepareRow = p -> "file '".concat(p).concat("'");

    private final FFmpegExecutor executor;
    private Path folder;

    static AudioConcatenator audioConcatenator(FFmpegExecutor executor) {
        return new AudioConcatenator(executor);
    }

    private AudioConcatenator(FFmpegExecutor executor) {
        this.executor = executor;
    }

    AudioConcatenator folder(Path folder) {
        this.folder = folder;
        return this;
    }

    Path concatenate() {
        try {
            Path audioOutput = folder.resolve("audioOutput.mp3");

            long totalSize = Files.walk(folder)
                    .filter(audio)
                    .mapToLong(getSize())
                    .sum();

            List<String> songs = Files.walk(folder)
                    .filter(audio)
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .sorted()
                    .map(escapeQuotes)
                    .map(prepareRow)
                    .collect(toList());

            Path songsFile = folder.resolve("songs");
            Files.write(songsFile, songs);

            FFmpegBuilder concatAudio = new FFmpegBuilder()
                    .addExtraArgs("-loglevel", "panic")
                    .addExtraArgs("-f", "concat")
                    .addExtraArgs("-safe", "0")
                    .addInput(songsFile.toString())
                    .addOutput(audioOutput.toString())
                    .addExtraArgs("-c", "copy")
                    .done();

            try (ProgressBar progress = new ProgressBar("Audio concatenation", totalSize)) {
                executor.createJob(concatAudio, p -> progress.stepTo(p.total_size)).run();
                progress.stepTo(totalSize);
            }

            delete(songsFile);

            return audioOutput;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
