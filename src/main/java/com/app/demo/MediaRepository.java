package com.app.demo;

import org.hibernate.annotations.processing.HQL;
import org.hibernate.query.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {
    Media findMediaByFilenameAndHash(String filename, String hash);
    @HQL("select count(m) > 0 from Media m where m.filename = :filename and m.hash = :hash")
    Boolean existsByFilenameAndHash(String filename, String hash);
    @Query("from Media m ORDER BY m.creationDate DESC")
    List<Media> media();

}
