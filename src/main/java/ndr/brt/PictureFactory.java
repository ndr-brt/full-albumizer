package ndr.brt;

import io.humble.video.MediaPicture;
import io.humble.video.PixelFormat;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PictureFactory {

    static MediaPicture picture(File image, PixelFormat.Type pixelFormat) throws IOException {
        final BufferedImage bufferedImage = ImageIO.read(image);
        final MediaPicture picture = MediaPicture.make(
                bufferedImage.getWidth(),
                bufferedImage.getHeight(),
                pixelFormat);
        MediaPictureConverter converter = MediaPictureConverterFactory.createConverter(bufferedImage, picture);
        converter.toPicture(picture, bufferedImage, 0);
        return picture;
    }
}
