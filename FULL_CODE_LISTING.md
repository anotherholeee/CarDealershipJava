# Полный листинг кода проекта

pom.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.2</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.example</groupId>
    <artifactId>autosalon</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>autosalon</name>
    <description>autosalon</description>
    <url/>
    <licenses>
        <license/>
    </licenses>
    <developers>
        <developer/>
    </developers>
    <scm>
        <connection/>
        <developerConnection/>
        <tag/>
        <url/>
    </scm>
    <properties>
        <java.version>17</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

            <!-- Checkstyle плагин -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-checkstyle-plugin</artifactId>
                <version>3.3.1</version>
                <configuration>
                    <configLocation>google_checks.xml</configLocation>
                    <consoleOutput>true</consoleOutput>
                    <failsOnError>true</failsOnError>
                    <linkXRef>false</linkXRef>
                </configuration>
                <executions>
                    <execution>
                        <id>checkstyle-check</id>
                        <phase>validate</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

</project>
```

src/main/resources/application.properties
```properties
spring.application.name=autosalon

# PostgreSQL connection
spring.datasource.url=jdbc:postgresql://localhost:5432/autosalon_db
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# Hibernate settings
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

src/main/java/com/example/autosalon/AutosalonApplication.java
```java
package com.example.autosalon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AutosalonApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutosalonApplication.class, args);
    }

}
```

