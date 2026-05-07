package com.example.autosalon.controller;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ResourceLoader resourceLoader;

    public HomeController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @GetMapping(value = {"/", "/index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> home() throws IOException {
        Resource fsIndex = resourceLoader.getResource("file:/app/static/index.html");
        if (fsIndex.exists() && fsIndex.isReadable()) {
            return ResponseEntity.ok(fsIndex);
        }

        Resource classpathIndex = resourceLoader.getResource("classpath:/static/index.html");
        if (classpathIndex.exists() && classpathIndex.isReadable()) {
            return ResponseEntity.ok(classpathIndex);
        }

        return ResponseEntity.notFound().build();
    }
}
