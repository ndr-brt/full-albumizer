package ndr.brt;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.Stream;
import me.tongfei.progressbar.ProgressBar;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static com.github.kokorin.jaffree.ffprobe.UrlInput.fromPath;

public interface Video {
    static Path render(Path coverImage, Path audioOutput) {
        var output = audioOutput.getParent().resolve("album.mkv");

        var duration = FFprobe.atPath()
                .setShowStreams(true)
                .setInput(fromPath(audioOutput))
                .execute().getStreams().stream()
                .findFirst().map(Stream::getDuration).get();

        try (ProgressBar progress = new ProgressBar("Video making", duration.longValue())) {
            var input = UrlInput.fromPath(coverImage)
                    .addArguments("-loop", "1")
                    .addArguments("-framerate", "2");

            FFmpeg.atPath()
                    .addInput(input)
                    .addInput(UrlInput.fromPath(audioOutput))
                    .addArguments("-c:v", "libx264")
                    .addArguments("-preset", "medium")
                    .addArguments("-tune", "stillimage")
                    .addArguments("-crf", "18")
                    .addArguments("-c:a", "copy")
                    .addArgument("-shortest")
                    .addArguments("-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2")
                    .addOutput(UrlOutput.toPath(output))
                    .setProgressListener(p -> progress.stepTo(p.getTime(TimeUnit.SECONDS)))
                    .setOverwriteOutput(true)
                    .execute();
        }

        return output;
    }
}