src/main/java/com/example/autosalon/DataInitializer.java
```java
package com.example.autosalon;

import com.example.autosalon.entity.*;
import com.example.autosalon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final FeatureRepository featureRepository;
    private final CarRepository carRepository;
    private final DealershipRepository dealershipRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🚗 ЗАПУСК ИНИЦИАЛИЗАЦИИ ТЕСТОВЫХ ДАННЫХ");
        System.out.println("=".repeat(60));

        // 1. СОЗДАНИЕ ОСОБЕННОСТЕЙ (FEATURES)
        createFeatures();

        // 2. СОЗДАНИЕ АВТОСАЛОНОВ (DEALERSHIPS)
        createDealerships();

        // 3. СОЗДАНИЕ МАШИН (CARS) - ВАЖНО: сначала фичи, потом машины
        createCars();

        // 3.1. ГАРАНТИЯ СВЯЗЕЙ CAR-FEATURE (если машины уже были в БД)
        ensureCarFeatures();

        // 4. СОЗДАНИЕ ПОКУПАТЕЛЕЙ (CUSTOMERS)
        createCustomers();

        // 5. СОЗДАНИЕ ПРОДАЖ (SALES)
        createSales();

        // 6. ВЫВОД СТАТИСТИКИ
        printStatistics();

        // 7. ПРОВЕРКА СВЯЗЕЙ
        verifyFeatures();

        System.out.println("=".repeat(60));
        System.out.println("✅ ИНИЦИАЛИЗАЦИЯ ЗАВЕРШЕНА!");
        System.out.println("=".repeat(60) + "\n");
    }

    private void createFeatures() {
        if (featureRepository.count() > 0) {
            System.out.println("📊 Особенности уже существуют, пропускаем...");
            return;
        }

        System.out.println("\n🔧 СОЗДАНИЕ ОСОБЕННОСТЕЙ:");

        List<Feature> features = Arrays.asList(
                createFeature("Полный привод", "4WD/AWD система", "Технологии"),
                createFeature("Панорамная крыша", "Стеклянная крыша с люком", "Комфорт"),
                createFeature("Автопилот", "Система автономного вождения 2 уровня", "Технологии"),
                createFeature("Кожаный салон", "Натуральная кожа Nappa", "Комфорт"),
                createFeature("Подогрев сидений", "Передние и задние сиденья", "Комфорт"),
                createFeature("Вентиляция сидений", "Передние сиденья", "Комфорт"),
                createFeature("Массаж сидений", "Передние сиденья с 5 режимами", "Комфорт"),
                createFeature("Адаптивный круиз-контроль", "С функцией Stop&Go", "Безопасность"),
                createFeature("Система ночного видения", "Распознавание пешеходов", "Безопасность"),
                createFeature("360° камеры", "Круговой обзор", "Безопасность"),
                createFeature("Парктроник", "Передний и задний", "Безопасность"),
                createFeature("Беспроводная зарядка", "Для смартфонов", "Мультимедиа"),
                createFeature("Apple CarPlay", "Беспроводное подключение", "Мультимедиа"),
                createFeature("Android Auto", "Беспроводное подключение", "Мультимедиа"),
                createFeature("Аудиосистема Bose", "14 динамиков", "Мультимедиа"),
                createFeature("Спортивные сиденья", "С усиленной боковой поддержкой", "Спорт"),
                createFeature("Спортивный режим", "Настройка подвески и двигателя", "Спорт"),
                createFeature("Лаунч-контроль", "Система быстрого старта", "Спорт")
        );

        featureRepository.saveAll(features);
        System.out.println("   ✅ Создано " + features.size() + " особенностей");
    }

    private void createDealerships() {
        if (dealershipRepository.count() > 0) {
            System.out.println("📊 Автосалоны уже существуют, пропускаем...");
            return;
        }

        System.out.println("\n🏢 СОЗДАНИЕ АВТОСАЛОНОВ:");

        List<Dealership> dealerships = Arrays.asList(
                createDealership("Автосалон Премиум", "г. Москва, ул. Тверская, 10", "+7 (495) 111-11-11"),
                createDealership("Автомир Юг", "г. Москва, ул. Южная, 25", "+7 (495) 222-22-22"),
                createDealership("СпортКар", "г. Москва, ул. Спортивная, 5", "+7 (495) 333-33-33"),
                createDealership("ЭкономАвто", "г. Москва, ул. Северная, 15", "+7 (495) 444-44-44"),
                createDealership("Дилерский Центр", "г. Москва, ул. Центральная, 30", "+7 (495) 555-55-55")
        );

        dealershipRepository.saveAll(dealerships);
        System.out.println("   ✅ Создано " + dealerships.size() + " автосалонов");
    }

    private void createCars() {
        if (carRepository.count() > 0) {
            System.out.println("📊 Машины уже существуют, пропускаем...");
            return;
        }

        System.out.println("\n🚙 СОЗДАНИЕ МАШИН:");

        List<Dealership> dealerships = dealershipRepository.findAll();
        List<Feature> features = featureRepository.findAll();

        if (dealerships.isEmpty() || features.isEmpty()) {
            System.out.println("   ❌ Нет автосалонов или особенностей для создания машин!");
            return;
        }

        // ВАЖНО: Создаем машины и сохраняем их по одной с синхронизацией
        System.out.println("   📋 Доступные фичи:");
        for (int i = 0; i < features.size(); i++) {
            System.out.println("      [" + i + "] " + features.get(i).getId() + ": " +
                    features.get(i).getName() + " (" + features.get(i).getCategory() + ")");
        }

        // Mercedes S-Class (id 1)
        Car car1 = new Car();
        car1.setBrand("Mercedes");
        car1.setModel("S-Class");
        car1.setYear(2024);
        car1.setColor("Black");
        car1.setPrice(150000);
        car1.setDealership(dealerships.get(0));
        car1 = carRepository.save(car1);  // Сначала сохраняем машину

        // Добавляем фичи
        car1.addFeature(features.get(0));  // Полный привод
        car1.addFeature(features.get(1));  // Панорамная крыша
        car1.addFeature(features.get(2));  // Автопилот
        car1.addFeature(features.get(3));  // Кожаный салон
        car1.addFeature(features.get(4));  // Подогрев сидений
        car1.addFeature(features.get(7));  // Адаптивный круиз-контроль
        car1.addFeature(features.get(9));  // 360° камеры
        car1.addFeature(features.get(14)); // Аудиосистема Bose
        carRepository.save(car1);  // Сохраняем с фичами
        System.out.println("   ✅ Mercedes S-Class: добавлено фич: " + car1.getFeatures().size());

        // BMW 7 Series (id 2)
        Car car2 = new Car();
        car2.setBrand("BMW");
        car2.setModel("7 Series");
        car2.setYear(2024);
        car2.setColor("Dark Blue");
        car2.setPrice(140000);
        car2.setDealership(dealerships.get(0));
        car2 = carRepository.save(car2);

        car2.addFeature(features.get(0));  // Полный привод
        car2.addFeature(features.get(1));  // Панорамная крыша
        car2.addFeature(features.get(3));  // Кожаный салон
        car2.addFeature(features.get(4));  // Подогрев сидений
        car2.addFeature(features.get(7));  // Адаптивный круиз-контроль
        car2.addFeature(features.get(8));  // Система ночного видения
        car2.addFeature(features.get(9));  // 360° камеры
        car2.addFeature(features.get(12)); // Apple CarPlay
        carRepository.save(car2);
        System.out.println("   ✅ BMW 7 Series: добавлено фич: " + car2.getFeatures().size());

        // Audi A8 (id 3)
        Car car3 = new Car();
        car3.setBrand("Audi");
        car3.setModel("A8");
        car3.setYear(2024);
        car3.setColor("Silver");
        car3.setPrice(135000);
        car3.setDealership(dealerships.get(0));
        car3 = carRepository.save(car3);

        car3.addFeature(features.get(0));  // Полный привод
        car3.addFeature(features.get(1));  // Панорамная крыша
        car3.addFeature(features.get(3));  // Кожаный салон
        car3.addFeature(features.get(5));  // Вентиляция сидений
        car3.addFeature(features.get(7));  // Адаптивный круиз-контроль
        car3.addFeature(features.get(9));  // 360° камеры
        car3.addFeature(features.get(13)); // Android Auto
        car3.addFeature(features.get(14)); // Аудиосистема Bose
        carRepository.save(car3);
        System.out.println("   ✅ Audi A8: добавлено фич: " + car3.getFeatures().size());

        // Porsche 911 Turbo S (id 4)
        Car car4 = new Car();
        car4.setBrand("Porsche");
        car4.setModel("911 Turbo S");
        car4.setYear(2024);
        car4.setColor("Red");
        car4.setPrice(250000);
        car4.setDealership(dealerships.get(2));
        car4 = carRepository.save(car4);

        car4.addFeature(features.get(0));  // Полный привод
        car4.addFeature(features.get(15)); // Спортивные сиденья
        car4.addFeature(features.get(16)); // Спортивный режим
        car4.addFeature(features.get(17)); // Лаунч-контроль
        car4.addFeature(features.get(7));  // Адаптивный круиз-контроль
        car4.addFeature(features.get(9));  // 360° камеры
        car4.addFeature(features.get(14)); // Аудиосистема Bose
        carRepository.save(car4);
        System.out.println("   ✅ Porsche 911: добавлено фич: " + car4.getFeatures().size());

        // BMW M5 Competition (id 5)
        Car car5 = new Car();
        car5.setBrand("BMW");
        car5.setModel("M5 Competition");
        car5.setYear(2024);
        car5.setColor("Blue");
        car5.setPrice(120000);
        car5.setDealership(dealerships.get(2));
        car5 = carRepository.save(car5);

        car5.addFeature(features.get(0));  // Полный привод
        car5.addFeature(features.get(15)); // Спортивные сиденья
        car5.addFeature(features.get(16)); // Спортивный режим
        car5.addFeature(features.get(17)); // Лаунч-контроль
        car5.addFeature(features.get(3));  // Кожаный салон
        car5.addFeature(features.get(4));  // Подогрев сидений
        car5.addFeature(features.get(14)); // Аудиосистема Bose
        carRepository.save(car5);
        System.out.println("   ✅ BMW M5: добавлено фич: " + car5.getFeatures().size());

        // Audi RS7 (id 6)
        Car car6 = new Car();
        car6.setBrand("Audi");
        car6.setModel("RS7");
        car6.setYear(2024);
        car6.setColor("Gray");
        car6.setPrice(115000);
        car6.setDealership(dealerships.get(2));
        car6 = carRepository.save(car6);

        car6.addFeature(features.get(0));  // Полный привод
        car6.addFeature(features.get(15)); // Спортивные сиденья
        car6.addFeature(features.get(16)); // Спортивный режим
        car6.addFeature(features.get(17)); // Лаунч-контроль
        car6.addFeature(features.get(3));  // Кожаный салон
        car6.addFeature(features.get(9));  // 360° камеры
        car6.addFeature(features.get(13)); // Android Auto
        carRepository.save(car6);
        System.out.println("   ✅ Audi RS7: добавлено фич: " + car6.getFeatures().size());

        // Toyota Camry (id 7)
        Car car7 = new Car();
        car7.setBrand("Toyota");
        car7.setModel("Camry");
        car7.setYear(2024);
        car7.setColor("White");
        car7.setPrice(35000);
        car7.setDealership(dealerships.get(3));
        car7 = carRepository.save(car7);

        car7.addFeature(features.get(4));  // Подогрев сидений
        car7.addFeature(features.get(5));  // Вентиляция сидений
        car7.addFeature(features.get(10)); // Парктроник
        car7.addFeature(features.get(11)); // Беспроводная зарядка
        carRepository.save(car7);
        System.out.println("   ✅ Toyota Camry: добавлено фич: " + car7.getFeatures().size());

        // Kia K5 (id 8)
        Car car8 = new Car();
        car8.setBrand("Kia");
        car8.setModel("K5");
        car8.setYear(2024);
        car8.setColor("Gray");
        car8.setPrice(32000);
        car8.setDealership(dealerships.get(3));
        car8 = carRepository.save(car8);

        car8.addFeature(features.get(4));  // Подогрев сидений
        car8.addFeature(features.get(10)); // Парктроник
        car8.addFeature(features.get(11)); // Беспроводная зарядка
        car8.addFeature(features.get(12)); // Apple CarPlay
        carRepository.save(car8);
        System.out.println("   ✅ Kia K5: добавлено фич: " + car8.getFeatures().size());

        // Hyundai Sonata (id 9)
        Car car9 = new Car();
        car9.setBrand("Hyundai");
        car9.setModel("Sonata");
        car9.setYear(2024);
        car9.setColor("Blue");
        car9.setPrice(30000);
        car9.setDealership(dealerships.get(3));
        car9 = carRepository.save(car9);

        car9.addFeature(features.get(4));  // Подогрев сидений
        car9.addFeature(features.get(10)); // Парктроник
        car9.addFeature(features.get(11)); // Беспроводная зарядка
        car9.addFeature(features.get(13)); // Android Auto
        carRepository.save(car9);
        System.out.println("   ✅ Hyundai Sonata: добавлено фич: " + car9.getFeatures().size());

        // Toyota Land Cruiser 300 (id 10)
        Car car10 = new Car();
        car10.setBrand("Toyota");
        car10.setModel("Land Cruiser 300");
        car10.setYear(2024);
        car10.setColor("White");
        car10.setPrice(100000);
        car10.setDealership(dealerships.get(1));
        car10 = carRepository.save(car10);

        car10.addFeature(features.get(0));  // Полный привод
        car10.addFeature(features.get(1));  // Панорамная крыша
        car10.addFeature(features.get(3));  // Кожаный салон
        car10.addFeature(features.get(4));  // Подогрев сидений
        car10.addFeature(features.get(7));  // Адаптивный круиз-контроль
        car10.addFeature(features.get(8));  // Система ночного видения
        car10.addFeature(features.get(9));  // 360° камеры
        car10.addFeature(features.get(14)); // Аудиосистема Bose
        carRepository.save(car10);
        System.out.println("   ✅ Toyota LC300: добавлено фич: " + car10.getFeatures().size());

        // Lexus LX600 (id 11)
        Car car11 = new Car();
        car11.setBrand("Lexus");
        car11.setModel("LX600");
        car11.setYear(2024);
        car11.setColor("Silver");
        car11.setPrice(110000);
        car11.setDealership(dealerships.get(1));
        car11 = carRepository.save(car11);

        car11.addFeature(features.get(0));  // Полный привод
        car11.addFeature(features.get(1));  // Панорамная крыша
        car11.addFeature(features.get(3));  // Кожаный салон
        car11.addFeature(features.get(4));  // Подогрев сидений
        car11.addFeature(features.get(5));  // Вентиляция сидений
        car11.addFeature(features.get(7));  // Адаптивный круиз-контроль
        car11.addFeature(features.get(9));  // 360° камеры
        car11.addFeature(features.get(14)); // Аудиосистема Bose
        carRepository.save(car11);
        System.out.println("   ✅ Lexus LX600: добавлено фич: " + car11.getFeatures().size());

        // Volvo XC90 (id 12)
        Car car12 = new Car();
        car12.setBrand("Volvo");
        car12.setModel("XC90");
        car12.setYear(2024);
        car12.setColor("Dark Gray");
        car12.setPrice(85000);
        car12.setDealership(dealerships.get(4));
        car12 = carRepository.save(car12);

        car12.addFeature(features.get(0));  // Полный привод
        car12.addFeature(features.get(1));  // Панорамная крыша
        car12.addFeature(features.get(2));  // Автопилот
        car12.addFeature(features.get(3));  // Кожаный салон
        car12.addFeature(features.get(4));  // Подогрев сидений
        car12.addFeature(features.get(7));  // Адаптивный круиз-контроль
        car12.addFeature(features.get(8));  // Система ночного видения
        car12.addFeature(features.get(9));  // 360° камеры
        carRepository.save(car12);
        System.out.println("   ✅ Volvo XC90: добавлено фич: " + car12.getFeatures().size());

        // Range Rover Sport (id 13)
        Car car13 = new Car();
        car13.setBrand("Range Rover");
        car13.setModel("Sport");
        car13.setYear(2024);
        car13.setColor("Black");
        car13.setPrice(120000);
        car13.setDealership(dealerships.get(4));
        car13 = carRepository.save(car13);

        car13.addFeature(features.get(0));  // Полный привод
        car13.addFeature(features.get(1));  // Панорамная крыша
        car13.addFeature(features.get(2));  // Автопилот
        car13.addFeature(features.get(3));  // Кожаный салон
        car13.addFeature(features.get(4));  // Подогрев сидений
        car13.addFeature(features.get(5));  // Вентиляция сидений
        car13.addFeature(features.get(7));  // Адаптивный круиз-контроль
        car13.addFeature(features.get(9));  // 360° камеры
        carRepository.save(car13);
        System.out.println("   ✅ Range Rover Sport: добавлено фич: " + car13.getFeatures().size());

        System.out.println("\n   🎉 Все машины созданы с фичами!");
    }

    private void verifyFeatures() {
        System.out.println("\n🔍 ПРОВЕРКА СВЯЗЕЙ CAR-FEATURE:");
        List<Car> cars = carRepository.findAll();
        for (Car car : cars) {
            System.out.println("   " + car.getBrand() + " " + car.getModel() +
                    ": фич = " + car.getFeatures().size());
            for (Feature f : car.getFeatures()) {
                System.out.println("      - " + f.getName());
            }
        }
    }

    private void ensureCarFeatures() {
        System.out.println("\n🔗 ПРОВЕРКА/ВОССТАНОВЛЕНИЕ СВЯЗЕЙ CAR-FEATURE:");

        List<Feature> allFeatures = featureRepository.findAll();
        if (allFeatures.isEmpty()) {
            System.out.println("   ❌ Особенности отсутствуют, нечего привязывать.");
            return;
        }

        Map<String, Feature> featuresByName = allFeatures.stream()
                .collect(Collectors.toMap(Feature::getName, Function.identity(), (a, b) -> a));

        List<Car> cars = carRepository.findAll(); // @EntityGraph загрузит features, если они есть
        if (cars.isEmpty()) {
            System.out.println("   ❌ Машины отсутствуют, нечего привязывать.");
            return;
        }

        int updated = 0;
        for (Car car : cars) {
            if (car.getFeatures() != null && !car.getFeatures().isEmpty()) {
                continue;
            }

            List<Feature> toAdd = getFeaturesForCar(car, featuresByName);
            if (toAdd.isEmpty()) {
                continue;
            }

            for (Feature f : toAdd) {
                car.addFeature(f);
            }
            updated++;
        }

        if (updated > 0) {
            carRepository.saveAll(cars);
            System.out.println("   ✅ Восстановлены связи для машин: " + updated);
        } else {
            System.out.println("   ✅ Все машины уже имеют фичи (или нет правил привязки).");
        }
    }

    private List<Feature> getFeaturesForCar(Car car, Map<String, Feature> featuresByName) {
        if (car.getBrand() == null || car.getModel() == null) {
            return List.of();
        }

        String brand = car.getBrand().trim().toLowerCase();
        String model = car.getModel().trim().toLowerCase();

        // Подбираем фичи по тем же правилам, что и при первичном создании в createCars()
        if (brand.equals("toyota") && model.equals("camry")) {
            return pick(featuresByName, "Подогрев сидений", "Вентиляция сидений", "Парктроник", "Беспроводная зарядка");
        }
        if (brand.equals("kia") && model.equals("k5")) {
            return pick(featuresByName, "Подогрев сидений", "Парктроник", "Беспроводная зарядка", "Apple CarPlay");
        }
        if (brand.equals("hyundai") && model.equals("sonata")) {
            return pick(featuresByName, "Подогрев сидений", "Парктроник", "Беспроводная зарядка", "Android Auto");
        }
        if (brand.equals("mercedes") && model.equals("s-class")) {
            return pick(featuresByName,
                    "Полный привод", "Панорамная крыша", "Автопилот", "Кожаный салон", "Подогрев сидений",
                    "Адаптивный круиз-контроль", "360° камеры", "Аудиосистема Bose");
        }
        if (brand.equals("bmw") && model.equals("7 series")) {
            return pick(featuresByName,
                    "Полный привод", "Панорамная крыша", "Кожаный салон", "Подогрев сидений",
                    "Адаптивный круиз-контроль", "Система ночного видения", "360° камеры", "Apple CarPlay");
        }
        if (brand.equals("audi") && model.equals("a8")) {
            return pick(featuresByName,
                    "Полный привод", "Панорамная крыша", "Кожаный салон", "Вентиляция сидений",
                    "Адаптивный круиз-контроль", "360° камеры", "Android Auto", "Аудиосистема Bose");
        }
        if (brand.equals("porsche") && model.equals("911 turbo s")) {
            return pick(featuresByName,
                    "Полный привод", "Спортивные сиденья", "Спортивный режим", "Лаунч-контроль",
                    "Адаптивный круиз-контроль", "360° камеры", "Аудиосистема Bose");
        }
        if (brand.equals("bmw") && model.equals("m5 competition")) {
            return pick(featuresByName,
                    "Полный привод", "Спортивные сиденья", "Спортивный режим", "Лаунч-контроль",
                    "Кожаный салон", "Подогрев сидений", "Аудиосистема Bose");
        }
        if (brand.equals("audi") && model.equals("rs7")) {
            return pick(featuresByName,
                    "Полный привод", "Спортивные сиденья", "Спортивный режим", "Лаунч-контроль",
                    "Кожаный салон", "360° камеры", "Android Auto");
        }
        if (brand.equals("toyota") && model.equals("land cruiser 300")) {
            return pick(featuresByName,
                    "Полный привод", "Панорамная крыша", "Кожаный салон", "Подогрев сидений",
                    "Адаптивный круиз-контроль", "Система ночного видения", "360° камеры", "Аудиосистема Bose");
        }
        if (brand.equals("lexus") && model.equals("lx600")) {
            return pick(featuresByName,
                    "Полный привод", "Панорамная крыша", "Кожаный салон", "Подогрев сидений", "Вентиляция сидений",
                    "Адаптивный круиз-контроль", "360° камеры", "Аудиосистема Bose");
        }
        if (brand.equals("volvo") && model.equals("xc90")) {
            return pick(featuresByName,
                    "Полный привод", "Панорамная крыша", "Автопилот", "Кожаный салон", "Подогрев сидений",
                    "Адаптивный круиз-контроль", "Система ночного видения", "360° камеры");
        }
        if (brand.equals("range rover") && model.equals("sport")) {
            return pick(featuresByName,
                    "Полный привод", "Панорамная крыша", "Автопилот", "Кожаный салон", "Подогрев сидений", "Вентиляция сидений",
                    "Адаптивный круиз-контроль", "360° камеры");
        }

        return List.of();
    }

    private List<Feature> pick(Map<String, Feature> featuresByName, String... names) {
        return Arrays.stream(names)
                .map(featuresByName::get)
                .filter(f -> f != null)
                .toList();
    }

    private void createCustomers() {
        if (customerRepository.count() > 0) {
            System.out.println("📊 Покупатели уже существуют, пропускаем...");
            return;
        }

        System.out.println("\n👥 СОЗДАНИЕ ПОКУПАТЕЛЕЙ:");

        List<Customer> customers = Arrays.asList(
                createCustomer("Иван", "Иванов", "ivan.ivanov@email.com", "+7 (901) 111-11-11"),
                createCustomer("Петр", "Петров", "petr.petrov@email.com", "+7 (902) 222-22-22"),
                createCustomer("Сергей", "Сергеев", "sergey@email.com", "+7 (903) 333-33-33"),
                createCustomer("Анна", "Смирнова", "anna.smirnova@email.com", "+7 (904) 444-44-44"),
                createCustomer("Елена", "Козлова", "elena.kozlova@email.com", "+7 (905) 555-55-55"),
                createCustomer("Дмитрий", "Морозов", "dmitry.morozov@email.com", "+7 (906) 666-66-66"),
                createCustomer("Ольга", "Волкова", "olga.volkova@email.com", "+7 (907) 777-77-77"),
                createCustomer("Алексей", "Соколов", "alexey.sokolov@email.com", "+7 (908) 888-88-88"),
                createCustomer("Татьяна", "Михайлова", "tatiana@email.com", "+7 (909) 999-99-99"),
                createCustomer("Николай", "Николаев", "nikolay@email.com", "+7 (910) 000-00-00")
        );

        customerRepository.saveAll(customers);
        System.out.println("   ✅ Создано " + customers.size() + " покупателей");
    }

    private void createSales() {
        if (saleRepository.count() > 0) {
            System.out.println("📊 Продажи уже существуют, пропускаем...");
            return;
        }

        System.out.println("\n💰 СОЗДАНИЕ ПРОДАЖ:");

        List<Car> cars = carRepository.findAll();
        List<Customer> customers = customerRepository.findAll();

        if (cars.isEmpty() || customers.isEmpty()) {
            System.out.println("   ❌ Нет машин или покупателей для создания продаж!");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        List<Sale> sales = Arrays.asList(
                createSale(cars.get(0), customers.get(0), now.minusDays(14), 148000),
                createSale(cars.get(1), customers.get(1), now.minusDays(13), 138000),
                createSale(cars.get(2), customers.get(2), now.minusDays(12), 133000),
                createSale(cars.get(3), customers.get(3), now.minusDays(11), 245000),
                createSale(cars.get(4), customers.get(4), now.minusDays(10), 118000),
                createSale(cars.get(5), customers.get(5), now.minusDays(6), 113000),
                createSale(cars.get(6), customers.get(6), now.minusDays(5), 34000),
                createSale(cars.get(7), customers.get(7), now.minusDays(4), 31000),
                createSale(cars.get(8), customers.get(8), now.minusDays(3), 29500),
                createSale(cars.get(9), customers.get(9), now.minusDays(2), 98000),
                createSale(cars.get(10), customers.get(0), now.minusDays(1), 108000),
                createSale(cars.get(11), customers.get(1), now, 83000)
        );

        saleRepository.saveAll(sales);
        System.out.println("   ✅ Создано " + sales.size() + " продаж");
    }

    private void printStatistics() {
        System.out.println("\n📊 СТАТИСТИКА БАЗЫ ДАННЫХ:");
        System.out.println("   Особенности: " + featureRepository.count());
        System.out.println("   Автосалоны: " + dealershipRepository.count());
        System.out.println("   Машины: " + carRepository.count());
        System.out.println("   Покупатели: " + customerRepository.count());
        System.out.println("   Продажи: " + saleRepository.count());
    }

    private Feature createFeature(String name, String description, String category) {
        Feature feature = new Feature();
        feature.setName(name);
        feature.setDescription(description);
        feature.setCategory(category);
        return feature;
    }

    private Dealership createDealership(String name, String address, String phone) {
        Dealership dealership = new Dealership();
        dealership.setName(name);
        dealership.setAddress(address);
        dealership.setPhone(phone);
        return dealership;
    }

    private Customer createCustomer(String firstName, String lastName, String email, String phone) {
        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setPhone(phone);
        return customer;
    }

    private Sale createSale(Car car, Customer customer, LocalDateTime date, double price) {
        Sale sale = new Sale();
        sale.setCar(car);
        sale.setCustomer(customer);
        sale.setSaleDate(date);
        sale.setSalePrice(price);
        return sale;
    }
}
```

