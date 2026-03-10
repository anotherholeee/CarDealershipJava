package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Feature;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.FeatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureService {

    private static final String FEATURE_NOT_FOUND_MESSAGE = "Feature not found with id: ";

    private final FeatureRepository featureRepository;
    private final CarRepository carRepository;

    @Transactional(readOnly = true)
    public List<Feature> getAllFeatures() {
        return featureRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Feature getFeatureById(Long id) {
        return featureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(FEATURE_NOT_FOUND_MESSAGE + id));
    }

    @Transactional(readOnly = true)
    public List<Feature> getFeaturesByCategory(String category) {
        return featureRepository.findByCategory(category);
    }

    @Transactional
    public Feature createFeature(Feature feature) {
        feature.setId(null);
        return featureRepository.save(feature);
    }

    @Transactional
    public Feature updateFeature(Long id, Feature featureDetails) {
        Feature existingFeature = featureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(FEATURE_NOT_FOUND_MESSAGE + id));

        existingFeature.setName(featureDetails.getName());
        existingFeature.setDescription(featureDetails.getDescription());
        existingFeature.setCategory(featureDetails.getCategory());

        return existingFeature;
    }

    @Transactional
    public Feature updateDescription(Long id, String description) {
        Feature feature = featureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(FEATURE_NOT_FOUND_MESSAGE + id));
        feature.setDescription(description);
        return feature;
    }

    @Transactional
    public void deleteFeature(Long id) {
        Feature feature = featureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(FEATURE_NOT_FOUND_MESSAGE + id));

        List<Car> carsWithFeature = carRepository.findAll().stream()
                .filter(car -> car.getFeatures().contains(feature))
                .toList();

        if (!carsWithFeature.isEmpty()) {
            for (Car car : carsWithFeature) {
                car.removeFeature(feature);
                carRepository.save(car);
            }
            log.info("Особенность удалена из {} машин", carsWithFeature.size());
        }

        featureRepository.delete(feature);
        log.info("Feature with id {} successfully deleted", id);
    }
}