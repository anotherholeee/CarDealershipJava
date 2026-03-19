package com.example.autosalon.cache;

import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.dto.PageResponseDto;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * In-memory кэш для результатов поиска автомобилей
 * Работает как полка с готовыми ответами
 */
@Slf4j
@Component
public class CarSearchCache {

    private final ConcurrentHashMap<CarCacheKey, PageResponseDto<CarResponseDto>> cache =
            new ConcurrentHashMap<>();

    /**
     * Получить результат из кэша
     * @param key составной ключ из параметров запроса
     * @return готовый ответ или null, если в кэше нет
     */
    public PageResponseDto<CarResponseDto> get(CarCacheKey key) {
        PageResponseDto<CarResponseDto> result = cache.get(key);

        if (result != null) {
            log.info("✅ КЭШ HIT: ключ={}", key);
        } else {
            log.info("❌ КЭШ MISS: ключ={}", key);
        }

        return result;
    }

    /**
     * Сохранить результат в кэш
     * @param key ключ
     * @param value результат запроса
     */
    public void put(CarCacheKey key, PageResponseDto<CarResponseDto> value) {
        cache.put(key, value);
        log.info("💾 КЭШ СОХРАНЕН: ключ={}, размер кэша={}", key, cache.size());
    }

    /**
     * Очистить весь кэш (когда данные меняются)
     */
    public void clear() {
        cache.clear();
        log.info("🧹 КЭШ ОЧИЩЕН");
    }

    /**
     * Удалить конкретную запись из кэша
     */
    public void remove(CarCacheKey key) {
        cache.remove(key);
        log.info("🗑️ КЭШ УДАЛЕН: ключ={}", key);
    }

    /**
     * Получить размер кэша
     */
    public int size() {
        return cache.size();
    }
}