src/main/java/com/example/autosalon/controller/CarController.java
```java
package com.example.autosalon.controller;

import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.entity.Car;
import com.example.autosalon.mapper.CarMapper;
import com.example.autosalon.service.CarService;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;
    private final CarMapper carMapper;

    @GetMapping
    public ResponseEntity<List<CarResponseDto>> getCars(
            @RequestParam(required = false) String brand) {
        List<Car> cars;
        if (brand != null) {
            cars = carService.getCarsByBrand(brand);
        } else {
            cars = carService.getAllCars();
        }

        List<CarResponseDto> responseDtos = cars.stream()
                .map(carMapper::toResponseDto)
                .sorted(Comparator.comparing(CarResponseDto::getId))
                .toList();

        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarResponseDto> getCarById(@PathVariable Long id) {
        Car car = carService.getCarById(id);
        CarResponseDto responseDto = carMapper.toResponseDto(car);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping
    public ResponseEntity<CarResponseDto> createCar(@RequestBody CarRequestDto createDto) {
        Car car = carMapper.toEntity(createDto);
        Car savedCar = carService.createCar(car);
        CarResponseDto responseDto = carMapper.toResponseDto(savedCar);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarResponseDto> updateCar(
            @PathVariable Long id,
            @RequestBody CarRequestDto updateDto) {
        Car carDetails = carMapper.toEntity(updateDto);
        Car updatedCar = carService.updateCar(id, carDetails);
        CarResponseDto responseDto = carMapper.toResponseDto(updatedCar);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCar(@PathVariable Long id) {
        carService.deleteCar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Демонстрация проблемы N+1
     * GET /api/cars/features/problem
     */
    @GetMapping("/features/problem")
    public ResponseEntity<List<Car>> demonstrateNplusOneProblem() {
        List<Car> cars = carService.getCarsWithNplusOneProblem();
        cars.sort(Comparator.comparing(Car::getId));
        return ResponseEntity.ok(cars);
    }

    /**
     * Демонстрация решения с @EntityGraph
     * GET /api/cars/features/solution
     */
    @GetMapping("/features/solution")
    public ResponseEntity<List<Car>> demonstrateSolution() {
        List<Car> cars = carService.getCarsWithSolution();
        cars.sort(Comparator.comparing(Car::getId));
        return ResponseEntity.ok(cars);
    }
}
```

