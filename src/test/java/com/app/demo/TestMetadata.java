package com.app.demo;

import java.time.LocalDateTime;

public record TestMetadata(Integer width,
                           Integer height,
                           LocalDateTime date,
                           String latitude,
                           String longitude) {
}