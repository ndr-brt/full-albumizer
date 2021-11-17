package ndr.brt;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.Stream;
import me.tongfei.progressbar.ProgressBar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static com.github.kokorin.jaffree.ffprobe.UrlInput.fromPath;
import static java.math.BigInteger.valueOf;

public class VideoMaker {

    private final Path videoOutput;
    private final Path images;

    public static VideoMaker videoMaker(Path folder) {
        return new VideoMaker(folder);
    }

    public VideoMaker(Path folder) {
        this.images = folder;
        this.videoOutput = folder.resolve("album.mkv");
    }

    public void attach(Path audio) {
        try {
            Path image = Files.walk(images)
                    .filter(FileType.image)
                    .map(Path::toAbsolutePath)
                    .sorted()
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Folder does not contains any image file"));

            Float duration = FFprobe.atPath()
                    .setShowStreams(true)
                    .setInput(fromPath(audio))
                    .execute().getStreams().stream()
                    .findFirst().map(Stream::getDuration).get();

            try (ProgressBar progress = new ProgressBar("Video making", duration.longValue())) {
                UrlInput input = UrlInput.fromPath(image)
                        .addArguments("-loop", "1")
                        .addArguments("-framerate", "2");

                FFmpeg.atPath()
                        .addInput(input)
                        .addInput(UrlInput.fromPath(audio))
                        .addArguments("-c:v", "libx264")
                        .addArguments("-preset", "medium")
                        .addArguments("-tune", "stillimage")
                        .addArguments("-crf", "18")
                        .addArguments("-c:a", "copy")
                        .addArgument("-shortest")
                        .addArguments("-pix_fmt", "yuv420p")
                        .addArguments("-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2")
                        .addOutput(UrlOutput.toPath(videoOutput))
                        .setProgressListener(p -> progress.stepTo(p.getTime(TimeUnit.SECONDS)))
                        .setOverwriteOutput(true)
                        .execute();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public Path getOutput() {
        return videoOutput;
    }

    private long toSeconds(long out_time_ns) {
        return valueOf(out_time_ns)
                .divide(valueOf(1000000000))
                .longValue();
    }
}
