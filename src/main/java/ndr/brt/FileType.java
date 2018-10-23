package ndr.brt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;

import static java.nio.file.Files.probeContentType;
import static java.util.Optional.ofNullable;

public enum FileType implements Predicate<Path> {
    image,
    audio;

    @Override
    public boolean test(Path path) {
        try {
            String probeContentType = probeContentType(path);
            return ofNullable(probeContentType)
                    .map(it -> it.startsWith(name()))
                    .orElse(false);
        } catch (IOException e) {
            return false;
        }
    }
}
