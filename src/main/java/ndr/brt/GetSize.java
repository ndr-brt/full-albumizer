package ndr.brt;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.function.ToLongFunction;

public interface GetSize extends ToLongFunction<Path> {

    static GetSize getSize() {
        return it -> {
            try (final FileChannel channel = FileChannel.open(it)) {
                return channel.size();
            }
            catch (Exception e) {
                return 0;
            }
        };
    }
}
