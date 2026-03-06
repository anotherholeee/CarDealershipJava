package com.example.autosalon.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cars")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"dealership", "sale", "features"})
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    private int year;
    private String color;
    private double price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealership_id")
    private Dealership dealership;


    @ManyToMany
    @JoinTable(
            name = "car_features",
            joinColumns = @JoinColumn(name = "car_id"),
            inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    private List<Feature> features = new ArrayList<>();

    @OneToOne(mappedBy = "car")
    @JsonIgnore
    private Sale sale;

    public void addFeature(Feature feature) {
        features.add(feature);
        feature.getCars().add(this);
    }

    public void removeFeature(Feature feature) {
        features.remove(feature);
        feature.getCars().remove(this);
    }
}