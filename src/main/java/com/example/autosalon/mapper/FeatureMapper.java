package com.example.autosalon.mapper;

import com.example.autosalon.dto.FeatureRequestDto;
import com.example.autosalon.dto.FeatureResponseDto;
import com.example.autosalon.entity.Feature;
import org.springframework.stereotype.Component;

@Component
public class FeatureMapper {

    public Feature toEntity(FeatureRequestDto dto) {
        Feature feature = new Feature();
        feature.setName(dto.getName());
        feature.setDescription(dto.getDescription());
        feature.setCategory(dto.getCategory());
        return feature;
    }

    public FeatureResponseDto toResponseDto(Feature feature) {
        FeatureResponseDto dto = new FeatureResponseDto();
        dto.setId(feature.getId());
        dto.setName(feature.getName());
        dto.setDescription(feature.getDescription());
        dto.setCategory(feature.getCategory());
        return dto;
    }
}

