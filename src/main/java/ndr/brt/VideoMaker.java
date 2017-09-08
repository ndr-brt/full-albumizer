package ndr.brt;

import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

import static java.nio.file.Files.probeContentType;

public class VideoMaker {
    private final FFmpegExecutor executor;
    private Path audio;
    private Path images;

    public static VideoMaker videoMaker(FFmpegExecutor executor) {
        return new VideoMaker(executor);
    }

    public VideoMaker(FFmpegExecutor executor) {
        this.executor = executor;
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
                    .findFirst()
                    .get();


            FFmpegBuilder audioVideo = new FFmpegBuilder()
                    .addInput(audio.toString())
                    .addInput(image.toString())
                    .addOutput(output.toString())
                    .addExtraArgs("-vcodec", "mjpeg")
                    .addExtraArgs("-acodec", "copy")
                    .done();

            executor.createJob(audioVideo).run();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static final Predicate<? super Path> imageFiles = path -> {
        try {
            return probeContentType(path).startsWith("image");
        } catch (IOException e) {
            return false;
        }
    };
}
