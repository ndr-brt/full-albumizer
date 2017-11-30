package ndr.brt;

import me.tongfei.progressbar.ProgressBar;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import static java.math.BigInteger.valueOf;
import static java.nio.file.Files.probeContentType;

public class VideoMaker {

    private final FFmpegExecutor executor;
    private final GetDuration getDuration;
    private Path audio;
    private Path images;

    public static VideoMaker videoMaker(FFmpegExecutor executor, GetDuration getDuration) {
        return new VideoMaker(executor, getDuration);
    }

    public VideoMaker(FFmpegExecutor executor, GetDuration getDuration) {
        this.executor = executor;
        this.getDuration = getDuration;
    }

    public VideoMaker audio(Path audio) {
        this.audio = audio;
        return this;
    }

    public VideoMaker images(Path images) {
        this.images = images;
        return this;
    }

    public void make(Path output) {
        try {
            Path image = Files.walk(images)
                    .filter(imageFiles)
                    .map(Path::toAbsolutePath)
                    .sorted()
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Folder does not contains any image file"));

            FFmpegBuilder audioVideo = new FFmpegBuilder()
                    .addExtraArgs("-loop", "1")
                    .addExtraArgs("-framerate", "2")
                    .addInput(image.toString())
                    .addInput(audio.toString())
                    .addOutput(output.toString())
                    .addExtraArgs("-c:v", "libx264")
                    .addExtraArgs("-preset", "medium")
                    .addExtraArgs("-tune", "stillimage")
                    .addExtraArgs("-crf", "18")
                    .addExtraArgs("-c:a", "copy")
                    .addExtraArgs("-shortest")
                    .addExtraArgs("-pix_fmt", "yuv420p")
                    .addExtraArgs("-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2")
                    .done();


            final Double duration = getDuration.apply(audio);

            ProgressBar progress = new ProgressBar("Video making", duration.longValue());

            progress.start();
            executor.createJob(audioVideo, p -> progress.stepTo(toSeconds(p.out_time_ns))).run();
            progress.stop();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private long toSeconds(long out_time_ns) {
        return valueOf(out_time_ns)
                .divide(valueOf(1000000000))
                .longValue();
    }

    private static final Predicate<? super Path> imageFiles = path -> {
        try {
            return probeContentType(path).startsWith("image");
        } catch (IOException e) {
            return false;
        }
    };
}