src/main/java/com/example/autosalon/controller/CustomerController.java
```java
package com.example.autosalon.controller;

import com.example.autosalon.dto.CustomerRequestDto;
import com.example.autosalon.dto.CustomerResponseDto;
import com.example.autosalon.entity.Customer;
import com.example.autosalon.mapper.CustomerMapper;
import com.example.autosalon.service.CustomerService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    @GetMapping
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerResponseDto> response = customers.stream()
                .map(customerMapper::toResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customerMapper.toResponseDto(customer));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<CustomerResponseDto> getCustomerByEmail(@PathVariable String email) {
        Customer customer = customerService.getCustomerByEmail(email);
        return ResponseEntity.ok(customerMapper.toResponseDto(customer));
    }

    @PostMapping
    public ResponseEntity<CustomerResponseDto> createCustomer(
            @RequestBody CustomerRequestDto requestDto) {
        Customer customer = customerMapper.toEntity(requestDto);
        Customer createdCustomer = customerService.createCustomer(customer);
        CustomerResponseDto responseDto = customerMapper.toResponseDto(createdCustomer);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> updateCustomer(
            @PathVariable Long id,
            @RequestBody CustomerRequestDto requestDto) {
        Customer customerDetails = customerMapper.toEntity(requestDto);
        Customer updatedCustomer = customerService.updateCustomer(id, customerDetails);
        CustomerResponseDto responseDto = customerMapper.toResponseDto(updatedCustomer);
        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/{id}/phone")
    public ResponseEntity<CustomerResponseDto> updateCustomerPhone(
            @PathVariable Long id,
            @RequestParam String phone) {
        Customer updatedCustomer = customerService.updatePhone(id, phone);
        CustomerResponseDto responseDto = customerMapper.toResponseDto(updatedCustomer);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
```

src/main/java/com/example/autosalon/controller/DealershipController.java
```java
package com.example.autosalon.controller;

import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.dto.DealershipRequestDto;
import com.example.autosalon.dto.DealershipResponseDto;
import com.example.autosalon.dto.DealershipWithCarsRequest;
import com.example.autosalon.dto.DealershipWithCarsResponseDto;
import com.example.autosalon.entity.Dealership;
import com.example.autosalon.mapper.CarMapper;
import com.example.autosalon.mapper.DealershipMapper;
import com.example.autosalon.service.DealershipService;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/dealerships")
@RequiredArgsConstructor
public class DealershipController {

    private final DealershipService dealershipService;
    private final DealershipMapper dealershipMapper;
    private final CarMapper carMapper;

    /**
     * Получить все автосалоны
     * GET /api/dealerships
     */
    @GetMapping
    public ResponseEntity<List<DealershipResponseDto>> getAllDealerships() {
        log.info("GET /api/dealerships - получение всех автосалонов");
        List<Dealership> dealerships = dealershipService.getAllDealerships();


        List<DealershipResponseDto> response = dealerships.stream()
                .sorted(Comparator.comparing(Dealership::getId))
                .map(dealershipMapper::toResponseDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Получить автосалон по ID
     * GET /api/dealerships/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<DealershipResponseDto> getDealershipById(@PathVariable Long id) {
        log.info("GET /api/dealerships/{} - получение автосалона", id);
        Dealership dealership = dealershipService.getDealershipById(id);
        return ResponseEntity.ok(dealershipMapper.toResponseDto(dealership));
    }

    /**
     * Получить автосалон с машинами
     * GET /api/dealerships/{id}/with-cars
     */
    @GetMapping("/{id}/with-cars")
    public ResponseEntity<DealershipWithCarsResponseDto> getDealershipWithCars(
            @PathVariable Long id) {
        log.info(
                "GET /api/dealerships/{}/with-cars - получение салона с машинами",
                id);
        Dealership dealership = dealershipService.getDealershipWithCars(id);
        return ResponseEntity.ok(dealershipMapper.toWithCarsResponseDto(dealership));
    }

    /**
     * Создать новый автосалон
     * POST /api/dealerships
     */
    @PostMapping
    public ResponseEntity<DealershipResponseDto> createDealership(
            @RequestBody DealershipRequestDto requestDto) {
        log.info(
                "POST /api/dealerships - создание нового автосалона: {}",
                requestDto.getName());
        Dealership created = dealershipService.createDealership(
                dealershipMapper.toEntity(requestDto));
        return new ResponseEntity<>(
                dealershipMapper.toResponseDto(created),
                HttpStatus.CREATED);
    }

    /**
     * Полностью обновить автосалон
     * PUT /api/dealerships/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<DealershipResponseDto> updateDealership(
            @PathVariable Long id,
            @RequestBody DealershipRequestDto requestDto) {
        log.info("PUT /api/dealerships/{} - обновление автосалона", id);
        Dealership updated = dealershipService.updateDealership(
                id,
                dealershipMapper.toEntity(requestDto));
        return ResponseEntity.ok(dealershipMapper.toResponseDto(updated));
    }

    /**
     * УДАЛИТЬ автосалон по ID
     * DELETE /api/dealerships/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDealership(@PathVariable Long id) {
        log.info(
                "DELETE /api/dealerships/{} - удаление автосалона",
                id);
        dealershipService.deleteDealership(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Добавить машину в автосалон
     * POST /api/dealerships/{dealershipId}/cars/{carId}
     */
    @PostMapping("/{dealershipId}/cars/{carId}")
    public ResponseEntity<DealershipWithCarsResponseDto> addCarToDealership(
            @PathVariable Long dealershipId,
            @PathVariable Long carId) {
        log.info(
                "POST /api/dealerships/{}/cars/{} - добавление машины в салон",
                dealershipId,
                carId);
        Dealership dealership = dealershipService.addCarToDealership(
                dealershipId,
                carId);
        return ResponseEntity.ok(dealershipMapper.toWithCarsResponseDto(dealership));
    }

    /**
     * УДАЛИТЬ машину из автосалона
     * DELETE /api/dealerships/{dealershipId}/cars/{carId}
     */
    @DeleteMapping("/{dealershipId}/cars/{carId}")
    public ResponseEntity<DealershipWithCarsResponseDto> removeCarFromDealership(
            @PathVariable Long dealershipId,
            @PathVariable Long carId) {
        log.info(
                "DELETE /api/dealerships/{}/cars/{} - удаление машины из салона",
                dealershipId,
                carId);
        Dealership dealership = dealershipService.removeCarFromDealership(
                dealershipId,
                carId);
        return ResponseEntity.ok(dealershipMapper.toWithCarsResponseDto(dealership));
    }



    /**
     * Демонстрация БЕЗ транзакции (частичное сохранение)
     * POST /api/dealerships/without-transaction
     */
    @PostMapping("/without-transaction")
    public String createWithoutTransaction(@RequestBody DealershipWithCarsRequest request) {
        log.info(
                "\n=== ПОЛУЧЕН ЗАПРОС: /api/dealerships/without-transaction ===");
        long beforeCount = dealershipService.countDealerships();
        log.info(" До операции: салонов в БД = {}", beforeCount);

        try {
            List<CarRequestDto> cars = request.getCars() == null
                    ? Collections.emptyList()
                    : request.getCars();
            Dealership saved =
                    dealershipService.createDealershipWithCarsWithoutTransaction(
                            dealershipMapper.toEntity(request.getDealership()),
                            cars.stream()
                                    .map(carMapper::toEntity)
                                    .toList());

            long afterCount = dealershipService.countDealerships();
            return String.format(
                    " УСПЕХ! (хотя не должно было) Салонов было: %d, стало: %d. "
                            + "Салон '%s' сохранен! (Проблема: машины не сохранились, "
                            + "но салон остался)",
                    beforeCount, afterCount, saved.getName()
            );

        } catch (Exception e) {
            long afterCount = dealershipService.countDealerships();
            log.error(
                    "Ошибка при сохранении: {}",
                    e.getMessage());
            return String.format(
                    "️ ОШИБКА: %s%n Салонов было: %d, стало: %d. "
                            + "(Данные сохранились частично - салон остался в БД!)",
                    e.getMessage(), beforeCount, afterCount
            );
        }
    }

    /**
     * Демонстрация С транзакцией (полный откат)
     * POST /api/dealerships/with-transaction
     */
    @PostMapping("/with-transaction")
    public String createWithTransaction(@RequestBody DealershipWithCarsRequest request) {
        log.info(
                "\n=== ПОЛУЧЕН ЗАПРОС: /api/dealerships/with-transaction ===");
        long beforeCount = dealershipService.countDealerships();
        log.info(" До операции: салонов в БД = {}", beforeCount);

        try {
            List<CarRequestDto> cars = request.getCars() == null
                    ? Collections.emptyList()
                    : request.getCars();
            dealershipService.createDealershipWithCarsWithTransaction(
                    dealershipMapper.toEntity(request.getDealership()),
                    cars.stream()
                            .map(carMapper::toEntity)
                            .toList());

            long afterCount = dealershipService.countDealerships();
            return String.format(
                    " УСПЕХ! (хотя не должно было) Салонов было: %d, стало: %d. "
                            + "(Этого не должно произойти с @Transactional!)",
                    beforeCount, afterCount
            );

        } catch (Exception e) {
            long afterCount = dealershipService.countDealerships();
            log.error(
                    "Ошибка при сохранении, транзакция откатилась: {}",
                    e.getMessage());
            return String.format(
                    " ОТКАТ: %s%n Салонов было: %d, стало: %d. "
                            + "Отлично! Транзакция сработала - салон НЕ сохранился!",
                    e.getMessage(), beforeCount, afterCount
            );
        }
    }
}
```

