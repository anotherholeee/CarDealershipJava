package com.example.autosalon.entity;

import com.example.autosalon.CarModelYear;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.hibernate.annotations.BatchSize;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "cars")
@Data
@EqualsAndHashCode(exclude = {"dealership", "sale", "features", "owner", "images"})
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"dealership", "sale", "features", "owner", "images"})
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Min(CarModelYear.MIN)
    @Max(CarModelYear.MAX)
    private int year;
    private String color;
    /** Цвет салона (опционально). */
    private String interiorColor;
    /** Материал салона (опционально). */
    private String interiorMaterial;
    /** Объем двигателя в литрах (опционально). */
    private Double engineVolume;
    /** Пробег в километрах (опционально). */
    private Integer mileage;
    /** Мощность, л.с. */
    private Integer powerHp;
    /** Расход л/100 км, город. */
    private Double fuelConsumptionCity;
    /** Расход л/100 км, трасса. */
    private Double fuelConsumptionHighway;
    /** Расход л/100 км, смешанный. */
    private Double fuelConsumptionMixed;
    /** Количество мест. */
    private Integer seatCount;
    /** Город размещения объявления. */
    private String city;
    /** Коробка: auto, manual, robot. */
    private String transmission;
    /** Кузов: sedan, suv, hatchback, wagon, coupe, cabriolet. */
    private String bodyType;
    /** Тип двигателя: petrol, diesel, electric. */
    private String engineType;
    /** Привод: fwd, rwd, awd. */
    private String driveType;
    private double price;

    /** ISO-подобный код валюты цены: USD или BYN. */
    @Column(name = "price_currency", length = 3)
    private String priceCurrency = "USD";

    /** Момент публикации объявления (UTC). */
    @Column(name = "published_at")
    private Instant publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealership_id")
    @JsonIgnore
    private Dealership dealership;

    @BatchSize(size = 64)
    @ManyToMany
    @JoinTable(
            name = "car_features",
            joinColumns = @JoinColumn(name = "car_id"),
            inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    private Set<Feature> features = new LinkedHashSet<>();

    @OneToOne(mappedBy = "car")
    @JsonIgnore
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private UserAccount owner;

    @BatchSize(size = 64)
    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<CarImage> images = new ArrayList<>();

    public void addFeature(Feature feature) {
        features.add(feature);
        feature.getCars().add(this);
    }

    public void removeFeature(Feature feature) {
        features.remove(feature);
        feature.getCars().remove(this);
    }

    @PrePersist
    public void prePersistPublishedAt() {
        if (publishedAt == null) {
            publishedAt = Instant.now();
        }
    }
}