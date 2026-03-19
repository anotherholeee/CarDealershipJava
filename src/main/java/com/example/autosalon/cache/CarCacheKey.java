package com.example.autosalon.cache;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.data.domain.Sort;

/**
 * Составной ключ для кэша
 * Содержит ВСЕ параметры запроса
 * equals() и hashCode() генерируются Lombok (@EqualsAndHashCode)
 */
@Getter
@EqualsAndHashCode
public class CarCacheKey {
    private final String category;
    private final int page;
    private final int size;
    private final String sortBy;
    private final Sort.Direction direction;

    public CarCacheKey(
            String category, int page, int size, String sortBy, Sort.Direction direction) {
        this.category = category;
        this.page = page;
        this.size = size;
        this.sortBy = sortBy;
        this.direction = direction;
    }

    /**
     * Переопределяем toString для читаемых ключей
     */
    @Override
    public String toString() {
        return String.format("CarCacheKey{category=%s, page=%d, size=%d, sortBy=%s, direction=%s}",
                category, page, size, sortBy, direction);
    }
}