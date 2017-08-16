import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static io.humble.video.Codec.ID.CODEC_ID_MPEG2VIDEO;
import static io.humble.video.PixelFormat.Type.PIX_FMT_YUV420P;

public class FullAlbumizer {

    /*
     good example:
     https://github.com/artclarke/humble-video/blob/master/humble-video-demos/src/main/java/io/humble/video/demos/RecordAndEncodeVideo.java
      */

    public static void main(String[] args) throws IOException, InterruptedException {
        final Muxer muxer = Muxer.make("/tmp/tmpmovie.mpeg", null, null);
        final Rational framerate = Rational.make(1, 1);
        final Codec codec = Codec.findEncodingCodec(CODEC_ID_MPEG2VIDEO);

        PixelFormat.Type pixelFormat = PIX_FMT_YUV420P;
        Encoder encoder = Encoder.make(codec);
        encoder.setWidth(787);
        encoder.setHeight(787);
        encoder.setPixelFormat(pixelFormat);
        encoder.setTimeBase(framerate);

        encoder.open(null, null);

        muxer.addNewStream(encoder);

        muxer.open(null, null);

        MediaPictureConverter converter = null;
        final MediaPicture picture = MediaPicture.make(
                encoder.getWidth(),
                encoder.getHeight(),
                pixelFormat);
        picture.setTimeBase(framerate);

        final MediaPacket packet = MediaPacket.make();
        for (int i = 0; i < 100 / framerate.getDouble(); i++) {
            final BufferedImage image = ImageIO.read(new File("./src/main/resources/slack.jpg"));

            converter = MediaPictureConverterFactory.createConverter(image, picture);

            converter.toPicture(picture, image, i);

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
