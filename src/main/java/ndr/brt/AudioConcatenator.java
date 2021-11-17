package ndr.brt;

import com.github.kokorin.jaffree.LogLevel;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.OutputListener;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import me.tongfei.progressbar.ProgressBar;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.UnaryOperator;

import static java.nio.file.Files.delete;
import static java.util.stream.Collectors.toList;
import static ndr.brt.FileType.audio;
import static ndr.brt.GetSize.getSize;

class AudioConcatenator {

    private static final UnaryOperator<String> escapeQuotes = p -> p.replace("\'", "\'\\\'\'");
    private static final UnaryOperator<String> prepareRow = p -> "file '".concat(p).concat("'");

    private final Path folder;
    private final Path audioOutput;

    public AudioConcatenator(Path folder) {
        this.folder = folder;
        this.audioOutput = folder.resolve("audioOutput.wav");
    }

    Path concatenate() {
        try {
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
            Files.write(songsFile, songs, StandardCharsets.UTF_8);

            try (ProgressBar progress = new ProgressBar("Audio concatenation", totalSize)) {
                UrlInput input = UrlInput.fromPath(songsFile)
                        .addArguments("-f", "concat")
                        .addArguments("-safe", "0");

                FFmpeg.atPath()
                        .setLogLevel(LogLevel.ERROR)
                        .addInput(input)
                        .addArguments("-c", "copy")
                        .addOutput(UrlOutput.toPath(audioOutput))
                        .setProgressListener(p -> progress.stepTo(p.getSize()))
                        .setOverwriteOutput(true)
                        .execute();

                progress.stepTo(totalSize);
            }

            delete(songsFile);

            return getOutput();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Path getOutput() {
        return audioOutput;
    }
}
