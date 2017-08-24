package ndr.brt;

import io.humble.video.*;

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

        PixelFormat.Type pixelFormat = PIX_FMT_YUV420P;

        final MediaPicture picture = PictureFactory.picture(image, pixelFormat);

        final Codec codec = Codec.findEncodingCodec(CODEC_ID_MPEG2VIDEO);
        Encoder videoEncoder = Encoder.make(codec);
        videoEncoder.setWidth(picture.getWidth());
        videoEncoder.setHeight(picture.getHeight());
        videoEncoder.setPixelFormat(pixelFormat);
        videoEncoder.setTimeBase(Rational.make(1, 1));

        videoEncoder.open(null, null);

        List<File> songs = Arrays.stream(folder.listFiles())
                .filter(MP3_FILE).collect(toList());

        AudioHandler audio = new AudioHandler(songs.get(0));
        audio.open();

        File album = new File(folder, "album.mpeg");
        final Muxer muxer = Muxer.make(album.getAbsolutePath(), null, null);
        muxer.addNewStream(videoEncoder);
        muxer.addNewStream(audio.encoder());

        muxer.open(null, null);

        final MediaPacket packet = MediaPacket.make();
        videoEncoder.encodeVideo(packet, picture);
        while(audio.thereIsDataToReadTo(packet)) {
            int offset = 0;
            do {
                offset += audio.decode(packet, offset);
                audio.encode(packet);

                if (packet.isComplete())
                    muxer.write(packet, false);

            } while (offset < packet.getSize());
        }

        do {
            videoEncoder.encodeVideo(packet, null);
            if (packet.isComplete())
                muxer.write(packet, false);
        } while (packet.isComplete());

        muxer.close();

    }

}
