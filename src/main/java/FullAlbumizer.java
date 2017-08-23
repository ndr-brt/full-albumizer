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

        File image = Arrays.stream(folder.listFiles())
                .filter(JPEG_FILE).findFirst().get();

        final Rational framerate = Rational.make(1, 1);
        final BufferedImage bufferedImage = ImageIO.read(image);

        PixelFormat.Type pixelFormat = PIX_FMT_YUV420P;
        final Codec codec = Codec.findEncodingCodec(CODEC_ID_MPEG2VIDEO);
        Encoder videoEncoder = Encoder.make(codec);
        videoEncoder.setWidth(bufferedImage.getWidth());
        videoEncoder.setHeight(bufferedImage.getHeight());
        videoEncoder.setPixelFormat(pixelFormat);
        videoEncoder.setTimeBase(framerate);

        videoEncoder.open(null, null);


        final MediaPicture picture = MediaPicture.make(
                videoEncoder.getWidth(),
                videoEncoder.getHeight(),
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
        Encoder audioEncoder = Encoder.make(audioCodec);
        audioEncoder.setSampleRate(audioDecoder.getSampleRate());
        audioEncoder.setChannelLayout(audioDecoder.getChannelLayout());
        audioEncoder.setSampleFormat(audioDecoder.getSampleFormat());

        audioDecoder.open(null, null);

        MediaAudio samples = MediaAudio.make(
                audioDecoder.getFrameSize(),
                audioDecoder.getSampleRate(),
                audioDecoder.getChannels(),
                audioDecoder.getChannelLayout(),
                audioDecoder.getSampleFormat()
        );

        audioEncoder.open(null, null);

        File album = new File(folder, "album.mpeg");
        final Muxer muxer = Muxer.make(album.getAbsolutePath(), null, null);
        muxer.addNewStream(videoEncoder);
        muxer.addNewStream(audioEncoder);

        muxer.open(null, null);

        final MediaPacket packet = MediaPacket.make();
        converter.toPicture(picture, bufferedImage, 0);
        videoEncoder.encodeVideo(packet, picture);
        while(audioMuxer.read(packet) >= 0) {
            int offset = 0;
            int bytesRead = 0;
            do {
                bytesRead += audioDecoder.decode(samples, packet, offset);

                audioEncoder.encodeAudio(packet, samples);

                offset += bytesRead;

                if (packet.isComplete())
                    muxer.write(packet, false);

            } while (offset < packet.getSize());
        }

        do {
            videoEncoder.encodeVideo(packet, null);
            if (packet.isComplete())
                muxer.write(packet,  false);
        } while (packet.isComplete());

        muxer.close();

    }

}