src/main/java/com/example/autosalon/controller/FeatureController.java
```java
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
import org.springframework.web.bind.annotation.RestController;

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
```

src/main/java/com/example/autosalon/controller/SaleController.java
```java
package com.example.autosalon.controller;

import com.example.autosalon.dto.SaleRequestDto;
import com.example.autosalon.dto.SaleResponseDto;
import com.example.autosalon.entity.Sale;
import com.example.autosalon.mapper.SaleMapper;
import com.example.autosalon.service.SaleService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final SaleMapper saleMapper;

    @GetMapping
    public ResponseEntity<List<SaleResponseDto>> getAllSales() {
        List<Sale> sales = saleService.getAllSales();
        List<SaleResponseDto> responseDtos = sales.stream()
                .map(saleMapper::toResponseDto)
                .sorted(Comparator.comparing(SaleResponseDto::getId))
                .toList();
        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleResponseDto> getSaleById(@PathVariable Long id) {
        Sale sale = saleService.getSaleById(id);
        return ResponseEntity.ok(saleMapper.toResponseDto(sale));
    }

    @GetMapping("/car/{carId}")
    public ResponseEntity<SaleResponseDto> getSaleByCarId(@PathVariable Long carId) {
        Sale sale = saleService.getSaleByCarId(carId);
        return ResponseEntity.ok(saleMapper.toResponseDto(sale));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<SaleResponseDto>> getSalesByCustomerId(
            @PathVariable Long customerId) {
        List<Sale> sales = saleService.getSalesByCustomerId(customerId);
        List<SaleResponseDto> responseDtos = sales.stream()
                .map(saleMapper::toResponseDto)
                .sorted(Comparator.comparing(SaleResponseDto::getId))
                .toList();
        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<SaleResponseDto>> getSalesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<Sale> sales = saleService.getSalesByDateRange(start, end);
        List<SaleResponseDto> responseDtos = sales.stream()
                .map(saleMapper::toResponseDto)
                .sorted(Comparator.comparing(SaleResponseDto::getId))
                .toList();
        return ResponseEntity.ok(responseDtos);
    }

    @PostMapping
    public ResponseEntity<SaleResponseDto> createSale(@RequestBody SaleRequestDto createDto) {
        Sale sale = saleMapper.toEntity(createDto);
        Sale savedSale = saleService.createSale(sale);
        SaleResponseDto responseDto = saleMapper.toResponseDto(savedSale);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SaleResponseDto> updateSale(
            @PathVariable Long id,
            @RequestBody SaleRequestDto updateDto) {
        Sale saleDetails = saleMapper.toEntity(updateDto);
        Sale updatedSale = saleService.updateSale(id, saleDetails);
        return ResponseEntity.ok(saleMapper.toResponseDto(updatedSale));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) {
        saleService.deleteSale(id);
        return ResponseEntity.noContent().build();
    }
}
```

src/main/java/com/example/autosalon/dto/CarRequestDto.java
```java
package com.example.autosalon.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarRequestDto {
    private String brand;
    private String model;
    private int year;
    private String color;
    private double price;
    private List<Long> featureIds;
}
```

src/main/java/com/example/autosalon/dto/CarResponseDto.java
```java
package com.example.autosalon.dto;

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
    private double price;
}
```

src/main/java/com/example/autosalon/dto/CustomerRequestDto.java
```java
package com.example.autosalon.dto;

import lombok.Data;

@Data
public class CustomerRequestDto {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}
```

src/main/java/com/example/autosalon/dto/CustomerResponseDto.java
```java
package com.example.autosalon.dto;

import lombok.Data;

@Data
public class CustomerResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}
```

src/main/java/com/example/autosalon/dto/DealershipRequestDto.java
```java
package com.example.autosalon.dto;

import lombok.Data;

@Data
public class DealershipRequestDto {
    private String name;
    private String address;
    private String phone;
}
```

src/main/java/com/example/autosalon/dto/DealershipResponseDto.java
```java
package com.example.autosalon.dto;

import lombok.Data;

@Data
public class DealershipResponseDto {
    private Long id;
    private String name;
    private String address;
    private String phone;
}
```

src/main/java/com/example/autosalon/dto/DealershipWithCarsRequest.java
```java
package com.example.autosalon.dto;

import java.util.List;
import lombok.Data;

@Data
public class DealershipWithCarsRequest {
    private DealershipRequestDto dealership;
    private List<CarRequestDto> cars;
}
```

src/main/java/com/example/autosalon/dto/DealershipWithCarsResponseDto.java
```java
package com.example.autosalon.dto;

import java.util.List;
import lombok.Data;

@Data
public class DealershipWithCarsResponseDto {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private List<CarResponseDto> cars;
}
```

src/main/java/com/example/autosalon/dto/FeatureRequestDto.java
```java
package com.example.autosalon.dto;

import lombok.Data;

@Data
public class FeatureRequestDto {
    private String name;
    private String description;
    private String category;
}
```

src/main/java/com/example/autosalon/dto/FeatureResponseDto.java
```java
package com.example.autosalon.dto;

import lombok.Data;

@Data
public class FeatureResponseDto {
    private Long id;
    private String name;
    private String description;
    private String category;
}
```

src/main/java/com/example/autosalon/dto/SaleRequestDto.java
```java
package com.example.autosalon.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequestDto {
    private Long carId;
    private Long customerId;
    private LocalDateTime saleDate;
    private double salePrice;
}
```

src/main/java/com/example/autosalon/dto/SaleResponseDto.java
```java
package com.example.autosalon.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleResponseDto {
    private Long id;
    private Long carId;
    private String carBrand;
    private String carModel;
    private Long customerId;
    private String customerName;
    private LocalDateTime saleDate;
    private double salePrice;
}
```

src/main/java/com/example/autosalon/entity/Car.java
```java
package com.example.autosalon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
    @JsonIgnore
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
```

src/main/java/com/example/autosalon/entity/Customer.java
```java
package com.example.autosalon.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Сущность "Покупатель"
 * Таблица: customers
 *
 * Связи:
 * 1. @OneToMany с Sale - один покупатель может совершить много покупок
 */
@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "sales")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", length = 150, unique = true)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sale> sales = new ArrayList<>();

    /**
     * Добавляет продажу покупателю
     * Устанавливает связь с двух сторон
     */
    public void addSale(Sale sale) {
        sales.add(sale);
        sale.setCustomer(this);
    }

    /**
     * Удаляет продажу у покупателя
     * Убирает связь с двух сторон
     */
    public void removeSale(Sale sale) {
        sales.remove(sale);
        sale.setCustomer(null);
    }
}
```

