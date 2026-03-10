package com.example.autosalon.controller;

import com.example.autosalon.dto.FeatureRequestDto;
import com.example.autosalon.dto.FeatureResponseDto;
import com.example.autosalon.entity.Feature;
import com.example.autosalon.mapper.FeatureMapper;
import com.example.autosalon.service.FeatureService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureService featureService;
    private final FeatureMapper featureMapper;

    @GetMapping
    public ResponseEntity<List<FeatureResponseDto>> getAllFeatures() {
        List<FeatureResponseDto> response = featureService.getAllFeatures().stream()
                .map(featureMapper::toResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeatureResponseDto> getFeatureById(@PathVariable Long id) {
        return ResponseEntity.ok(featureMapper.toResponseDto(featureService.getFeatureById(id)));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<FeatureResponseDto>> getFeaturesByCategory(
            @PathVariable String category) {
        List<FeatureResponseDto> response = featureService
                .getFeaturesByCategory(category).stream()
                .map(featureMapper::toResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<FeatureResponseDto> createFeature(
            @RequestBody FeatureRequestDto requestDto) {
        Feature createdFeature =
                featureService.createFeature(featureMapper.toEntity(requestDto));
        return new ResponseEntity<>(
                featureMapper.toResponseDto(createdFeature),
                HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeatureResponseDto> updateFeature(
            @PathVariable Long id,
            @RequestBody FeatureRequestDto requestDto) {
        return ResponseEntity.ok(
                featureMapper.toResponseDto(
                        featureService.updateFeature(
                                id,
                                featureMapper.toEntity(requestDto)))
        );
    }

    @PatchMapping("/{id}/description")
    public ResponseEntity<FeatureResponseDto> updateFeatureDescription(
            @PathVariable Long id,
            @RequestParam String description) {
        return ResponseEntity.ok(
                featureMapper.toResponseDto(
                        featureService.updateDescription(id, description)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeature(@PathVariable Long id) {
        featureService.deleteFeature(id);
        return ResponseEntity.noContent().build();
    }
}