import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Predicate;

import static io.humble.video.Codec.ID.CODEC_ID_MPEG2VIDEO;
import static io.humble.video.PixelFormat.Type.PIX_FMT_YUV420P;

public class FullAlbumizer {

    private static final Predicate<File> JPEG_FILE = file -> file.getName().endsWith(".jpg");

    /*
     good example:
     https://github.com/artclarke/humble-video/blob/master/humble-video-demos/src/main/java/io/humble/video/demos/RecordAndEncodeVideo.java
      */

    public static void main(String[] args) throws IOException, InterruptedException {

        String folderPath = "/home/andrea/Music/Rituals, The - 2009 - Celebrate Life";

        File folder = new File(folderPath);
        File album = new File(folder, "album.mpeg");

        File image = Arrays.stream(folder.listFiles())
                .filter(JPEG_FILE).findFirst().get();

        final Muxer muxer = Muxer.make(album.getAbsolutePath(), null, null);
        final Rational framerate = Rational.make(1, 1);
        final Codec codec = Codec.findEncodingCodec(CODEC_ID_MPEG2VIDEO);
        final BufferedImage bufferedImage = ImageIO.read(image);

        PixelFormat.Type pixelFormat = PIX_FMT_YUV420P;
        Encoder encoder = Encoder.make(codec);
        encoder.setWidth(bufferedImage.getWidth());
        encoder.setHeight(bufferedImage.getHeight());
        encoder.setPixelFormat(pixelFormat);
        encoder.setTimeBase(framerate);

        encoder.open(null, null);

        muxer.addNewStream(encoder);

        muxer.open(null, null);

        final MediaPicture picture = MediaPicture.make(
                encoder.getWidth(),
                encoder.getHeight(),
                pixelFormat);
        picture.setTimeBase(framerate);

        MediaPictureConverter converter = MediaPictureConverterFactory.createConverter(bufferedImage, picture);

        final MediaPacket packet = MediaPacket.make();
        for (int i = 0; i < 100 / framerate.getDouble(); i++) {

            converter.toPicture(picture, bufferedImage, i);

            do {
                encoder.encode(packet, picture);
                if (packet.isComplete())
                    muxer.write(packet, false);
            } while (packet.isComplete());
        }

        do {
            encoder.encode(packet, null);
            if (packet.isComplete())
                muxer.write(packet,  false);
        } while (packet.isComplete());

        muxer.close();

    }

}
