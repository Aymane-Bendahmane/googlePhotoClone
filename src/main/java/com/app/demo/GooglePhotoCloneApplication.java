package com.app.demo;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.codec.digest.DigestUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class GooglePhotoCloneApplication {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".generated_thumbnails");
    static ImageMagick imageMagick = new ImageMagick();
    private static DataSource dataSource = dataSource();

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
        hikariConfig.setJdbcUrl("jdbc:h2:file:./media;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql';");
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
}
