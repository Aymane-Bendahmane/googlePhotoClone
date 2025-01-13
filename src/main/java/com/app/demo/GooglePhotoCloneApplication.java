package com.app.demo;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class GooglePhotoCloneApplication {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".generated_thumbnails");

    public static void main(String[] args) throws IOException {

        Files.createDirectories(thumbnailsDir);
        //point to the current directory where the jar exists "."
        String directory = args.length == 1 ? args[0] : ".";
        Path sourceDir = Path.of(directory);
        AtomicInteger counter = new AtomicInteger();
        long start = System.currentTimeMillis();
        try (Stream<Path> files = Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .filter(GooglePhotoCloneApplication::isImage)) {
            files.forEach(path -> {
                counter.incrementAndGet();
                createThumbnail(path, thumbnailsDir.resolve(path.getFileName()));
            });
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

    private static boolean createThumbnail(Path source, Path target) {
        try {
            System.out.println("Creating thumbnail " + source.normalize().toAbsolutePath());
            List<String> commande = List.of("magick", source.normalize().toAbsolutePath().toString(), "-resize", "300x", target.normalize().toAbsolutePath().toString());
            ProcessBuilder builder = new ProcessBuilder(commande);
            builder.inheritIO();
            Process process = builder.start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished)
                process.destroy();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public static class ImageMagick{
        public String detetImageMagickInstalled() {
            return "bla";
        }
        public enum Version {
            NA,IM_6,IM_7
        }
    }


}