src/main/java/com/example/autosalon/entity/Dealership.java
```java
package com.example.autosalon.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Сущность "Автосалон"
 * Таблица: dealerships
 *
 * Связи:
 * 1. @OneToMany с Car - один автосалон имеет много машин
 */
@Entity
@Table(name = "dealerships")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "cars")
public class Dealership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @OneToMany(mappedBy = "dealership", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Car> cars = new ArrayList<>();

    /**
     * Добавляет машину в автосалон
     * Устанавливает связь с двух сторон
     */
    public void addCar(Car car) {
        cars.add(car);
        car.setDealership(this);
    }

    /**
     * Удаляет машину из автосалона
     * Убирает связь с двух сторон
     */
    public void removeCar(Car car) {
        cars.remove(car);
        car.setDealership(null);
    }
}
```

src/main/java/com/example/autosalon/entity/Feature.java
```java
package com.example.autosalon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "features")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "category")
    private String category;

    @ManyToMany(mappedBy = "features")
    @JsonIgnore
    private List<Car> cars = new ArrayList<>();
}
```

src/main/java/com/example/autosalon/entity/Sale.java
```java
package com.example.autosalon.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Сущность "Продажа"
 * Таблица: sales
 *
 * Связи:
 * 1. @OneToOne с Car - одна продажа относится к одной машине
 * 2. @ManyToOne с Customer - много продаж могут быть у одного покупателя
 */
@Entity
@Table(name = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"car", "customer"})
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", unique = true)
    private Car car;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "date", nullable = false)
    private LocalDateTime saleDate;

    @Column(name = "price")
    private double salePrice;
}
```

src/main/java/com/example/autosalon/mapper/CarMapper.java
```java
package com.example.autosalon.mapper;

import com.example.autosalon.dto.CarRequestDto;
import com.example.autosalon.dto.CarResponseDto;
import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Feature;
import com.example.autosalon.repository.FeatureRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarMapper {

    private final FeatureRepository featureRepository;

    public CarResponseDto toResponseDto(Car car) {
        if (car == null) {
            return null;
        }
        CarResponseDto dto = new CarResponseDto();
        dto.setId(car.getId());
        dto.setBrand(car.getBrand());
        dto.setModel(car.getModel());
        dto.setYear(car.getYear());
        dto.setColor(car.getColor());
        dto.setPrice(car.getPrice());
        return dto;
    }

    public Car toEntity(CarRequestDto dto) {
        if (dto == null) {
            return null;
        }
        Car car = new Car();
        car.setBrand(dto.getBrand());
        car.setModel(dto.getModel());
        car.setYear(dto.getYear());
        car.setColor(dto.getColor());
        car.setPrice(dto.getPrice());

        if (dto.getFeatureIds() != null && !dto.getFeatureIds().isEmpty()) {
            List<Feature> features = featureRepository.findAllById(dto.getFeatureIds());
            car.setFeatures(features);
        }

        return car;
    }
}
```

src/main/java/com/example/autosalon/mapper/CustomerMapper.java
```java
package com.example.autosalon.mapper;

import com.example.autosalon.dto.CustomerRequestDto;
import com.example.autosalon.dto.CustomerResponseDto;
import com.example.autosalon.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequestDto dto) {
        Customer customer = new Customer();
        customer.setFirstName(dto.getFirstName());
        customer.setLastName(dto.getLastName());
        customer.setEmail(dto.getEmail());
        customer.setPhone(dto.getPhone());
        return customer;
    }

    public CustomerResponseDto toResponseDto(Customer customer) {
        CustomerResponseDto dto = new CustomerResponseDto();
        dto.setId(customer.getId());
        dto.setFirstName(customer.getFirstName());
        dto.setLastName(customer.getLastName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        return dto;
    }
}
```

src/main/java/com/example/autosalon/mapper/DealershipMapper.java
```java
package com.example.autosalon.mapper;

import com.example.autosalon.dto.DealershipRequestDto;
import com.example.autosalon.dto.DealershipResponseDto;
import com.example.autosalon.dto.DealershipWithCarsResponseDto;
import com.example.autosalon.entity.Dealership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DealershipMapper {

    private final CarMapper carMapper;

    public Dealership toEntity(DealershipRequestDto dto) {
        Dealership dealership = new Dealership();
        dealership.setName(dto.getName());
        dealership.setAddress(dto.getAddress());
        dealership.setPhone(dto.getPhone());
        return dealership;
    }

    public DealershipResponseDto toResponseDto(Dealership dealership) {
        DealershipResponseDto dto = new DealershipResponseDto();
        dto.setId(dealership.getId());
        dto.setName(dealership.getName());
        dto.setAddress(dealership.getAddress());
        dto.setPhone(dealership.getPhone());
        return dto;
    }

    public DealershipWithCarsResponseDto toWithCarsResponseDto(Dealership dealership) {
        DealershipWithCarsResponseDto dto = new DealershipWithCarsResponseDto();
        dto.setId(dealership.getId());
        dto.setName(dealership.getName());
        dto.setAddress(dealership.getAddress());
        dto.setPhone(dealership.getPhone());
        dto.setCars(dealership.getCars().stream().map(carMapper::toResponseDto).toList());
        return dto;
    }
}
```

src/main/java/com/example/autosalon/mapper/FeatureMapper.java
```java
package com.example.autosalon.mapper;

import com.example.autosalon.dto.FeatureRequestDto;
import com.example.autosalon.dto.FeatureResponseDto;
import com.example.autosalon.entity.Feature;
import org.springframework.stereotype.Component;

@Component
public class FeatureMapper {

    public Feature toEntity(FeatureRequestDto dto) {
        Feature feature = new Feature();
        feature.setName(dto.getName());
        feature.setDescription(dto.getDescription());
        feature.setCategory(dto.getCategory());
        return feature;
    }

    public FeatureResponseDto toResponseDto(Feature feature) {
        FeatureResponseDto dto = new FeatureResponseDto();
        dto.setId(feature.getId());
        dto.setName(feature.getName());
        dto.setDescription(feature.getDescription());
        dto.setCategory(feature.getCategory());
        return dto;
    }
}
```

src/main/java/com/example/autosalon/mapper/SaleMapper.java
```java
package com.example.autosalon.mapper;

import com.example.autosalon.dto.SaleRequestDto;
import com.example.autosalon.dto.SaleResponseDto;
import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Customer;
import com.example.autosalon.entity.Sale;
import com.example.autosalon.service.CarService;
import com.example.autosalon.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SaleMapper {

    private final CarService carService;
    private final CustomerService customerService;

    public SaleResponseDto toResponseDto(Sale sale) {
        if (sale == null) {
            return null;
        }

        SaleResponseDto dto = new SaleResponseDto();
        dto.setId(sale.getId());
        dto.setSaleDate(sale.getSaleDate());
        dto.setSalePrice(sale.getSalePrice());

        if (sale.getCar() != null) {
            dto.setCarId(sale.getCar().getId());
            dto.setCarBrand(sale.getCar().getBrand());
            dto.setCarModel(sale.getCar().getModel());
        }

        if (sale.getCustomer() != null) {
            dto.setCustomerId(sale.getCustomer().getId());
            dto.setCustomerName(
                    sale.getCustomer().getFirstName() + " "
                            + sale.getCustomer().getLastName());
        }

        return dto;
    }

    public Sale toEntity(SaleRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Sale sale = new Sale();
        sale.setSaleDate(dto.getSaleDate());
        sale.setSalePrice(dto.getSalePrice());

        if (dto.getCarId() != null) {
            Car car = carService.getCarById(dto.getCarId());
            sale.setCar(car);
        }

        if (dto.getCustomerId() != null) {
            Customer customer = customerService.getCustomerById(dto.getCustomerId());
            sale.setCustomer(customer);
        }

        return sale;
    }
}
```

src/main/java/com/example/autosalon/repository/CarRepository.java
```java
package com.example.autosalon.repository;

import com.example.autosalon.entity.Car;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    List<Car> findByBrandIgnoreCase(String brand);

    @Override
    @EntityGraph(attributePaths = {"features", "sale"})
    List<Car> findAll();
}
```

src/main/java/com/example/autosalon/repository/CarRepositoryWithoutGraph.java
```java
package com.example.autosalon.repository;

import com.example.autosalon.entity.Car;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarRepositoryWithoutGraph extends JpaRepository<Car, Long> {
    List<Car> findByBrandIgnoreCase(String brand);
}
```

src/main/java/com/example/autosalon/repository/CustomerRepository.java
```java
package com.example.autosalon.repository;

import com.example.autosalon.entity.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с сущностью Customer (покупатели)
 * Предоставляет базовые CRUD операции:
 * - findAll() - получить всех покупателей
 * - findById(id) - найти по ID
 * - save(customer) - сохранить/обновить
 * - deleteById(id) - удалить по ID
 * - count() - количество записей
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);
}
```

