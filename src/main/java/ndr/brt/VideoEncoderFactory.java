package ndr.brt;

import io.humble.video.*;

import static io.humble.video.Codec.ID.CODEC_ID_MPEG2VIDEO;
import static io.humble.video.Codec.findEncodingCodec;

public class VideoEncoderFactory {

    static Encoder videoEncoder(PixelFormat.Type pixelFormat, MediaPicture picture) {
        Encoder videoEncoder = Encoder.make(findEncodingCodec(CODEC_ID_MPEG2VIDEO));
        videoEncoder.setWidth(picture.getWidth());
        videoEncoder.setHeight(picture.getHeight());
        videoEncoder.setPixelFormat(pixelFormat);
        videoEncoder.setTimeBase(Rational.make(1, 1));
        return videoEncoder;
    }

}
