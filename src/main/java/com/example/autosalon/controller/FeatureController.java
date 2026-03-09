package com.example.autosalon.controller;

import com.example.autosalon.entity.Feature;
import com.example.autosalon.service.FeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureService featureService;

    @GetMapping
    public ResponseEntity<List<Feature>> getAllFeatures() {
        return ResponseEntity.ok(featureService.getAllFeatures());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Feature> getFeatureById(@PathVariable Long id) {
        return ResponseEntity.ok(featureService.getFeatureById(id));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Feature>> getFeaturesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(featureService.getFeaturesByCategory(category));
    }

    @PostMapping
    public ResponseEntity<Feature> createFeature(@RequestBody Feature feature) {
        Feature createdFeature = featureService.createFeature(feature);
        return new ResponseEntity<>(createdFeature, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Feature> updateFeature(@PathVariable Long id, @RequestBody Feature feature) {
        return ResponseEntity.ok(featureService.updateFeature(id, feature));
    }

    @PatchMapping("/{id}/description")
    public ResponseEntity<Feature> updateFeatureDescription(@PathVariable Long id, @RequestParam String description) {
        return ResponseEntity.ok(featureService.updateDescription(id, description));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeature(@PathVariable Long id) {
        featureService.deleteFeature(id);
        return ResponseEntity.noContent().build();
    }
}