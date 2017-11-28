package ndr.brt;

import net.bramp.ffmpeg.FFprobe;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;

public interface GetDuration extends Function<Path, Double> {

    static GetDuration getDuration(final FFprobe probe) {
        return path -> {
            try {
                return probe.probe(path.toString()).format.duration;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

}
