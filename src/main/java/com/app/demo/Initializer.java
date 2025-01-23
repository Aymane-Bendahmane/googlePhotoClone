package com.app.demo;


import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.png.PngChunkType;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
@Component
public class Initializer implements ApplicationRunner {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".generated_thumbnails");
    private final ImageMagick imageMagick;
    static HttpClient client = HttpClient.newHttpClient();
    private final MediaRepository mediaRepository;

    public Initializer(ImageMagick imageMagick,MediaRepository mediaRepository) {
        this.imageMagick = imageMagick;
        this.mediaRepository = mediaRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Files.createDirectories(thumbnailsDir);
        //point to the current directory where the jar exists "."

        String directory = "C:\\Users\\ayman\\OneDrive\\Desktop\\GooglePhotoClone\\sample-images\\100-100-color";
        Path sourceDir = Path.of(directory);
        AtomicInteger counter = new AtomicInteger();
        long start = System.currentTimeMillis();

        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try (Stream<Path> files = Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .filter(Initializer::isImage)) {
            files.forEach(image -> executorService.submit(() -> {
                String filename = image.getFileName().toString();

                String hash = DigestUtils.sha256Hex(image.toString());


                if (!mediaRepository.existsByFilenameAndHash(filename, hash)) {


                    Path thumbnail = getThumbnailPath(hash);

                    if (!Files.exists(thumbnail)) {
                        final boolean success = imageMagick.createThumbnail(image, thumbnail);
                        if (!success) {
                            System.err.println("Error creating thumbnail");
                            return;
                        }
                    }


                    try (InputStream is = Files.newInputStream(image)) {
                        Metadata metadata = ImageMetadataReader.readMetadata(is);
                        Location location = getLocation(metadata);
                        LocalDateTime creationTime = getCreationTime(image, metadata);
                        mediaRepository.save(new Media(hash, filename, creationTime, location));
                        counter.incrementAndGet();
                    } catch (ImageProcessingException e) {
                        e.printStackTrace();
                        // not an image or something else
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }));
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.HOURS);
        }
        long end = System.currentTimeMillis();
        System.out.println(" Converted " + counter + " images to thumbnails. took " + ((end - start) * 0.001) + " seconds");
    }
    static Location getLocation(Metadata metadata) {
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gpsDirectory == null) {
            return null;
        }

        GeoLocation geoLocation = gpsDirectory.getGeoLocation();
        if (geoLocation == null) {
            return null;
        }

        double latitude = geoLocation.getLatitude();
        double longitude = geoLocation.getLongitude();
        String dms = geoLocation.toDMSString();
        AtomicReference<String> state = new AtomicReference<>("UNKNOWN");
        AtomicReference<String> city = new AtomicReference<>("UNKNOWN");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.3geonames.org/" + latitude + "," + longitude))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(xml -> {
                    state.set(xml.substring(xml.indexOf("<state>") + 7, xml.indexOf("</state>")));
                    city.set(xml.substring(xml.indexOf("<city>") + 6, xml.indexOf("</city>")));
                })
                .join();
        return new Location(latitude, longitude, state.get(), city.get(), dms);
    }
    static LocalDateTime getCreationTime(Path image, Metadata metadata) {

        ExifSubIFDDirectory exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (exifSubIFDDirectory != null && exifSubIFDDirectory.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
            Date creatioDate = exifSubIFDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            return creatioDate.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
        }


        ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (exifIFD0Directory != null && exifIFD0Directory.containsTag(ExifIFD0Directory.TAG_DATETIME)) {
            Date creatioDate = exifIFD0Directory.getDate(ExifIFD0Directory.TAG_DATETIME);
            return creatioDate.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
        }


        GpsDirectory firstDirectoryOfType = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (firstDirectoryOfType != null && firstDirectoryOfType.getGpsDate() != null) {
            Date gpsDate = firstDirectoryOfType.getGpsDate();
            return gpsDate.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
        }

        try {
            BasicFileAttributes attr = Files.readAttributes(image, BasicFileAttributes.class);
            FileTime fileTime = attr.creationTime();
            return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            e.printStackTrace();
            return LocalDateTime.now();
        }
    }


    private static Path getThumbnailPath(String hash){

        String subDir = hash.substring(0, 2);
        String fileName = hash.substring(2);

        Path subDirectory = thumbnailsDir.resolve(subDir);
        if (!Files.exists(subDirectory)) {
            try {
                Files.createDirectories(subDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return subDirectory.resolve(fileName + ".webp");
    }

    private static boolean isImage(Path path) {
        try {
            String mimeType = Files.probeContentType(path);
            return mimeType != null && mimeType.contains("image");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static LocalDateTime creationTime(Path file) {
        try {
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
            FileTime fileTime = attr.creationTime();
            return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    record Dimensions(int width, int height) {
    }

    static Dimensions getDimensions(Metadata metadata) {
        try {
            ExifIFD0Directory exifIfD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exifIfD0Directory != null && exifIfD0Directory.containsTag(ExifIFD0Directory.TAG_IMAGE_WIDTH) && exifIfD0Directory.containsTag(ExifIFD0Directory.TAG_IMAGE_HEIGHT)) {
                int width = exifIfD0Directory.getInt(ExifIFD0Directory.TAG_IMAGE_WIDTH);
                int height = exifIfD0Directory.getInt(ExifIFD0Directory.TAG_IMAGE_HEIGHT);
                return new Dimensions(width, height);
            }

            PngDirectory pngDirectory = metadata.getFirstDirectoryOfType(PngDirectory.class);
            if (pngDirectory != null && pngDirectory.getPngChunkType().equals(PngChunkType.IHDR)) {
                int width = pngDirectory.getInt(PngDirectory.TAG_IMAGE_WIDTH);
                int height = pngDirectory.getInt(PngDirectory.TAG_IMAGE_HEIGHT);
                return new Dimensions(width, height);
            }

            JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
            if (jpegDirectory != null && jpegDirectory.containsTag(JpegDirectory.TAG_IMAGE_WIDTH) && jpegDirectory.containsTag(JpegDirectory.TAG_IMAGE_HEIGHT)) {
                int width = jpegDirectory.getInt(JpegDirectory.TAG_IMAGE_WIDTH);
                int height = jpegDirectory.getInt(JpegDirectory.TAG_IMAGE_HEIGHT);
                return new Dimensions(width, height);
            }
        } catch (MetadataException e) {
            e.printStackTrace();
        }
        return new Dimensions(0, 0);
    }
}