src/main/java/com/example/autosalon/repository/DealershipRepository.java
```java
package com.example.autosalon.repository;

import com.example.autosalon.entity.Dealership;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с сущностью Dealership (автосалоны)
 */
@Repository
public interface DealershipRepository extends JpaRepository<Dealership, Long> {

    /**
     * Поиск автосалонов по названию (без учета регистра)
     */
    List<Dealership> findByNameContainingIgnoreCase(String name);

    /**
     * Поиск автосалонов с количеством машин больше указанного
     */
    @Query("SELECT d FROM Dealership d WHERE SIZE(d.cars) > :minCars")
    List<Dealership> findDealershipsWithMinCars(@Param("minCars") int minCars);

    /**
     * Получение автосалонов с их машинами (решение проблемы N+1)
     */
    @Query("SELECT DISTINCT d FROM Dealership d LEFT JOIN FETCH d.cars")
    List<Dealership> findAllWithCars();
}
```

src/main/java/com/example/autosalon/repository/FeatureRepository.java
```java
package com.example.autosalon.repository;

import com.example.autosalon.entity.Feature;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с сущностью Feature (особенности автомобилей)
 * Предоставляет базовые CRUD операции:
 * - findAll() - получить все особенности
 * - findById(id) - найти по ID
 * - save(feature) - сохранить/обновить
 * - deleteById(id) - удалить по ID
 * - count() - количество записей
 */
@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {
    List<Feature> findByCategory(String category);
}
```

src/main/java/com/example/autosalon/repository/SaleRepository.java
```java
package com.example.autosalon.repository;

import com.example.autosalon.entity.Sale;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    Optional<Sale> findByCarId(Long carId);

    List<Sale> findByCustomerId(Long customerId);

    @Query("SELECT s FROM Sale s WHERE s.saleDate BETWEEN :start AND :end")
    List<Sale> findBySaleDateBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
```

src/main/java/com/example/autosalon/service/CarService.java

```java
package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Sale;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.SaleRepository;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final CarRepositoryWithoutGraph carRepositoryWithout;
    private final SaleRepository saleRepository;
    private final ObjectProvider<CarService> self;

    @Transactional(readOnly = true)
    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Car getCarById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Машина с id " + id + " не найдена"));
    }

    @Transactional
    public Car createCar(Car car) {
        car.setId(null);
        return carRepository.save(car);
    }

    @Transactional
    public Car updateCar(Long id, Car carDetails) {
        Car existingCar = self.getObject().getCarById(id);

        existingCar.setBrand(carDetails.getBrand());
        existingCar.setModel(carDetails.getModel());
        existingCar.setYear(carDetails.getYear());
        existingCar.setColor(carDetails.getColor());
        existingCar.setPrice(carDetails.getPrice());

        return existingCar;
    }

    @Transactional
    public void deleteCar(Long id) {
        Car car = self.getObject().getCarById(id);

        if (car.getSale() != null) {
            Sale sale = car.getSale();
            String errorMessage = String.format(
                    "Невозможно удалить машину ID=%d %s %s %d - она уже продана! "
                            +
                            "(ID продажи: %d, дата продажи: %s, покупатель: %s %s)",
                    car.getId(),
                    car.getBrand(),
                    car.getModel(),
                    car.getYear(),
                    sale.getId(),
                    sale.getSaleDate().toLocalDate(),
                    sale.getCustomer() != null ? sale.getCustomer().getFirstName() : "неизвестно",
                    sale.getCustomer() != null ? sale.getCustomer().getLastName() : "неизвестно"
            );

            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        log.info("Удаление машины ID={} {} {} (не продана)",
                car.getId(), car.getBrand(), car.getModel());

        car.getFeatures().clear();
        carRepository.delete(car);
        log.info("Машина с id {} успешно удалена", id);
    }

    @Transactional(readOnly = true)
    public List<Car> getCarsByBrand(String brand) {
        return carRepository.findByBrandIgnoreCase(brand);
    }

    @Transactional(readOnly = true)
    public List<Car> getCarsWithNplusOneProblem() {
        log.info("=== ПРОБЛЕМА N+1: обычный findAll ===");
        return carRepositoryWithout.findAll();
    }

    @Transactional(readOnly = true)
    public List<Car> getCarsWithSolution() {
        log.info("=== РЕШЕНИЕ: findAll с @EntityGraph ===");
        return carRepository.findAll();
    }
}
```

src/main/java/com/example/autosalon/service/CustomerService.java
```java
package com.example.autosalon.service;

import com.example.autosalon.entity.Customer;
import com.example.autosalon.repository.CustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ObjectProvider<CustomerService> self;

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found with email: " + email));
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        customer.setId(null);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customerDetails) {
        Customer existingCustomer = self.getObject().getCustomerById(id);

        existingCustomer.setFirstName(customerDetails.getFirstName());
        existingCustomer.setLastName(customerDetails.getLastName());
        existingCustomer.setEmail(customerDetails.getEmail());
        existingCustomer.setPhone(customerDetails.getPhone());

        return existingCustomer;
    }

    @Transactional
    public Customer updatePhone(Long id, String phone) {
        Customer customer = self.getObject().getCustomerById(id);
        customer.setPhone(phone);
        return customer;
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = self.getObject().getCustomerById(id);

        if (!customer.getSales().isEmpty()) {
            throw new IllegalStateException(
                    String.format(
                            "Невозможно удалить покупателя ID=%d %s %s - "
                                    + "у него есть продажи (количество: %d)",
                            customer.getId(),
                            customer.getFirstName(),
                            customer.getLastName(),
                            customer.getSales().size()));
        }

        customerRepository.delete(customer);
    }
}
```

src/main/java/com/example/autosalon/service/DealershipService.java
```java
package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Dealership;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.DealershipRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DealershipService {

    private final DealershipRepository dealershipRepository;
    private final CarRepository carRepository;
    private final CarService carService;
    private final DealershipTransactionalService dealershipTransactionalService;


    @Transactional(readOnly = true)
    public List<Dealership> getAllDealerships() {
        List<Dealership> dealerships = dealershipRepository.findAllWithCars();

        carRepository.findAll();

        for (Dealership d : dealerships) {
            for (Car c : d.getCars()) {
                c.getFeatures().size();
            }
        }

        return dealerships;
    }

    public Dealership getDealershipById(Long id) {
        return dealershipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Автосалон с id " + id + " не найден"));
    }

    @Transactional(readOnly = true)
    public Dealership getDealershipWithCars(Long id) {
        Dealership dealership = getDealershipById(id);
        if (!dealership.getCars().isEmpty()) {
            log.debug(
                    "Автосалон {} содержит {} машин",
                    dealership.getName(),
                    dealership.getCars().size());
            for (Car car : dealership.getCars()) {
                car.getFeatures().size();
            }
        }
        return dealership;
    }

    @Transactional
    public Dealership createDealership(Dealership dealership) {
        dealership.setId(null);
        return dealershipRepository.save(dealership);
    }

    @Transactional
    public Dealership updateDealership(Long id, Dealership dealershipDetails) {
        Dealership existing = getDealershipById(id);
        existing.setName(dealershipDetails.getName());
        existing.setAddress(dealershipDetails.getAddress());
        existing.setPhone(dealershipDetails.getPhone());
        return existing;
    }

    @Transactional
    public void deleteDealership(Long id) {
        Dealership dealership = getDealershipById(id);


        List<Car> cars = dealership.getCars();
        for (Car car : cars) {
            carService.deleteCar(car.getId());
        }

        dealershipRepository.delete(dealership);

        log.info("Автосалон с id {} успешно удален", id);
    }


    @Transactional
    public Dealership addCarToDealership(Long dealershipId, Long carId) {
        Dealership dealership = getDealershipById(dealershipId);
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Машина с id " + carId + " не найдена"));

        dealership.addCar(car);
        dealership.getCars().size();
        return dealership;
    }

    @Transactional
    public Dealership removeCarFromDealership(Long dealershipId, Long carId) {
        Dealership dealership = getDealershipById(dealershipId);
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Машина с id " + carId + " не найдена"));

        dealership.removeCar(car);
        dealership.getCars().size();
        return dealership;
    }


    @Transactional(readOnly = true)
    public long countDealerships() {
        return dealershipRepository.count();
    }

    public Dealership createDealershipWithCarsWithoutTransaction(
            Dealership dealership, List<Car> cars) {
        Dealership savedDealership = dealershipRepository.save(dealership);
        saveCarsWithErrorOnSecond(cars, savedDealership);
        return savedDealership;
    }

    @Transactional
    public Dealership createDealershipWithCarsWithTransaction(
            Dealership dealership, List<Car> cars) {
        return dealershipTransactionalService
                .createDealershipWithCarsWithTransaction(dealership, cars);
    }

    private void saveCarsWithErrorOnSecond(List<Car> cars, Dealership dealership) {
        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            car.setDealership(dealership);
            car.setId(null);

            if (i == 1) {
                throw new IllegalArgumentException(
                        String.format("Ошибка сохранения машины: %s %s",
                                car.getBrand(), car.getModel())
                );
            }

            carRepository.save(car);
        }
    }
}
```

src/main/java/com/example/autosalon/service/DealershipTransactionalService.java
```java
package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Dealership;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.DealershipRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DealershipTransactionalService {

    private final DealershipRepository dealershipRepository;
    private final CarRepository carRepository;

    @Transactional
    public Dealership createDealershipWithCarsWithTransaction(
            Dealership dealership,
            List<Car> cars) {
        Dealership savedDealership = dealershipRepository.save(dealership);
        saveCarsWithErrorOnSecond(cars, savedDealership);
        return savedDealership;
    }

    private void saveCarsWithErrorOnSecond(List<Car> cars, Dealership dealership) {
        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            car.setDealership(dealership);
            car.setId(null);

            if (i == 1) {
                throw new IllegalArgumentException(
                        String.format("Ошибка сохранения машины: %s %s",
                                car.getBrand(), car.getModel())
                );
            }

            carRepository.save(car);
        }
    }
}
```

