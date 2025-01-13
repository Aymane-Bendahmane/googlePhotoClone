package com.app.demo;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

class ImageMagickTest{
    @Test
    void imageMagick_is_installed(){
        assertThat(new ImageMagick().detectImageMagickInstalled()).isNotEqualTo(ImageMagick.Version.NA);
    }

    @Test
    void thumbnail_creation_works() {

    }

}
