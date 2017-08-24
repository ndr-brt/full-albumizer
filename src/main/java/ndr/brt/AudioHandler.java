package ndr.brt;

import io.humble.video.*;

import java.io.File;

import static io.humble.video.Codec.findEncodingCodec;

public class AudioHandler {

    private final Demuxer audioMuxer;
    private final MediaAudio samples;
    private final Decoder decoder;
    private final Encoder encoder;

    public AudioHandler(File file) {
        audioMuxer = Demuxer.make();
        try {
            audioMuxer.open(file.getAbsolutePath(), null, false, true, null, null);
            decoder = audioMuxer.getStream(0).getDecoder();
            encoder = Encoder.make(findEncodingCodec(decoder.getCodecID()));
            encoder.setSampleRate(decoder.getSampleRate());
            encoder.setChannelLayout(decoder.getChannelLayout());
            encoder.setSampleFormat(decoder.getSampleFormat());
            samples = MediaAudio.make(
                    decoder.getFrameSize(),
                    decoder.getSampleRate(),
                    decoder.getChannels(),
                    decoder.getChannelLayout(),
                    decoder.getSampleFormat()
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean thereIsDataToReadTo(MediaPacket packet) {
        try {
            return audioMuxer.read(packet) >= 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void open() {
        decoder.open(null, null);
        encoder.open(null, null);
    }

    public Coder encoder() {
        return encoder;
    }

    public int decode(MediaPacket packet, int offset) {
        return decoder.decode(samples, packet, offset);
    }

    public void encode(MediaPacket packet) {
        encoder.encodeAudio(packet, samples);
    }
}
