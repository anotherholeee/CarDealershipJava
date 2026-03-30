# 4 ЛИСТИНГ КОДА

Файл `pom.xml`

```xml
<!-- AOP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- OpenAPI / Swagger UI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>

<!-- Bean Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Файл `src/main/resources/application.properties`

```properties
# Hibernate logging
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# 404 через exception для GlobalExceptionHandler
spring.mvc.throw-exception-if-no-handler-found=true
spring.web.resources.add-mappings=false
```

Файл `src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"/>
    <property name="LOG_PATH" value="logs"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/autosalon.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/autosalon-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>10MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/autosalon-error.log</file>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/autosalon-error-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>10MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <logger name="com.example.autosalon" level="DEBUG"/>
    <logger name="org.springframework.web" level="INFO"/>
    <logger name="org.springframework.security" level="INFO"/>
    <logger name="org.hibernate" level="WARN"/>
    <logger name="org.hibernate.SQL" level="DEBUG"/>
    <logger name="org.hibernate.type.descriptor.sql.BasicBinder" level="TRACE"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="ERROR_FILE"/>
    </root>
</configuration>
```

Файл `src/main/java/com/example/autosalon/dto/ErrorResponseDto.java`

```java
package com.example.autosalon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Единый формат ошибки API")
public class ErrorResponseDto {
    @Schema(description = "Время возникновения ошибки", example = "2026-03-29 20:15:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    @Schema(description = "HTTP статус-код", example = "400")
    private int status;
    @Schema(description = "Краткое название ошибки", example = "Bad Request")
    private String error;
    @Schema(description = "Подробное сообщение об ошибке", example = "Некорректный формат email")
    private String message;
    @Schema(description = "Путь запроса", example = "/api/customers")
    private String path;
    @Schema(description = "HTTP метод запроса", example = "POST")
    private String method;
}
```

Файл `src/main/java/com/example/autosalon/GlobalExceptionHandler.java`

```java
package com.example.autosalon;

import com.example.autosalon.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Ошибка валидации входных данных");
        ErrorResponseDto error = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String msg = String.format("Параметр '%s' должен быть типа %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "неизвестно");
        ErrorResponseDto error = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(msg)
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message("Endpoint не найден: " + ex.getRequestURL())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(
            Exception ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Внутренняя ошибка сервера: " + ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

Файл `src/main/java/com/example/autosalon/aspect/LoggingAspect.java`

```java
package com.example.autosalon.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.example.autosalon.service..*.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.info("▶ ВЫЗОВ: {}.{}", className, methodName);

        Object result;
        try {
            result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("◀ ВЫПОЛНЕН: {}.{} за {} мс", className, methodName, elapsed);
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("✗ ОШИБКА: {}.{} после {} мс - {}", className, methodName, elapsed, ex.getMessage());
            throw ex;
        }
        return result;
    }
}
```

Файл `src/main/java/com/example/autosalon/config/OpenApiConfig.java`

```java
package com.example.autosalon.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI autosalonOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Autosalon API")
                        .description("REST API для управления автосалоном, автомобилями, клиентами, продажами и опциями.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Autosalon Team")
                                .email("support@autosalon.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
```

Файл `src/main/java/com/example/autosalon/dto/CarRequestDto.java`

```java
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
    @Min(value = 1886, message = "Год выпуска не может быть меньше 1886")
    @Max(value = 2026, message = "Год выпуска не может быть больше 2026")
    private Integer year;

    @Schema(description = "Цвет автомобиля", example = "Black")
    @NotBlank(message = "Цвет не может быть пустым")
    private String color;

    @Schema(description = "Цена автомобиля", example = "3200000")
    @NotNull(message = "Цена обязательна")
    @Positive(message = "Цена должна быть положительной")
    private Double price;

    @ArraySchema(schema = @Schema(description = "Список ID опций", example = "1"))
    private List<Long> featureIds;
}
```

Файл `src/main/java/com/example/autosalon/dto/CustomerRequestDto.java`

```java
@Schema(description = "DTO для создания или обновления клиента")
public class CustomerRequestDto {

    @Schema(description = "Имя клиента", example = "Ivan")
    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 2, max = 100, message = "Имя должно содержать от 2 до 100 символов")
    private String firstName;

    @Schema(description = "Фамилия клиента", example = "Petrov")
    @NotBlank(message = "Фамилия не может быть пустой")
    @Size(min = 2, max = 100, message = "Фамилия должна содержать от 2 до 100 символов")
    private String lastName;

    @Schema(description = "Email клиента", example = "ivan.petrov@example.com")
    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат email")
    private String email;

    @Schema(description = "Телефон клиента", example = "+79991234567")
    @Pattern(regexp = "^\\+?[0-9\\-\\s]{10,20}$", message = "Некорректный формат телефона")
    private String phone;
}
```

Файл `src/main/java/com/example/autosalon/dto/DealershipRequestDto.java`

```java
@Schema(description = "DTO для создания или обновления автосалона")
public class DealershipRequestDto {

    @Schema(description = "Название автосалона", example = "Auto Premium")
    @NotBlank(message = "Название автосалона не может быть пустым")
    @Size(min = 3, max = 200, message = "Название должно содержать от 3 до 200 символов")
    private String name;

    @Schema(description = "Адрес автосалона", example = "Moscow, Tverskaya 12")
    @NotBlank(message = "Адрес не может быть пустым")
    @Size(max = 300, message = "Адрес не может превышать 300 символов")
    private String address;

    @Schema(description = "Телефон автосалона", example = "+74951234567")
    @Pattern(regexp = "^\\+?[0-9\\-\\s]{10,20}$", message = "Некорректный формат телефона")
    private String phone;
}
```

Файл `src/main/java/com/example/autosalon/dto/FeatureRequestDto.java`

```java
@Schema(description = "DTO для создания или обновления опции автомобиля")
public class FeatureRequestDto {

    @Schema(description = "Название опции", example = "Подогрев сидений")
    @NotBlank(message = "Название особенности не может быть пустым")
    @Size(min = 3, max = 100, message = "Название должно содержать от 3 до 100 символов")
    private String name;

    @Schema(description = "Описание опции", example = "Подогрев передних и задних сидений")
    @Size(max = 500, message = "Описание не может превышать 500 символов")
    private String description;

    @Schema(description = "Категория опции", example = "Комфорт")
    @NotBlank(message = "Категория не может быть пустой")
    private String category;
}
```

Файл `src/main/java/com/example/autosalon/dto/SaleRequestDto.java`

```java
@Schema(description = "DTO для создания или обновления продажи")
public class SaleRequestDto {

    @Schema(description = "ID автомобиля", example = "10")
    @NotNull(message = "ID автомобиля обязателен")
    private Long carId;

    @Schema(description = "ID покупателя", example = "7")
    @NotNull(message = "ID покупателя обязателен")
    private Long customerId;

    @Schema(description = "Дата продажи в формате ISO-8601", example = "2026-03-01T12:30:00")
    @NotNull(message = "Дата продажи обязательна")
    @PastOrPresent(message = "Дата продажи не может быть в будущем")
    private LocalDateTime saleDate;

    @Schema(description = "Итоговая цена продажи", example = "2999000")
    @NotNull(message = "Цена продажи обязательна")
    @Positive(message = "Цена продажи должна быть положительной")
    private Double salePrice;
}
```

Файл `src/main/java/com/example/autosalon/controller/CarController.java`

```java
@Tag(name = "Cars", description = "Операции с автомобилями")
public class CarController {

    @GetMapping
    @Operation(summary = "Получить список автомобилей", description = "Возвращает все автомобили или фильтрует по бренду")
    public ResponseEntity<List<CarResponseDto>> getCars(@RequestParam(required = false) String brand) { ... }

    @GetMapping("/{id}")
    @Operation(summary = "Получить автомобиль по ID")
    public ResponseEntity<CarResponseDto> getCarById(@PathVariable Long id) { ... }

    @GetMapping("/search/jpql")
    @Operation(summary = "Поиск автомобилей по категории опции (JPQL)")
    public ResponseEntity<List<CarResponseDto>> getCarsByFeatureCategoryJpql(@RequestParam String category) { ... }

    @GetMapping("/pagination/jpql")
    @Operation(summary = "Поиск автомобилей с пагинацией (JPQL)")
    public ResponseEntity<PageResponseDto<CarResponseDto>> getCarsWithPaginationJpql(@ModelAttribute CarSearchRequest request) { ... }

    @PostMapping
    @Operation(summary = "Создать автомобиль")
    public ResponseEntity<CarResponseDto> createCar(@Valid @RequestBody CarRequestDto createDto) { ... }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить автомобиль")
    public ResponseEntity<CarResponseDto> updateCar(@PathVariable Long id, @Valid @RequestBody CarRequestDto updateDto) { ... }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить автомобиль")
    public ResponseEntity<Void> deleteCar(@PathVariable Long id) { ... }
}
```

Файл `src/main/java/com/example/autosalon/controller/CustomerController.java`

```java
@Tag(name = "Customers", description = "Операции с клиентами")
public class CustomerController {

    @GetMapping @Operation(summary = "Получить список клиентов")
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() { ... }

    @GetMapping("/{id}") @Operation(summary = "Получить клиента по ID")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@PathVariable Long id) { ... }

    @GetMapping("/email/{email}") @Operation(summary = "Получить клиента по email")
    public ResponseEntity<CustomerResponseDto> getCustomerByEmail(@PathVariable String email) { ... }

    @PostMapping @Operation(summary = "Создать клиента")
    public ResponseEntity<CustomerResponseDto> createCustomer(@Valid @RequestBody CustomerRequestDto requestDto) { ... }

    @PutMapping("/{id}") @Operation(summary = "Обновить клиента")
    public ResponseEntity<CustomerResponseDto> updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerRequestDto requestDto) { ... }

    @PatchMapping("/{id}/phone") @Operation(summary = "Обновить телефон клиента")
    public ResponseEntity<CustomerResponseDto> updateCustomerPhone(@PathVariable Long id, @RequestParam String phone) { ... }

    @DeleteMapping("/{id}") @Operation(summary = "Удалить клиента")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) { ... }
}
```

Файл `src/main/java/com/example/autosalon/controller/DealershipController.java`

```java
@Tag(name = "Dealerships", description = "Операции с автосалонами")
public class DealershipController {

    @GetMapping @Operation(summary = "Получить список автосалонов")
    public ResponseEntity<List<DealershipResponseDto>> getAllDealerships() { ... }

    @GetMapping("/{id}") @Operation(summary = "Получить автосалон по ID")
    public ResponseEntity<DealershipResponseDto> getDealershipById(@PathVariable Long id) { ... }

    @GetMapping("/{id}/with-cars") @Operation(summary = "Получить автосалон вместе с машинами")
    public ResponseEntity<DealershipWithCarsResponseDto> getDealershipWithCars(@PathVariable Long id) { ... }

    @PostMapping @Operation(summary = "Создать автосалон")
    public ResponseEntity<DealershipResponseDto> createDealership(@Valid @RequestBody DealershipRequestDto requestDto) { ... }

    @PutMapping("/{id}") @Operation(summary = "Обновить автосалон")
    public ResponseEntity<DealershipResponseDto> updateDealership(@PathVariable Long id, @Valid @RequestBody DealershipRequestDto requestDto) { ... }

    @DeleteMapping("/{id}") @Operation(summary = "Удалить автосалон")
    public ResponseEntity<Void> deleteDealership(@PathVariable Long id) { ... }

    @PostMapping("/{dealershipId}/cars/{carId}") @Operation(summary = "Добавить автомобиль в автосалон")
    public ResponseEntity<DealershipWithCarsResponseDto> addCarToDealership(@PathVariable Long dealershipId, @PathVariable Long carId) { ... }

    @DeleteMapping("/{dealershipId}/cars/{carId}") @Operation(summary = "Удалить автомобиль из автосалона")
    public ResponseEntity<DealershipWithCarsResponseDto> removeCarFromDealership(@PathVariable Long dealershipId, @PathVariable Long carId) { ... }
}
```

Файл `src/main/java/com/example/autosalon/controller/FeatureController.java`

```java
@Tag(name = "Features", description = "Операции с опциями автомобилей")
public class FeatureController {

    @GetMapping @Operation(summary = "Получить список опций")
    public ResponseEntity<List<FeatureResponseDto>> getAllFeatures() { ... }

    @GetMapping("/{id}") @Operation(summary = "Получить опцию по ID")
    public ResponseEntity<FeatureResponseDto> getFeatureById(@PathVariable Long id) { ... }

    @GetMapping("/category/{category}") @Operation(summary = "Получить опции по категории")
    public ResponseEntity<List<FeatureResponseDto>> getFeaturesByCategory(@PathVariable String category) { ... }

    @PostMapping @Operation(summary = "Создать опцию")
    public ResponseEntity<FeatureResponseDto> createFeature(@Valid @RequestBody FeatureRequestDto requestDto) { ... }

    @PutMapping("/{id}") @Operation(summary = "Обновить опцию")
    public ResponseEntity<FeatureResponseDto> updateFeature(@PathVariable Long id, @Valid @RequestBody FeatureRequestDto requestDto) { ... }

    @PatchMapping("/{id}/description") @Operation(summary = "Обновить описание опции")
    public ResponseEntity<FeatureResponseDto> updateFeatureDescription(@PathVariable Long id, @RequestParam String description) { ... }

    @DeleteMapping("/{id}") @Operation(summary = "Удалить опцию")
    public ResponseEntity<Void> deleteFeature(@PathVariable Long id) { ... }
}
```

Файл `src/main/java/com/example/autosalon/controller/SaleController.java`

```java
@Tag(name = "Sales", description = "Операции с продажами автомобилей")
public class SaleController {

    @GetMapping @Operation(summary = "Получить список продаж")
    public ResponseEntity<List<SaleResponseDto>> getAllSales() { ... }

    @GetMapping("/{id}") @Operation(summary = "Получить продажу по ID")
    public ResponseEntity<SaleResponseDto> getSaleById(@PathVariable Long id) { ... }

    @GetMapping("/car/{carId}") @Operation(summary = "Получить продажу по ID автомобиля")
    public ResponseEntity<SaleResponseDto> getSaleByCarId(@PathVariable Long carId) { ... }

    @GetMapping("/customer/{customerId}") @Operation(summary = "Получить продажи клиента")
    public ResponseEntity<List<SaleResponseDto>> getSalesByCustomerId(@PathVariable Long customerId) { ... }

    @GetMapping("/date-range") @Operation(summary = "Получить продажи в интервале дат")
    public ResponseEntity<List<SaleResponseDto>> getSalesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) { ... }

    @PostMapping @Operation(summary = "Создать продажу")
    public ResponseEntity<SaleResponseDto> createSale(@Valid @RequestBody SaleRequestDto createDto) { ... }

    @PutMapping("/{id}") @Operation(summary = "Обновить продажу")
    public ResponseEntity<SaleResponseDto> updateSale(@PathVariable Long id, @Valid @RequestBody SaleRequestDto updateDto) { ... }

    @DeleteMapping("/{id}") @Operation(summary = "Удалить продажу")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) { ... }
}
```
