package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Feature;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.FeatureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

    @Mock private FeatureRepository featureRepository;
    @Mock private CarRepository carRepository;
    @InjectMocks private FeatureService featureService;

    private Feature feature;

    @BeforeEach
    void setUp() {
        feature = new Feature();
        feature.setId(1L);
        feature.setName("Heated seats");
        feature.setCategory("Comfort");
    }

    @Test
    void getAllFeatures_shouldReturnList() {
        when(featureRepository.findAll()).thenReturn(List.of(feature));
        List<Feature> features = featureService.getAllFeatures();
        assertThat(features).hasSize(1);
    }

    @Test
    void getFeatureById_shouldReturn() {
        when(featureRepository.findById(1L)).thenReturn(Optional.of(feature));
        Feature found = featureService.getFeatureById(1L);
        assertThat(found).isEqualTo(feature);
    }

    @Test
    void getFeatureById_notFound_throws() {
        when(featureRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> featureService.getFeatureById(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getFeaturesByCategory_shouldReturnList() {
        when(featureRepository.findByCategory("Comfort")).thenReturn(List.of(feature));
        List<Feature> result = featureService.getFeaturesByCategory("Comfort");
        assertThat(result).containsExactly(feature);
    }

    @Test
    void createFeature_shouldSave() {
        when(featureRepository.save(any(Feature.class))).thenReturn(feature);
        Feature created = featureService.createFeature(feature);
        assertThat(created).isEqualTo(feature);
    }

    @Test
    void updateFeature_shouldUpdateFields() {
        Feature details = new Feature();
        details.setName("New");
        details.setDescription("Desc");
        details.setCategory("Tech");

        when(featureRepository.findById(1L)).thenReturn(Optional.of(feature));
        Feature updated = featureService.updateFeature(1L, details);
        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getDescription()).isEqualTo("Desc");
        assertThat(updated.getCategory()).isEqualTo("Tech");
    }

    @Test
    void updateDescription_shouldUpdate() {
        when(featureRepository.findById(1L)).thenReturn(Optional.of(feature));
        Feature updated = featureService.updateDescription(1L, "D");
        assertThat(updated.getDescription()).isEqualTo("D");
    }

    @Test
    void deleteFeature_shouldRemoveFromCars() {
        when(featureRepository.findById(1L)).thenReturn(Optional.of(feature));
        when(carRepository.findCarsByFeatureId(1L)).thenReturn(List.of()); // нет машин с этой опцией
        featureService.deleteFeature(1L);
        verify(featureRepository).delete(feature);
    }

    @Test
    void deleteFeature_whenCarsHaveFeature_shouldRemoveFeatureFromCars() {
        Car car = new Car();
        car.setId(10L);
        car.addFeature(feature);

        when(featureRepository.findById(1L)).thenReturn(Optional.of(feature));
        when(carRepository.findCarsByFeatureId(1L)).thenReturn(List.of(car));

        featureService.deleteFeature(1L);

        assertThat(car.getFeatures()).doesNotContain(feature);
        verify(featureRepository).delete(feature);
    }
}