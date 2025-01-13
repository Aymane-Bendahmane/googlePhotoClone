package com.app.demo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ImageMagick {


    public int run(String... cmds) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(cmds);
        builder.inheritIO();
        Process process = builder.start();
        boolean finished = process.waitFor(1, TimeUnit.SECONDS);
        if (!finished)
            process.destroy();
        return process.exitValue();
    }

    public Version detectImageMagickInstalled() {

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
