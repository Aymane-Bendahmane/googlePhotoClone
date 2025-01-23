package com.app.demo;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Component
public class ImageMagick {

    private final Version currentImageMagickVersion = detectVersion();

    public boolean createThumbnail(Path source, Path target) {
        try {
            System.out.println("Creating thumbnail " + source.normalize().toAbsolutePath());
            List<String> command = new ArrayList<>(List.of("magick", source.normalize().toAbsolutePath().toString(), "-resize", "300x", target.normalize().toAbsolutePath().toString()));

            if(currentImageMagickVersion == Version.IM_6 )
                command.add(0,"convert");

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.inheritIO();
            Process process = builder.start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished)
                process.destroy();
            return finished;
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int run(String... cmds) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(cmds);
        builder.inheritIO();
        Process process = builder.start();
        boolean finished = process.waitFor(1, TimeUnit.SECONDS);
        if (!finished)
            process.destroy();
        return process.exitValue();
    }

    public Version detectVersion() {

        try {
            int existCode = run("magick", "--version");
            if (existCode == 0) {
                return Version.IM_7;
            }

            existCode = run("convert", "--version");
            if (existCode == 0) {
                return Version.IM_6;
            }

            return Version.NA;
        } catch (IOException | InterruptedException e) {
            return Version.NA;
        }

    }

    public enum Version {
        NA, IM_6, IM_7
    }
}
