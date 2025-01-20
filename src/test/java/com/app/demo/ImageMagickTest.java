package com.app.demo;


import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class ImageMagickTest{
    public static ImageMagick imageMagick = new ImageMagick();

    @Test
    void imageMagick_is_installed(){
        assertThat(imageMagick.detectVersion()).isNotEqualTo(ImageMagick.Version.NA);
    }

    @Test
    @EnabledIfImageMagickIsInstalled
    void thumbnail_creation_works(@TempDir Path testDir) throws IOException {
        Path originalImage = copyTestImageTo(testDir.resolve("large.jpg"));
        Path thumbnail = testDir.resolve("thumbnail.jpg");
        imageMagick.createThumbnail(originalImage, thumbnail);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(thumbnail).exists();
        softly.assertThat(Files.size(thumbnail)).isLessThan(Files.size(originalImage)/2);
        softly.assertThat(getDimensions(thumbnail).width()).isEqualTo(300);

        softly.assertAll();
    }
    private Dimensions getDimensions(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            BufferedImage read = ImageIO.read(is);
            return new Dimensions(read.getWidth(), read.getHeight());
        } catch (IOException e) {
            return new Dimensions(-1, -1);
        }
    }

    public record Dimensions(int width, int height){}
    private static Path copyTestImageTo(Path targetFile) {
        try (InputStream resourceAsStream = ImageMagickTest.class.getResourceAsStream("/image-car.jpg")) {
            assert resourceAsStream != null;
            Files.copy(resourceAsStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied test image to: = " + targetFile.toAbsolutePath());
            return targetFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
