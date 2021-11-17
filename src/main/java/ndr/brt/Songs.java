package ndr.brt;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.UnaryOperator;

import static java.util.stream.Collectors.toList;
import static ndr.brt.GetSize.getSize;

public interface Songs {

    UnaryOperator<String> escapeQuotes = p -> p.replace("\'", "\'\\\'\'");
    UnaryOperator<String> prepareRow = p -> "file '".concat(p).concat("'");

    static Path createFile(List<Path> files) {
        try {
            var folder = files.stream().findFirst().map(Path::getParent).orElseThrow();

            var songs = files.stream()
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .sorted()
                    .map(escapeQuotes)
                    .map(prepareRow)
                    .collect(toList());

            var songsFile = folder.resolve("songs");
            Files.write(songsFile, songs, StandardCharsets.UTF_8);

            return songsFile;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
