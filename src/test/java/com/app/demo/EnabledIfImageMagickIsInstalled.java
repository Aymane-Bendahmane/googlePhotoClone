package com.app.demo;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(EnabledIfImageMagickIsInstalledCondition.class)
public @interface EnabledIfImageMagickIsInstalled {
}
