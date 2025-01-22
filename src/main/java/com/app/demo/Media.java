package com.app.demo;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String hash;
    private String filename;
    private LocalDateTime creationDate;

    @Embedded
    private Location location;

    public Media() {
    }

    public Media(String hash, String filename, LocalDateTime creationDate, Location location) {
        this.hash = hash;
        this.filename = filename;
        this.creationDate = creationDate;
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
