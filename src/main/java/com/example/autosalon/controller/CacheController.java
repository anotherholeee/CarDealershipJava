package com.example.autosalon.controller;

import com.example.autosalon.cache.CarSearchCache;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CarSearchCache carSearchCache;


    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getCacheInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("size", carSearchCache.size());
        info.put("message", "Кэш хранит результаты поиска машин с пагинацией");
        return ResponseEntity.ok(info);
    }


    @GetMapping("/clear")
    public ResponseEntity<String> clearCache() {
        carSearchCache.clear();
        return ResponseEntity.ok("Кэш успешно очищен");
    }
}