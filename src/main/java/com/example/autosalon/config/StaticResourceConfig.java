package com.example.autosalon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Явно публикуем frontend-ассеты из контейнера и из classpath fallback.
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("file:/app/static/assets/", "classpath:/static/assets/");
        registry.addResourceHandler("/index.html", "/favicon.svg")
                .addResourceLocations("file:/app/static/", "classpath:/static/");
    }
}
