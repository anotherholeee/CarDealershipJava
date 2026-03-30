package com.example.autosalon.cache;

import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.dto.PageResponseDto;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class CarSearchCache {

    private final ConcurrentHashMap<CarCacheKey, PageResponseDto<CarResponseDto>> cache =
            new ConcurrentHashMap<>();


    public PageResponseDto<CarResponseDto> get(CarCacheKey key) {
        PageResponseDto<CarResponseDto> result = cache.get(key);

        if (result != null) {
            log.info(" КЭШ HIT: ключ={}", key);
        } else {
            log.info(" КЭШ MISS: ключ={}", key);
        }

        return result;
    }


    public void put(CarCacheKey key, PageResponseDto<CarResponseDto> value) {
        cache.put(key, value);
        log.info(" КЭШ СОХРАНЕН: ключ={}, размер кэша={}", key, cache.size());
    }


    public void clear() {
        cache.clear();
        log.info(" КЭШ ОЧИЩЕН");
    }


    public void remove(CarCacheKey key) {
        cache.remove(key);
        log.info(" КЭШ УДАЛЕН: ключ={}", key);
    }


    public int size() {
        return cache.size();
    }
}