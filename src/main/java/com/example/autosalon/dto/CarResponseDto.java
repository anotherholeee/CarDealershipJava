package com.example.autosalon.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarResponseDto {
    private Long id;
    private String brand;
    private String model;
    private int year;
    private String color;
    private String interiorColor;
    private String interiorMaterial;
    private Double engineVolume;
    private Integer mileage;
    private Integer powerHp;
    private Double fuelConsumptionCity;
    private Double fuelConsumptionHighway;
    private Double fuelConsumptionMixed;
    private Integer seatCount;
    private String city;
    private String transmission;
    private String bodyType;
    private String engineType;
    private String driveType;
    private double price;
    /** USD или BYN. */
    private String priceCurrency;
    private List<String> featureNames;
    /** PERSON / DEALERSHIP — тип продавца; null, если владелец не задан (старые данные). */
    private String sellerAccountType;
    /** Название автосалона или имя продавца для списка объявлений. */
    private String sellerDisplayName;
    /** Телефон для связи: у автосалона из карточки салона, иначе телефон владельца объявления. */
    private String sellerPhone;
    /** Фотографии объявления (порядок — как на диске). */
    private List<CarImageInfoDto> photos;
    /** Когда объявление опубликовано (UTC). */
    private Instant publishedAt;
}