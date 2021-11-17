package ndr.brt;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.delete;
import static ndr.brt.VideoMaker.videoMaker;

public class FullAlbumizer {

    private final static Logger log = LoggerFactory.getLogger(FullAlbumizer.class);

    public static void main(String[] args) throws Exception {
        var parser = new DefaultParser();
        var commandLine = parser.parse(new Options(), args);

//        albumize(commandLine.getArgs()[0]);
        albumize("/home/andrea/Music/C/Chubby and the Gang - 2021 - The Mutt's Nuts");
    }

    private static void albumize(String path) throws NoSuchFileException {

        Path folder = Paths.get(path);
        if (!Files.exists(folder)) {
            throw new NoSuchFileException("Path '" + folder + "' does not exists");
        }

        var audioConcatenator = new AudioConcatenator(folder);
        var videoMaker = videoMaker(folder);

        try {
            var audioOutput = audioConcatenator.concatenate();

            videoMaker.attach(audioOutput);

        } catch (Exception e) {
            log.error("Exception", e);
            deleteQuietly(videoMaker.getOutput());
        } finally {
            deleteQuietly(audioConcatenator.getOutput());
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            if (file != null) delete(file);
        } catch (Exception e) {
            log.error("File " + file + " not exists", e);
        }
    }

}
