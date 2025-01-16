package com.app.demo;


import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class GooglePhotoCloneApplication {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".generated_thumbnails");
    static ImageMagick imageMagick = new ImageMagick();
    public static void main(String[] args) throws IOException, InterruptedException {

        Files.createDirectories(thumbnailsDir);
        //point to the current directory where the jar exists "."

        String directory = "C:\\Users\\ayman\\OneDrive\\Desktop\\GooglePhotoClone\\sample-images\\100-100-color";
        Path sourceDir = Path.of(directory);
        AtomicInteger counter = new AtomicInteger();
        long start = System.currentTimeMillis();

        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try (Stream<Path> files = Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .filter(GooglePhotoCloneApplication::isImage)) {
            files.forEach(path -> executorService.submit(() -> {

                try {
                    Path thumbnail = getThumbnailPath(path);
                    boolean success  = imageMagick.createThumbnail(path, thumbnail);
                    if (success) {
                        counter.incrementAndGet();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }));
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.HOURS);
        }
        long end = System.currentTimeMillis();
        System.out.println(" Converted " + counter + " images to thumbnails. took " + ((end - start) * 0.001) + " seconds");
    }

    private static Path getThumbnailPath(Path path) throws IOException {
        String hash = DigestUtils.sha256Hex( path.getFileName().toString());
        String subDir = hash.substring(0, 2);
        String fileName = hash.substring(2);

        Path subDirectory = thumbnailsDir.resolve(subDir);
        if (!Files.exists(subDirectory)) {
            Files.createDirectories(subDirectory);
        }
        return subDirectory.resolve(fileName+".webp");
    }

    private static boolean isImage(Path path) {
        try {
            String mimeType = Files.probeContentType(path);
            return mimeType != null && mimeType.contains("image");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
