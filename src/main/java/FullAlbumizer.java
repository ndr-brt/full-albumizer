import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static io.humble.video.Codec.ID.CODEC_ID_MPEG2VIDEO;
import static io.humble.video.PixelFormat.Type.PIX_FMT_YUV420P;
import static java.util.stream.Collectors.toList;

public class FullAlbumizer {

    private static final Predicate<File> JPEG_FILE = file -> file.getName().endsWith(".jpg");
    private static final Predicate<File> MP3_FILE = file -> file.getName().endsWith(".mp3");

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
        final BufferedImage bufferedImage = ImageIO.read(image);

        PixelFormat.Type pixelFormat = PIX_FMT_YUV420P;
        final Codec codec = Codec.findEncodingCodec(CODEC_ID_MPEG2VIDEO);
        Encoder encoder = Encoder.make(codec);
        encoder.setWidth(bufferedImage.getWidth());
        encoder.setHeight(bufferedImage.getHeight());
        encoder.setPixelFormat(pixelFormat);
        encoder.setTimeBase(framerate);

        encoder.open(null, null);


        final MediaPicture picture = MediaPicture.make(
                encoder.getWidth(),
                encoder.getHeight(),
                pixelFormat);
        picture.setTimeBase(framerate);

        MediaPictureConverter converter = MediaPictureConverterFactory.createConverter(bufferedImage, picture);

        List<File> songs = Arrays.stream(folder.listFiles())
                .filter(MP3_FILE).collect(toList());

        Demuxer audioMuxer = Demuxer.make();
        audioMuxer.open(songs.get(0).getAbsolutePath(), null, false, true, null, null);
        DemuxerStream stream = audioMuxer.getStream(0);
        Decoder audioDecoder = stream.getDecoder();
        Codec audioCodec = Codec.findEncodingCodec(audioDecoder.getCodecID());
        Encoder aEncoder = Encoder.make(audioCodec);
        aEncoder.setSampleRate(audioDecoder.getSampleRate());
        aEncoder.setChannelLayout(audioDecoder.getChannelLayout());
        aEncoder.setSampleFormat(audioDecoder.getSampleFormat());

        audioDecoder.open(null, null);

        MediaAudio samples = MediaAudio.make(
                audioDecoder.getFrameSize(),
                audioDecoder.getSampleRate(),
                audioDecoder.getChannels(),
                audioDecoder.getChannelLayout(),
                audioDecoder.getSampleFormat()
        );

        aEncoder.open(null, null);

        muxer.addNewStream(encoder);
        muxer.addNewStream(aEncoder);

        muxer.open(null, null);

        final MediaPacket audioPacket = MediaPacket.make();
        final MediaPacket videoPacket = MediaPacket.make();
        converter.toPicture(picture, bufferedImage, 0);
        encoder.encode(videoPacket, picture);
        while(audioMuxer.read(audioPacket) >= 0) {
            int offset = 0;
            int bytesRead = 0;
            do {
                bytesRead += audioDecoder.decode(samples, audioPacket, offset);

                aEncoder.encodeAudio(audioPacket, samples);

                offset += bytesRead;

                if (audioPacket.isComplete())
                    muxer.write(audioPacket, false);

            } while (offset < audioPacket.getSize());
        }

        do {
            encoder.encodeVideo(audioPacket, null);
            aEncoder.encodeAudio(videoPacket, null);
            if (audioPacket.isComplete())
                muxer.write(audioPacket,  false);
        } while (audioPacket.isComplete());

        muxer.close();

    }

}
