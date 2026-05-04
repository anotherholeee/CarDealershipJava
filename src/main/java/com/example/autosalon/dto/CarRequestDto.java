package com.example.autosalon.dto;

import com.example.autosalon.CarModelYear;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO для создания или обновления автомобиля")
public class CarRequestDto {

    @Schema(description = "Бренд автомобиля", example = "Toyota")
    @NotBlank(message = "Бренд не может быть пустым")
    @Size(min = 2, max = 100, message = "Бренд должен содержать от 2 до 100 символов")
    private String brand;

    @Schema(description = "Модель автомобиля", example = "Camry")
    @NotBlank(message = "Модель не может быть пустой")
    @Size(min = 1, max = 100, message = "Модель должна содержать от 1 до 100 символов")
    private String model;

    @Schema(description = "Год выпуска", example = "2022")
    @NotNull(message = "Год выпуска обязателен")
    @Min(value = CarModelYear.MIN, message = "Год выпуска не может быть меньше 1886")
    @Max(value = CarModelYear.MAX, message = "Год выпуска не может быть больше 2026")
    private Integer year;

    @Schema(description = "Цвет автомобиля", example = "Чёрный металлик")
    @NotBlank(message = "Цвет не может быть пустым")
    private String color;

    @Schema(description = "Цвет салона", example = "Чёрный")
    @NotBlank(message = "Укажите цвет салона")
    @Size(max = 100)
    private String interiorColor;

    @Schema(description = "Материал салона", example = "Кожа")
    @NotBlank(message = "Укажите материал салона")
    @Size(max = 100)
    private String interiorMaterial;

    @Schema(description = "Объем двигателя, л", example = "2.0")
    @NotNull(message = "Укажите объем двигателя")
    @DecimalMin(value = "0.1", message = "Объем двигателя должен быть больше 0")
    @DecimalMax(value = "20.0", message = "Объем двигателя не может превышать 20 л")
    private Double engineVolume;

    @Schema(description = "Пробег, км", example = "37000")
    @NotNull(message = "Укажите пробег")
    @PositiveOrZero(message = "Пробег не может быть отрицательным")
    private Integer mileage;

    @Schema(description = "Мощность, л.с.", example = "190")
    @NotNull(message = "Укажите мощность")
    @Min(value = 1, message = "Мощность должна быть не менее 1 л.с.")
    @Max(value = 2000, message = "Мощность не может превышать 2000 л.с.")
    private Integer powerHp;

    @Schema(description = "Расход, л/100км (по городу)", example = "9.5")
    @NotNull(message = "Укажите расход по городу")
    @DecimalMin(value = "0.1", inclusive = true, message = "Расход по городу должен быть больше 0")
    @DecimalMax(value = "100.0", message = "Расход по городу не может превышать 100 л/100км")
    private Double fuelConsumptionCity;

    @Schema(description = "Расход, л/100км (по трассе)", example = "6.2")
    @NotNull(message = "Укажите расход по трассе")
    @DecimalMin(value = "0.1", inclusive = true, message = "Расход по трассе должен быть больше 0")
    @DecimalMax(value = "100.0", message = "Расход по трассе не может превышать 100 л/100км")
    private Double fuelConsumptionHighway;

    @Schema(description = "Расход, л/100км (смешанный)", example = "7.5")
    @NotNull(message = "Укажите смешанный расход")
    @DecimalMin(value = "0.1", inclusive = true, message = "Смешанный расход должен быть больше 0")
    @DecimalMax(value = "100.0", message = "Смешанный расход не может превышать 100 л/100км")
    private Double fuelConsumptionMixed;

    @Schema(description = "Количество мест", example = "5")
    @NotNull(message = "Укажите количество мест")
    @Min(value = 1, message = "Минимум 1 место")
    @Max(value = 9, message = "Максимум 9 мест")
    private Integer seatCount;

    @Schema(description = "Город, где находится автомобиль", example = "Гродно")
    @NotBlank(message = "Укажите город")
    @Size(max = 100, message = "Название города не длиннее 100 символов")
    private String city;

    @Schema(description = "Коробка передач", example = "auto", allowableValues = {"auto", "manual", "robot"})
    @NotBlank(message = "Укажите коробку передач")
    @Size(max = 32)
    private String transmission;

    @Schema(description = "Тип кузова", example = "sedan")
    @NotBlank(message = "Укажите тип кузова")
    @Size(max = 32)
    private String bodyType;

    @Schema(description = "Тип двигателя", example = "petrol")
    @NotBlank(message = "Укажите тип двигателя")
    @Size(max = 32)
    private String engineType;

    @Schema(description = "Привод", example = "fwd")
    @NotBlank(message = "Укажите тип привода")
    @Size(max = 32)
    private String driveType;

    @Schema(description = "Цена в указанной валюте (USD или BYN)", example = "32000")
    @NotNull(message = "Цена обязательна")
    @Positive(message = "Цена должна быть положительной")
    @DecimalMax(value = "1000000", inclusive = true, message = "Цена не может превышать 1 000 000 в выбранной валюте")
    private Double price;

    @Schema(description = "Валюта цены", example = "USD", allowableValues = {"USD", "BYN"})
    @Pattern(regexp = "USD|BYN", message = "Валюта: только USD или BYN")
    private String priceCurrency = "USD";

    @ArraySchema(schema = @Schema(description = "Список ID опций", example = "1"))
    private List<Long> featureIds;

    @Schema(description = "Только для ADMIN: id владельца (user_accounts). Если не указан — владелец = текущий пользователь.")
    @Positive(message = "ownerUserId должен быть положительным")
    private Long ownerUserId;
}