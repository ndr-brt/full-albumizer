package ndr.brt;

import com.github.kokorin.jaffree.LogLevel;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import me.tongfei.progressbar.ProgressBar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.Files.delete;
import static ndr.brt.GetSize.getSize;

public interface Audio {

    static Path concat(List<Path> files) {
        try {
            var folder = files.stream().findFirst().map(Path::getParent).orElseThrow();
            var songsFile = Songs.createFile(files);
            var output = folder.resolve("audioOutput.wav");

            var totalSize = files.stream().mapToLong(getSize()).sum();
            try (ProgressBar progress = new ProgressBar("Audio join", totalSize)) {
                var input = UrlInput.fromPath(songsFile)
                        .addArguments("-f", "concat")
                        .addArguments("-safe", "0");

                FFmpeg.atPath()
                        .setLogLevel(LogLevel.ERROR)
                        .addInput(input)
                        .addArguments("-c", "copy")
                        .addOutput(UrlOutput.toPath(output))
                        .setProgressListener(p -> progress.stepTo(p.getSize()))
                        .setOverwriteOutput(true)
                        .execute();

                progress.stepTo(totalSize);
            }

            delete(songsFile);

            return output;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
