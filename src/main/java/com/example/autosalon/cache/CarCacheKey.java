package com.example.autosalon.cache;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.data.domain.Sort;


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


    @Override
    public String toString() {
        return String.format("CarCacheKey{category=%s, page=%d, size=%d, sortBy=%s, direction=%s}",
                category, page, size, sortBy, direction);
    }
}