src/main/java/com/example/autosalon/service/FeatureService.java
```java
package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Feature;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.repository.FeatureRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
```

src/main/java/com/example/autosalon/service/SaleService.java
```java
package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.Customer;
import com.example.autosalon.entity.Sale;
import com.example.autosalon.repository.SaleRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {

    private static final String SALE_NOT_FOUND_MESSAGE = "Sale not found with id: ";

    private final SaleRepository saleRepository;
    private final CarService carService;
    private final CustomerService customerService;

    @Transactional(readOnly = true)
    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Sale getSaleById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(SALE_NOT_FOUND_MESSAGE + id));
    }

    @Transactional(readOnly = true)
    public Sale getSaleByCarId(Long carId) {
        return saleRepository.findByCarId(carId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sale not found for car id: " + carId));
    }

    @Transactional(readOnly = true)
    public List<Sale> getSalesByCustomerId(Long customerId) {
        return saleRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<Sale> getSalesByDateRange(LocalDateTime start, LocalDateTime end) {
        return saleRepository.findBySaleDateBetween(start, end);
    }

    @Transactional
    public Sale createSale(Sale sale) {
        sale.setId(null);

        if (sale.getCar() == null || sale.getCar().getId() == null) {
            throw new IllegalArgumentException("Car must be specified for sale");
        }
        if (sale.getCustomer() == null || sale.getCustomer().getId() == null) {
            throw new IllegalArgumentException("Customer must be specified for sale");
        }

        Car car = carService.getCarById(sale.getCar().getId());
        if (car.getSale() != null) {
            throw new IllegalStateException(
                    String.format("Машина ID=%d %s %s %d уже продана (ID продажи: %d)",
                            car.getId(),
                            car.getBrand(),
                            car.getModel(),
                            car.getYear(),
                            car.getSale().getId()));
        }

        Customer customer = customerService.getCustomerById(sale.getCustomer().getId());

        sale.setCar(car);
        car.setSale(sale);

        sale.setCustomer(customer);
        customer.getSales().add(sale);

        return saleRepository.save(sale);
    }

    @Transactional
    public Sale updateSale(Long id, Sale saleDetails) {
        Sale existingSale = saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(SALE_NOT_FOUND_MESSAGE + id));

        existingSale.setSaleDate(saleDetails.getSaleDate());
        existingSale.setSalePrice(saleDetails.getSalePrice());

        if (saleDetails.getCar() != null
                && saleDetails.getCar().getId() != null
                && !saleDetails.getCar().getId().equals(
                        existingSale.getCar() != null ? existingSale.getCar().getId() : null)) {
            throw new IllegalStateException("Нельзя изменить машину у существующей продажи");
        }

        if (saleDetails.getCustomer() != null && saleDetails.getCustomer().getId() != null) {
            Customer newCustomer = customerService.getCustomerById(
                    saleDetails.getCustomer().getId());

            Customer oldCustomer = existingSale.getCustomer();
            if (oldCustomer != null && !oldCustomer.equals(newCustomer)) {
                oldCustomer.getSales().remove(existingSale);
            }

            existingSale.setCustomer(newCustomer);
            if (!newCustomer.getSales().contains(existingSale)) {
                newCustomer.getSales().add(existingSale);
            }
        }

        return existingSale;
    }

    @Transactional
    public void deleteSale(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(SALE_NOT_FOUND_MESSAGE + id));

        if (sale.getCar() != null) {
            sale.getCar().setSale(null);
        }

        if (sale.getCustomer() != null) {
            sale.getCustomer().getSales().remove(sale);
        }

        saleRepository.delete(sale);
        log.info("Продажа с id {} успешно удалена", id);
    }
}
```

src/test/java/com/example/autosalon/AutosalonApplicationTests.java
```java
package com.example.autosalon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AutosalonApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

README.MD
```md
# Автосалон

## Название темы
Веб-приложение для управления списком автомобилей в автосалоне.

---

## Этап 1: Basic REST service

### Описание проекта
Данный проект представляет собой RESTful веб-приложение, разработанное с использованием Spring Boot. Оно позволяет выполнять базовые CRUD-операции (создание, чтение, обновление, удаление) для сущности "Автомобиль" (Car).

В качестве хранилища данных используется In-Memory репозиторий, что позволяет запускать приложение без настройки внешней базы данных. Приложение построено с соблюдением многослойной архитектуры (контроллер, сервис, репозиторий) и использует DTO для передачи данных между слоями.

### Выполняемые функции
Приложение предоставляет REST API для управления автомобилями:

1. **Получение списка всех автомобилей**
   - Метод: GET
   - Эндпоинт: /api/cars
   - Описание: Возвращает список всех автомобилей, хранящихся в "базе данных".

2. **Получение автомобиля по ID**
   - Метод: GET
   - Эндпоинт: /api/cars/{id}
   - Описание: Возвращает детальную информацию об автомобиле по его уникальному идентификатору.

3. **Добавление нового автомобиля**
   - Метод: POST
   - Эндпоинт: /api/cars
   - Описание: Принимает JSON с данными нового автомобиля, сохраняет его и возвращает созданную запись.

4. **Обновление данных автомобиля**
   - Метод: PUT
   - Эндпоинт: /api/cars/{id}
   - Описание: Обновляет информацию об existing автомобиле по его ID на основе переданных данных.

5. **Удаление автомобиля**
   - Метод: DELETE
   - Эндпоинт: /api/cars/{id}
   - Описание: Удаляет автомобиль из "базы данных" по его ID.

### Ссылка на SonarQube проверку
https://sonarcloud.io/summary/new_code?id=anotherholeee_CarDealershipJava&branch=main

![ER-диграмма](https://github.com/anotherholeee/CarDealershipJava/wiki/ER%E2%80%90diagram)
---

## Этап 2: JPA (Hibernate/Spring Data)

### Описание проекта
На втором этапе проект был расширен для работы с реляционной базой данных PostgreSQL. Вместо In-Memory хранилища теперь используется полноценная база данных с JPA (Hibernate). Модель данных расширена до 5 взаимосвязанных сущностей, реализованы все необходимые связи, решена проблема N+1 запросов и продемонстрирована работа транзакций.

### Модель данных (5 сущностей)

#### 1. Car (Машина)
- Центральная сущность проекта
- Содержит информацию об автомобиле: бренд, модель, год, цвет, цена
- Связана с `Dealership` (многие к одному)
- Связана с `Feature` (многие ко многим)
- Связана с `Sale` (один к одному)

#### 2. Dealership (Автосалон)
- Представляет автосалон, где продаются машины
- Содержит название, адрес, телефон
- Связан с `Car` (один ко многим)

#### 3. Feature (Особенность)
- Дополнительные характеристики автомобилей (полный привод, панорамная крыша, автопилот)
- Связана с `Car` (многие ко многим)

#### 4. Customer (Покупатель)
- Информация о покупателях: имя, фамилия, email, телефон
- Связан с `Sale` (один ко многим)

#### 5. Sale (Продажа)
- Фиксирует факт продажи автомобиля покупателю
- Содержит дату продажи и цену
- Связана с `Car` (один к одному) и `Customer` (многие к одному)

### Связи между сущностями

| Тип связи | От | К | Описание |
|-----------|----|----|----------|
| OneToMany | Dealership | Car | Один автосалон содержит много машин |
| ManyToOne | Car | Dealership | Много машин принадлежат одному салону |
| OneToMany | Customer | Sale | Один покупатель совершает много покупок |
| ManyToOne | Sale | Customer | Много продаж относятся к одному покупателю |
| ManyToMany | Car | Feature | Машины и особенности (через таблицу car_features) |
| OneToOne | Car | Sale | Одна машина может быть продана один раз |

### Технические детали реализации

#### 1. Подключение реляционной БД
- Использована PostgreSQL
- Настройки подключения вынесены в переменные окружения для безопасности
- Hibernate автоматически создает и обновляет схему базы данных

#### 2. CascadeType и FetchType
- **FetchType.LAZY** используется для всех связей, чтобы избежать загрузки лишних данных
- **CascadeType.ALL** настроен на стороне `@OneToMany` (Dealership → Car, Customer → Sale) для автоматического сохранения/удаления связанных сущностей
- **orphanRemoval = true** гарантирует удаление "осиротевших" записей

#### 3. Решение проблемы N+1
Проблема N+1 возникает при загрузке списка машин и последующем обращении к их особенностям. Реализовано два подхода:

**Проблема (эндпоинт `/api/cars/features/problem`):**
- Используется обычный `findAll()` без оптимизации
- Выполняется 1 запрос для получения машин и N запросов для получения особенностей

**Решение (эндпоинт `/api/cars/features/solution`):**
- Используется `@EntityGraph(attributePaths = {"features", "sale"})`
- Все связанные данные загружаются одним запросом с LEFT JOIN

#### 4. Демонстрация транзакций
Реализовано два метода для сохранения автосалона с несколькими машинами:

**Без `@Transactional` (эндпоинт `/api/dealerships/without-transaction`):**
- При ошибке на второй машине салон и первая машина сохраняются в БД
- Демонстрирует проблему частичного сохранения данных

**С `@Transactional` (эндпоинт `/api/dealerships/with-transaction`):**
- При ошибке все изменения откатываются
- Ни одна запись не сохраняется в БД
- Демонстрирует атомарность операций
```
