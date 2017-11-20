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
                    .sorted()
                    .findFirst()
                    .get();

            //ffmpeg -loop 1 -framerate 2 -i input.png -i audio.m4a -c:v libx264 -preset medium -tune stillimage -crf 18 -c:a copy -shortest -pix_fmt yuv420p output.mkv

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
