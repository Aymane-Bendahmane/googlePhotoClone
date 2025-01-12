package com.app.demo;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

class GooglePhotoCloneApplicationTests {
    @Test
    void imageMagick_is_installed(){
        assertThat(GooglePhotoCloneApplication.detetImageMagickInstalled().equals("bla"))
    }

    @Test
    void thumbnail_creation_works() {

    }

}
