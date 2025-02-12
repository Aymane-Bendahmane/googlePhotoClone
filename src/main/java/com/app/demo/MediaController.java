package com.app.demo;


import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

@Controller
public class MediaController {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".generated_thumbnails");

    private final MediaRepository mediaRepository;

    public MediaController(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        System.out.println("get request");
        Map<LocalDate, List<Media>> images = new LinkedHashMap<>();
        List<Media> media = mediaRepository.media();

        media.forEach(m -> {
            LocalDate creationDate = m.getCreationDate().toLocalDate();
            images.putIfAbsent(creationDate, new ArrayList<>());
            images.get(creationDate).add(m);
        });

        model.addAttribute("images", images);
        return "index";
    }

    @GetMapping("/a/{hash}")
    @ResponseBody
    public Resource download(@PathVariable String hash) {
        Path media = thumbnailsDir.resolve(hash.substring(0, 2)).resolve(hash.substring(2) + ".webp");
        return new PathResource(media);
    }
}
