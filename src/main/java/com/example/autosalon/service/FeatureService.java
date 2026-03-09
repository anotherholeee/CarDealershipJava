package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Feature;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.FeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeatureService {

    private final FeatureRepository featureRepository;
    private final CarRepository carRepository;

    @Transactional(readOnly = true)
    public List<Feature> getAllFeatures() {
        return featureRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Feature getFeatureById(Long id) {
        return featureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feature not found with id: " + id));
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
        Feature existingFeature = getFeatureById(id);

        existingFeature.setName(featureDetails.getName());
        existingFeature.setDescription(featureDetails.getDescription());
        existingFeature.setCategory(featureDetails.getCategory());

        return existingFeature;
    }

    @Transactional
    public Feature updateDescription(Long id, String description) {
        Feature feature = getFeatureById(id);
        feature.setDescription(description);
        return feature;
    }

    @Transactional
    public void deleteFeature(Long id) {
        Feature feature = getFeatureById(id);


        List<Car> carsWithFeature = carRepository.findAll().stream()
                .filter(car -> car.getFeatures().contains(feature))
                .toList();

        if (!carsWithFeature.isEmpty()) {
            for (Car car : carsWithFeature) {
                car.removeFeature(feature);
                carRepository.save(car);
            }
            System.out.println("Особенность удалена из " + carsWithFeature.size() + " машин");

        }

        featureRepository.delete(feature);
        System.out.println("Feature with id " + id + " successfully deleted");
    }
}