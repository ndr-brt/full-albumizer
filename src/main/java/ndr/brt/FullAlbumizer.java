package ndr.brt;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

import static java.nio.file.Files.delete;
import static java.util.stream.Collectors.toList;
import static ndr.brt.FileType.audio;
import static ndr.brt.FileType.image;

public class FullAlbumizer {

    private final static Logger log = LoggerFactory.getLogger(FullAlbumizer.class);

    public static void main(String[] args) throws Exception {
        var parser = new DefaultParser();
        var commandLine = parser.parse(new Options(), args);

        Path folder = Paths.get(commandLine.getArgs()[0]);
        if (!Files.exists(folder)) {
            throw new NoSuchFileException("Path '" + folder + "' does not exists");
        }

        new FullAlbumizer().createVideo(folder);
    }

    private void createVideo(Path folder) throws IOException {
        var audioFiles = Files.walk(folder).filter(audio).collect(toList());

        var audioOutput = Audio.concat(audioFiles);

        var coverImage = Files.walk(folder)
                .filter(image)
                .map(Path::toAbsolutePath)
                .sorted()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Folder does not contains any image file"));

        var videoOutput = Video.render(coverImage, audioOutput);

        log.info("Video rendered at " + videoOutput);
        deleteQuietly(audioOutput);
    }

    private void deleteQuietly(Path file) {
        try {
            if (file != null) delete(file);
        } catch (Exception e) {
            log.error("File " + file + " not exists", e);
        }
    }

}
