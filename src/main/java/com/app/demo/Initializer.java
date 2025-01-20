package com.app.demo;


import com.drew.imaging.png.PngChunkType;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.codec.digest.DigestUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Initializer {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".generated_thumbnails");
    static ImageMagick imageMagick = new ImageMagick();
    private static final DataSource dataSource = dataSource();
    static String template = """
            <!DOCTYPE html>
            <html lang="en">
                <head>
                    <meta charset="UTF-8">
                        <title>Title</title>
                    </head>
                <body>
                    <h1>Pictures</h1>
                    {{pics}}
                </body>
            </html>
            """;

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
                .filter(Initializer::isImage)) {
            files.forEach(image -> executorService.submit(() -> {

                String hash = DigestUtils.sha256Hex(image.toString());

                if (!exists(image, hash)) {
                    save(image, hash);
                    final boolean success = createThumbnail(image, hash);
                    if (success) {
                        counter.incrementAndGet();
                    }
                }
            }));
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.HOURS);
        }
        long end = System.currentTimeMillis();
        writeHtmlFile();
        System.out.println(" Converted " + counter + " images to thumbnails. took " + ((end - start) * 0.001) + " seconds");
    }

    private static boolean exists(Path image, String hash) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select 1 from media where filename = ? and hash = ?")) {
            stmt.setString(1, image.getFileName().toString());
            stmt.setString(2, hash);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    private static void save(Path path, String hash) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO media (FILENAME, HASH, CREATION_DATE ) VALUES (?,? ,?)")) {

            statement.setString(1, path.getFileName().toString());
            statement.setString(2, hash);
            statement.setObject(3, creationTime(path));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path getThumbnailPath(String hash) throws IOException {

        String subDir = hash.substring(0, 2);
        String fileName = hash.substring(2);

        Path subDirectory = thumbnailsDir.resolve(subDir);
        if (!Files.exists(subDirectory)) {
            Files.createDirectories(subDirectory);
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

    private static DataSource dataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:h2:file:./media;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'");
        hikariConfig.setUsername("root");
        hikariConfig.setPassword("root");
        return new HikariDataSource(hikariConfig);
    }

    private static boolean createThumbnail(Path image, String hash) {
        try {
            Path thumbnail = getThumbnailPath(hash);
            return imageMagick.createThumbnail(image, thumbnail);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
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

    private static void writeHtmlFile() throws IOException {
        Map<LocalDate, List<String>> images = new TreeMap<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "select hash, CAST(creation_date AS DATE) creation_date from media " +
                     "order by creation_date desc")) {
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                String hash = resultSet.getString("hash");
                LocalDate creationDate = resultSet.getObject("creation_date", LocalDate.class);

                images.putIfAbsent(creationDate, new ArrayList<>());
                images.get(creationDate).add(hash);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        StringBuilder html = new StringBuilder();
        images.forEach((date, hashes) -> {
            html.append("<h2>").append(date).append("</h2>");

            hashes.forEach(hash -> {
                Path image = thumbnailsDir.resolve(hash.substring(0, 2)).resolve(hash.substring(2) + ".webp");
                html.append("<img width='300' src='").append(image.toAbsolutePath()).append("' loading='lazy'/>");
            });
            html.append("<br/>");
        });

        Files.write(Paths.get("./output.html"), template.replace("{{pics}}", html.toString()).getBytes());
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
