package com.app.demo;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

// test extracted meta data from images
public class MediaTest {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @TestFactory
    Collection<DynamicTest> testMedia() {
        try (ScanResult scanResult = new ClassGraph().scan()) {
            List<String> images = scanResult
                    .getResourcesMatchingPattern(Pattern.compile("image-(.*)\\.jpg"))
                    .getPaths();

            // dimensions are correct
            // creation time
            // location


            return images.stream().map(img -> DynamicTest.dynamicTest(img, () -> {
                String fileName = img.substring(0, img.indexOf("."));
                try (InputStream json = MediaTest.class.getResourceAsStream("/" + fileName + ".json")) {
                    TestMetadata expectedMetaData = objectMapper.readValue(json, TestMetadata.class);

                    Path file = Path.of(MediaTest.class.getResource("/" + img).toURI());
                    Metadata actualMetadata = ImageMetadataReader.readMetadata(file.toFile());


                    Initializer.Dimensions dimensions = Initializer.getDimensions(actualMetadata);
                    assertThat(dimensions.height()).as("image height").isEqualTo(expectedMetaData.height());
                    assertThat(dimensions.width()).as("image width").isEqualTo(expectedMetaData.width());

                }

                assertThat(true).isTrue();
            })).toList();
        }

    }
}
