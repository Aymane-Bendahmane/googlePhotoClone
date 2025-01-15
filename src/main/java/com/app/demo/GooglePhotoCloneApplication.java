package com.app.demo;


import java.io.IOException;
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
            files.forEach(path -> {
                executorService.submit(() -> {
                    counter.incrementAndGet();
                    new ImageMagick().createThumbnail(path, thumbnailsDir.resolve(path.getFileName()));
                });

            });
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.HOURS);
        }
        long end = System.currentTimeMillis();
        System.out.println(" Converted " + counter + " images to thumbnails. took " + ((end - start) * 0.001) + " seconds");
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
