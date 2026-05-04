package com.example.autosalon;

import com.example.autosalon.catalog.UiFeatureCatalog;
import com.example.autosalon.entity.*;
import com.example.autosalon.enums.AccountType;
import com.example.autosalon.repository.*;
import lombok.RequiredArgsConstructor;
import com.example.autosalon.service.CarImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final List<String> BY_CITIES = List.of(
            "Минск", "Гродно", "Брест", "Витебск", "Гомель", "Могилёв",
            "Барановичи", "Борисов", "Пинск", "Полоцк", "Орша", "Мозырь", "Солигорск", "Лида", "Новополоцк");

    private static String randomByCity(ThreadLocalRandom rnd) {
        return BY_CITIES.get(rnd.nextInt(BY_CITIES.size()));
    }

    private static Dealership pickDealership(List<Dealership> dealerships, ThreadLocalRandom rnd) {
        return dealerships.get(rnd.nextInt(dealerships.size()));
    }

    private final FeatureRepository featureRepository;
    private final CarRepository carRepository;
    private final CarImageRepository carImageRepository;
    private final CarImageService carImageService;
    private final ResourceLoader resourceLoader;
    private final DealershipRepository dealershipRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final UserAccountRepository userAccountRepository;

    @Value("${app.admin.username:}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(" ЗАПУСК ИНИЦИАЛИЗАЦИИ ТЕСТОВЫХ ДАННЫХ");
        System.out.println("=".repeat(60));

        // 1. СОЗДАНИЕ ОСОБЕННОСТЕЙ (FEATURES)
        createFeatures();
        ensureUiFeatureCatalogChips();

        // 2. СОЗДАНИЕ АВТОСАЛОНОВ (DEALERSHIPS)
        createDealerships();

        // 3. СОЗДАНИЕ МАШИН (CARS) - ВАЖНО: сначала фичи, потом машины
        createCars();

        // 3.0. Если машины уже были в БД со старыми английскими цветами — приводим к русским
        migrateLegacyCarColorsToRussian();

        // 3.0.1. Старые записи без города / расхода — заполняем разумными значениями по умолчанию
        backfillCarTechnicalSpecsIfMissing();

        // 3.0.2. Дата публикации для записей до появления поля published_at
        backfillCarPublishedAtIfMissing();

        // 3.1. ГАРАНТИЯ СВЯЗЕЙ CAR-FEATURE (если машины уже были в БД)
        ensureCarFeatures();

        // 3.2. Фото объявлений: из classpath seed/cars/... или PNG-заглушки, если в БД нет снимков
        ensureSeedCarPhotos();

        // 4. СОЗДАНИЕ ПОКУПАТЕЛЕЙ (CUSTOMERS)
        createCustomers();

        // 5. СОЗДАНИЕ ПРОДАЖ (SALES)
        createSales();

        // 6. ВЫВОД СТАТИСТИКИ
        printStatistics();

        // 7. ПРОВЕРКА СВЯЗЕЙ
        verifyFeatures();

        // 8. Учётная запись администратора (если заданы app.admin.* и логин свободен)
        ensureAdminUser();

        System.out.println("=".repeat(60));
        System.out.println(" ИНИЦИАЛИЗАЦИЯ ЗАВЕРШЕНА!");
        System.out.println("=".repeat(60) + "\n");
    }

    private void ensureAdminUser() {
        if (adminUsername == null || adminUsername.isBlank()) {
            return;
        }
        String login = adminUsername.trim();
        if (userAccountRepository.existsByUsernameIgnoreCase(login)) {
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            System.out.println(" Пропуск создания admin: не задан app.admin.password");
            return;
        }
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UserAccount admin = new UserAccount();
        admin.setUsername(login);
        admin.setPasswordHash(encoder.encode(adminPassword));
        admin.setAccountType(AccountType.ADMIN);
        admin.setPersonName("Администратор");
        admin.setPhone(login);
        userAccountRepository.save(admin);
        System.out.println(" Создана учётная запись администратора: " + login);
    }

    private void ensureSeedCarPhotos() {
        System.out.println("\n ФОТО ОБЪЯВЛЕНИЙ (classpath:seed/cars/<марка-модель>/1..5.jpg или seed/cars/default/):");
        int touched = 0;
        for (Car car : carRepository.findAll()) {
            if (carImageRepository.countByCarId(car.getId()) > 0) {
                continue;
            }
            String key = seedPhotoFolderKey(car.getBrand(), car.getModel());
            List<Resource> gallery = loadSeedGalleryForCar(key);
            try {
                int n = carImageService.seedImagesIfAbsent(car.getId(), gallery);
                if (n == 0) {
                    n = carImageService.seedPlaceholderImagesIfAbsent(car.getId(), 5);
                }
                if (n > 0) {
                    touched++;
                    System.out.println("    id=" + car.getId() + " " + car.getBrand() + " " + car.getModel() + ": "
                            + n + " снимков");
                }
            } catch (IOException e) {
                System.out.println("    id=" + car.getId() + " ошибка: " + e.getMessage());
            }
        }
        if (touched == 0) {
            System.out.println("    Пропуск: у всех объявлений уже есть фото, либо нет машин.");
        }
    }

    private static String seedPhotoFolderKey(String brand, String model) {
        return slugPart(brand) + "-" + slugPart(model);
    }

    private static String slugPart(String raw) {
        if (raw == null || raw.isBlank()) {
            return "x";
        }
        return raw.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
    }

    private List<Resource> loadSeedGalleryForCar(String key) {
        List<Resource> out = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Resource chosen = pickFirstExistingResource(
                    resourceLoader.getResource("classpath:seed/cars/" + key + "/" + i + ".jpg"),
                    resourceLoader.getResource("classpath:seed/cars/" + key + "/" + i + ".jpeg"),
                    resourceLoader.getResource("classpath:seed/cars/" + key + "/" + i + ".png"),
                    resourceLoader.getResource("classpath:seed/cars/" + key + "/" + i + ".webp"));
            if (chosen == null) {
                chosen = pickFirstExistingResource(
                        resourceLoader.getResource("classpath:seed/cars/default/" + i + ".jpg"),
                        resourceLoader.getResource("classpath:seed/cars/default/" + i + ".jpeg"),
                        resourceLoader.getResource("classpath:seed/cars/default/" + i + ".png"),
                        resourceLoader.getResource("classpath:seed/cars/default/" + i + ".webp"));
            }
            if (chosen != null) {
                out.add(chosen);
            }
        }
        return out;
    }

    private static Resource pickFirstExistingResource(Resource... candidates) {
        for (Resource r : candidates) {
            if (r != null && r.exists() && r.isReadable()) {
                return r;
            }
        }
        return null;
    }

    private void createFeatures() {
        if (featureRepository.count() > 0) {
            System.out.println(" Особенности уже существуют, пропускаем...");
            return;
        }

        System.out.println("\n СОЗДАНИЕ ОСОБЕННОСТЕЙ:");

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
        System.out.println("    Создано " + features.size() + " особенностей");
    }

    /** Дополняет справочник опциями из формы объявления (чипы на фронте), без дубликатов по имени. */
    private void ensureUiFeatureCatalogChips() {
        int added = 0;
        for (UiFeatureCatalog.NamedFeature nf : UiFeatureCatalog.ENTRIES) {
            if (!featureRepository.existsByName(nf.name())) {
                featureRepository.save(createFeature(nf.name(), "", nf.category()));
                added++;
            }
        }
        if (added > 0) {
            System.out.println("    Добавлено опций каталога UI: " + added);
        }
    }

    private void createDealerships() {
        if (dealershipRepository.count() > 0) {
            System.out.println(" Автосалоны уже существуют, пропускаем...");
            return;
        }

        System.out.println("\n СОЗДАНИЕ АВТОСАЛОНОВ:");

        List<Dealership> dealerships = Arrays.asList(
                createDealership("Автосалон Премиум", "г. Минск, пр-т Независимости, 95", "+375 (17) 311-11-11"),
                createDealership("Автомир Юг", "г. Гомель, пр-т Ленина, 32", "+375 (232) 22-33-44"),
                createDealership("СпортКар", "г. Брест, ул. Машерова, 15", "+375 (162) 45-67-89"),
                createDealership("ЭкономАвто", "г. Витебск, ул. Ленина, 26", "+375 (212) 36-47-58"),
                createDealership("Дилерский Центр", "г. Гродно, ул. Советская, 5А", "+375 (152) 44-55-66")
        );

        dealershipRepository.saveAll(dealerships);
        System.out.println("    Создано " + dealerships.size() + " автосалонов");
    }

    private void createCars() {
        if (carRepository.count() > 0) {
            System.out.println(" Машины уже существуют, пропускаем...");
            return;
        }

        System.out.println("\n СОЗДАНИЕ МАШИН:");

        List<Dealership> dealerships = dealershipRepository.findAll();
        List<Feature> features = featureRepository.findAll();

        if (dealerships.isEmpty() || features.isEmpty()) {
            System.out.println("    Нет автосалонов или особенностей для создания машин!");
            return;
        }

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long[] seedPublishDaysAgo = {0, 1, 2, 4, 7, 10, 14, 21, 30, 45, 60, 90, 5};
        final int[] publishIdx = {0};
        java.util.function.Supplier<Instant> nextPublishedAt =
                () -> Instant.now().minus(seedPublishDaysAgo[publishIdx[0]++], ChronoUnit.DAYS);

        // ВАЖНО: Создаем машины и сохраняем их по одной с синхронизацией
        System.out.println("    Доступные фичи:");
        for (int i = 0; i < features.size(); i++) {
            System.out.println("      [" + i + "] " + features.get(i).getId() + ": " +
                    features.get(i).getName() + " (" + features.get(i).getCategory() + ")");
        }

        // Mercedes S-Class (id 1)
        Car car1 = new Car();
        car1.setBrand("Mercedes");
        car1.setModel("S-Class");
        car1.setYear(2024);
        car1.setColor("Чёрный металлик");
        car1.setPrice(150000);
        car1.setDealership(pickDealership(dealerships, rnd));
        car1.setInteriorColor("Чёрный");
        car1.setInteriorMaterial("Кожа Nappa премиум");
        enrichSpecs(car1, 3.0, 12000, 449, 12.5, 7.5, 9.5, 5, randomByCity(rnd), "auto", "sedan", "petrol", "awd");
        car1.setPublishedAt(nextPublishedAt.get());
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
        System.out.println("    Mercedes S-Class: добавлено фич: " + car1.getFeatures().size());

        // BMW 7 Series (id 2)
        Car car2 = new Car();
        car2.setBrand("BMW");
        car2.setModel("7 Series");
        car2.setYear(2024);
        car2.setColor("Тёмно-синий металлик");
        car2.setPrice(140000);
        car2.setDealership(pickDealership(dealerships, rnd));
        car2.setInteriorColor("Коньяк");
        car2.setInteriorMaterial("Кожа Merino");
        enrichSpecs(car2, 3.0, 8000, 381, 11.5, 6.8, 8.5, 5, randomByCity(rnd), "auto", "sedan", "petrol", "awd");
        car2.setPublishedAt(nextPublishedAt.get());
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
        System.out.println("    BMW 7 Series: добавлено фич: " + car2.getFeatures().size());

        // Audi A8 (id 3)
        Car car3 = new Car();
        car3.setBrand("Audi");
        car3.setModel("A8");
        car3.setYear(2024);
        car3.setColor("Серебристый");
        car3.setPrice(135000);
        car3.setDealership(pickDealership(dealerships, rnd));
        car3.setInteriorColor("Слоновая кость");
        car3.setInteriorMaterial("Перфорированная кожа");
        enrichSpecs(car3, 3.0, 15000, 340, 11.0, 6.5, 8.2, 5, randomByCity(rnd), "auto", "sedan", "petrol", "awd");
        car3.setPublishedAt(nextPublishedAt.get());
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
        System.out.println("    Audi A8: добавлено фич: " + car3.getFeatures().size());

        // Porsche 911 Turbo S (id 4)
        Car car4 = new Car();
        car4.setBrand("Porsche");
        car4.setModel("911 Turbo S");
        car4.setYear(2024);
        car4.setColor("Красный");
        car4.setPrice(250000);
        car4.setDealership(pickDealership(dealerships, rnd));
        car4.setInteriorColor("Чёрный с красной строчкой");
        car4.setInteriorMaterial("Алькантара и кожа");
        enrichSpecs(car4, 3.7, 5000, 650, 14.0, 8.5, 10.5, 4, randomByCity(rnd), "auto", "coupe", "petrol", "awd");
        car4.setPublishedAt(nextPublishedAt.get());
        car4 = carRepository.save(car4);

        car4.addFeature(features.get(0));  // Полный привод
        car4.addFeature(features.get(15)); // Спортивные сиденья
        car4.addFeature(features.get(16)); // Спортивный режим
        car4.addFeature(features.get(17)); // Лаунч-контроль
        car4.addFeature(features.get(7));  // Адаптивный круиз-контроль
        car4.addFeature(features.get(9));  // 360° камеры
        car4.addFeature(features.get(14)); // Аудиосистема Bose
        carRepository.save(car4);
        System.out.println("    Porsche 911: добавлено фич: " + car4.getFeatures().size());

        // BMW M5 Competition (id 5)
        Car car5 = new Car();
        car5.setBrand("BMW");
        car5.setModel("M5 Competition");
        car5.setYear(2024);
        car5.setColor("Синий металлик");
        car5.setPrice(120000);
        car5.setDealership(pickDealership(dealerships, rnd));
        car5.setInteriorColor("Чёрный");
        car5.setInteriorMaterial("Кожа и алькантара");
        enrichSpecs(car5, 4.4, 22000, 625, 13.5, 8.0, 10.0, 5, randomByCity(rnd), "auto", "sedan", "petrol", "awd");
        car5.setPublishedAt(nextPublishedAt.get());
        car5 = carRepository.save(car5);

        car5.addFeature(features.get(0));  // Полный привод
        car5.addFeature(features.get(15)); // Спортивные сиденья
        car5.addFeature(features.get(16)); // Спортивный режим
        car5.addFeature(features.get(17)); // Лаунч-контроль
        car5.addFeature(features.get(3));  // Кожаный салон
        car5.addFeature(features.get(4));  // Подогрев сидений
        car5.addFeature(features.get(14)); // Аудиосистема Bose
        carRepository.save(car5);
        System.out.println("    BMW M5: добавлено фич: " + car5.getFeatures().size());

        // Audi RS7 (id 6)
        Car car6 = new Car();
        car6.setBrand("Audi");
        car6.setModel("RS7");
        car6.setYear(2024);
        car6.setColor("Серый нардо");
        car6.setPrice(115000);
        car6.setDealership(pickDealership(dealerships, rnd));
        car6.setInteriorColor("Тёмно-серый");
        car6.setInteriorMaterial("Кожа с велюровыми вставками");
        enrichSpecs(car6, 4.0, 18000, 600, 12.8, 7.8, 9.8, 5, randomByCity(rnd), "auto", "hatchback", "petrol", "awd");
        car6.setPublishedAt(nextPublishedAt.get());
        car6 = carRepository.save(car6);

        car6.addFeature(features.get(0));  // Полный привод
        car6.addFeature(features.get(15)); // Спортивные сиденья
        car6.addFeature(features.get(16)); // Спортивный режим
        car6.addFeature(features.get(17)); // Лаунч-контроль
        car6.addFeature(features.get(3));  // Кожаный салон
        car6.addFeature(features.get(9));  // 360° камеры
        car6.addFeature(features.get(13)); // Android Auto
        carRepository.save(car6);
        System.out.println("    Audi RS7: добавлено фич: " + car6.getFeatures().size());

        // Toyota Camry (id 7)
        Car car7 = new Car();
        car7.setBrand("Toyota");
        car7.setModel("Camry");
        car7.setYear(2024);
        car7.setColor("Белый перламутр");
        car7.setPrice(35000);
        car7.setDealership(pickDealership(dealerships, rnd));
        car7.setInteriorColor("Светло-серый");
        car7.setInteriorMaterial("Ткань премиум");
        enrichSpecs(car7, 2.5, 37000, 200, 8.8, 5.5, 6.9, 5, randomByCity(rnd), "auto", "sedan", "petrol", "fwd");
        car7.setPublishedAt(nextPublishedAt.get());
        car7 = carRepository.save(car7);

        car7.addFeature(features.get(4));  // Подогрев сидений
        car7.addFeature(features.get(5));  // Вентиляция сидений
        car7.addFeature(features.get(10)); // Парктроник
        car7.addFeature(features.get(11)); // Беспроводная зарядка
        carRepository.save(car7);
        System.out.println("    Toyota Camry: добавлено фич: " + car7.getFeatures().size());

        // Kia K5 (id 8)
        Car car8 = new Car();
        car8.setBrand("Kia");
        car8.setModel("K5");
        car8.setYear(2024);
        car8.setColor("Графитовый металлик");
        car8.setPrice(32000);
        car8.setDealership(pickDealership(dealerships, rnd));
        car8.setInteriorColor("Чёрный");
        car8.setInteriorMaterial("Ткань и экокожа");
        enrichSpecs(car8, 2.5, 28000, 190, 9.0, 5.8, 7.2, 5, randomByCity(rnd), "auto", "sedan", "petrol", "fwd");
        car8.setPublishedAt(nextPublishedAt.get());
        car8 = carRepository.save(car8);

        car8.addFeature(features.get(4));  // Подогрев сидений
        car8.addFeature(features.get(10)); // Парктроник
        car8.addFeature(features.get(11)); // Беспроводная зарядка
        car8.addFeature(features.get(12)); // Apple CarPlay
        carRepository.save(car8);
        System.out.println("    Kia K5: добавлено фич: " + car8.getFeatures().size());

        // Hyundai Sonata (id 9)
        Car car9 = new Car();
        car9.setBrand("Hyundai");
        car9.setModel("Sonata");
        car9.setYear(2024);
        car9.setColor("Голубой металлик");
        car9.setPrice(30000);
        car9.setDealership(pickDealership(dealerships, rnd));
        car9.setInteriorColor("Кремовый");
        car9.setInteriorMaterial("Экокожа Softex");
        enrichSpecs(car9, 2.5, 41000, 180, 9.2, 6.0, 7.4, 5, randomByCity(rnd), "auto", "sedan", "petrol", "fwd");
        car9.setPublishedAt(nextPublishedAt.get());
        car9 = carRepository.save(car9);

        car9.addFeature(features.get(4));  // Подогрев сидений
        car9.addFeature(features.get(10)); // Парктроник
        car9.addFeature(features.get(11)); // Беспроводная зарядка
        car9.addFeature(features.get(13)); // Android Auto
        carRepository.save(car9);
        System.out.println("    Hyundai Sonata: добавлено фич: " + car9.getFeatures().size());

        // Toyota Land Cruiser 300 (id 10)
        Car car10 = new Car();
        car10.setBrand("Toyota");
        car10.setModel("Land Cruiser 300");
        car10.setYear(2024);
        car10.setColor("Белый жемчужный");
        car10.setPrice(100000);
        car10.setDealership(pickDealership(dealerships, rnd));
        car10.setInteriorColor("Бежевый орех");
        car10.setInteriorMaterial("Натуральная кожа");
        enrichSpecs(car10, 3.5, 15000, 415, 14.5, 9.0, 11.2, 7, randomByCity(rnd), "auto", "suv", "petrol", "awd");
        car10.setPublishedAt(nextPublishedAt.get());
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
        System.out.println("    Toyota LC300: добавлено фич: " + car10.getFeatures().size());

        // Lexus LX600 (id 11)
        Car car11 = new Car();
        car11.setBrand("Lexus");
        car11.setModel("LX600");
        car11.setYear(2024);
        car11.setColor("Платиновый металлик");
        car11.setPrice(110000);
        car11.setDealership(pickDealership(dealerships, rnd));
        car11.setInteriorColor("Бордовый");
        car11.setInteriorMaterial("Полукожа и микрофибра");
        enrichSpecs(car11, 3.5, 9000, 415, 14.2, 9.2, 11.0, 7, randomByCity(rnd), "auto", "suv", "petrol", "awd");
        car11.setPublishedAt(nextPublishedAt.get());
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
        System.out.println("    Lexus LX600: добавлено фич: " + car11.getFeatures().size());

        // Volvo XC90 (id 12)
        Car car12 = new Car();
        car12.setBrand("Volvo");
        car12.setModel("XC90");
        car12.setYear(2024);
        car12.setColor("Тёмно-серый металлик");
        car12.setPrice(85000);
        car12.setDealership(pickDealership(dealerships, rnd));
        car12.setInteriorColor("Светло-бежевый");
        car12.setInteriorMaterial("Микрофибра и кожа");
        enrichSpecs(car12, 2.0, 45000, 250, 10.5, 6.8, 8.2, 7, randomByCity(rnd), "auto", "suv", "petrol", "awd");
        car12.setPublishedAt(nextPublishedAt.get());
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
        System.out.println("    Volvo XC90: добавлено фич: " + car12.getFeatures().size());

        // Range Rover Sport (id 13)
        Car car13 = new Car();
        car13.setBrand("Range Rover");
        car13.setModel("Sport");
        car13.setYear(2024);
        car13.setColor("Чёрный сапфир");
        car13.setPrice(120000);
        car13.setDealership(pickDealership(dealerships, rnd));
        car13.setInteriorColor("Терракотовый");
        car13.setInteriorMaterial("Кожа Windsor");
        enrichSpecs(car13, 3.0, 30000, 400, 12.0, 7.5, 9.5, 5, randomByCity(rnd), "auto", "suv", "petrol", "awd");
        car13.setPublishedAt(nextPublishedAt.get());
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
        System.out.println("    Range Rover Sport: добавлено фич: " + car13.getFeatures().size());

        System.out.println("\n    Все машины созданы с фичами!");
    }

    private void verifyFeatures() {
        System.out.println("\n ПРОВЕРКА СВЯЗЕЙ CAR-FEATURE:");
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
        System.out.println("\n ПРОВЕРКА/ВОССТАНОВЛЕНИЕ СВЯЗЕЙ CAR-FEATURE:");

        List<Feature> allFeatures = featureRepository.findAll();
        if (allFeatures.isEmpty()) {
            System.out.println("    Особенности отсутствуют, нечего привязывать.");
            return;
        }

        Map<String, Feature> featuresByName = allFeatures.stream()
                .collect(Collectors.toMap(Feature::getName, Function.identity(), (a, b) -> a));

        List<Car> cars = carRepository.findAll(); // @EntityGraph загрузит features, если они есть
        if (cars.isEmpty()) {
            System.out.println("    Машины отсутствуют, нечего привязывать.");
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
            System.out.println("    Восстановлены связи для машин: " + updated);
        } else {
            System.out.println("    Все машины уже имеют фичи (или нет правил привязки).");
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
                createCustomer("Иван", "Иванов", "ivan.ivanov@email.com", "+375 (29) 111-11-11"),
                createCustomer("Петр", "Петров", "petr.petrov@email.com", "+375 (29) 222-22-22"),
                createCustomer("Сергей", "Сергеев", "sergey@email.com", "+375 (29) 333-33-33"),
                createCustomer("Анна", "Смирнова", "anna.smirnova@email.com", "+375 (29) 444-44-44"),
                createCustomer("Елена", "Козлова", "elena.kozlova@email.com", "+375 (29) 555-55-55"),
                createCustomer("Дмитрий", "Морозов", "dmitry.morozov@email.com", "+375 (29) 666-66-66"),
                createCustomer("Ольга", "Волкова", "olga.volkova@email.com", "+375 (29) 777-77-77"),
                createCustomer("Алексей", "Соколов", "alexey.sokolov@email.com", "+375 (29) 888-88-88"),
                createCustomer("Татьяна", "Михайлова", "tatiana@email.com", "+375 (29) 999-99-99"),
                createCustomer("Николай", "Николаев", "nikolay@email.com", "+375 (29) 000-00-00")
        );

        customerRepository.saveAll(customers);
        System.out.println("    Создано " + customers.size() + " покупателей");
    }

    private void createSales() {
        if (saleRepository.count() > 0) {
            System.out.println(" Продажи уже существуют, пропускаем...");
            return;
        }

        System.out.println("\n СОЗДАНИЕ ПРОДАЖ:");

        List<Car> cars = carRepository.findAll();
        List<Customer> customers = customerRepository.findAll();

        if (cars.isEmpty() || customers.isEmpty()) {
            System.out.println("    Нет машин или покупателей для создания продаж!");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Integer> prices = Arrays.asList(
                148000, 138000, 133000, 245000, 118000, 113000,
                34000, 31000, 29500, 98000, 108000, 83000
        );
        List<Integer> daysAgo = Arrays.asList(
                14, 13, 12, 11, 10, 6, 5, 4, 3, 2, 1, 0
        );

        int salesCount = Math.min(Math.min(cars.size(), prices.size()), daysAgo.size());
        List<Sale> sales = new ArrayList<>();

        for (int i = 0; i < salesCount; i++) {
            Customer customer = customers.get(i % customers.size());
            sales.add(createSale(
                    cars.get(i),
                    customer,
                    now.minusDays(daysAgo.get(i)),
                    prices.get(i)
            ));
        }

        if (sales.isEmpty()) {
            System.out.println("    Не удалось создать продажи: недостаточно данных");
            return;
        }

        saleRepository.saveAll(sales);
        System.out.println("    Создано " + sales.size() + " продаж");
    }

    /**
     * Сидер не перезаписывает уже существующие машины — в БД остаются старые значения {@code color}.
     * Фронт строит список «Цвет кузова» из API, поэтому без этого шага подтягиваются английские строки.
     */
    private void migrateLegacyCarColorsToRussian() {
        record Fix(String brand, String model, String wasColor, String ruColor) {
        }

        List<Fix> byModel = Arrays.asList(
                new Fix("Mercedes", "S-Class", "Black", "Чёрный металлик"),
                new Fix("BMW", "7 Series", "Dark Blue", "Тёмно-синий металлик"),
                new Fix("Audi", "A8", "Silver", "Серебристый"),
                new Fix("Porsche", "911 Turbo S", "Red", "Красный"),
                new Fix("BMW", "M5 Competition", "Blue", "Синий металлик"),
                new Fix("Audi", "RS7", "Gray", "Серый нардо"),
                new Fix("Toyota", "Camry", "White", "Белый перламутр"),
                new Fix("Kia", "K5", "Gray", "Графитовый металлик"),
                new Fix("Hyundai", "Sonata", "Blue", "Голубой металлик"),
                new Fix("Toyota", "Land Cruiser 300", "White", "Белый жемчужный"),
                new Fix("Lexus", "LX600", "Silver", "Платиновый металлик"),
                new Fix("Volvo", "XC90", "Dark Gray", "Тёмно-серый металлик"),
                new Fix("Range Rover", "Sport", "Black", "Чёрный сапфир")
        );

        Map<String, String> anyCar = new LinkedHashMap<>();
        anyCar.put("Black", "Чёрный металлик");
        anyCar.put("White", "Белый перламутр");
        anyCar.put("Silver", "Серебристый");
        anyCar.put("Gray", "Серый нардо");
        anyCar.put("Dark Gray", "Тёмно-серый металлик");
        anyCar.put("Dark Blue", "Тёмно-синий металлик");
        anyCar.put("Blue", "Синий металлик");
        anyCar.put("Red", "Красный");

        List<Car> cars = carRepository.findAll();
        int updated = 0;
        for (Car car : cars) {
            String color = car.getColor();
            if (color == null || color.isBlank()) {
                continue;
            }
            boolean matched = false;
            for (Fix f : byModel) {
                if (f.brand.equalsIgnoreCase(car.getBrand())
                        && f.model.equalsIgnoreCase(car.getModel())
                        && f.wasColor.equals(color)) {
                    car.setColor(f.ruColor);
                    matched = true;
                    updated++;
                    break;
                }
            }
            if (matched) {
                continue;
            }
            String ru = anyCar.get(color);
            if (ru != null) {
                car.setColor(ru);
                updated++;
            }
        }
        if (updated > 0) {
            carRepository.saveAll(cars);
            System.out.println("\n   Миграция цветов кузова EN→RU: обновлено записей: " + updated);
        }
    }

    private void printStatistics() {
        System.out.println("\n СТАТИСТИКА БАЗЫ ДАННЫХ:");
        System.out.println("   Особенности: " + featureRepository.count());
        System.out.println("   Автосалоны: " + dealershipRepository.count());
        System.out.println("   Машины: " + carRepository.count());
        System.out.println("   Покупатели: " + customerRepository.count());
        System.out.println("   Продажи: " + saleRepository.count());
    }

    private void backfillCarTechnicalSpecsIfMissing() {
        List<Car> cars = carRepository.findAll();
        boolean changed = false;
        for (Car car : cars) {
            if (car.getEngineVolume() == null) {
                car.setEngineVolume(2.0);
                changed = true;
            }
            if (car.getMileage() == null) {
                car.setMileage(30000);
                changed = true;
            }
            if (car.getPowerHp() == null) {
                car.setPowerHp(180);
                changed = true;
            }
            if (car.getFuelConsumptionCity() == null) {
                car.setFuelConsumptionCity(9.0);
                changed = true;
            }
            if (car.getFuelConsumptionHighway() == null) {
                car.setFuelConsumptionHighway(6.0);
                changed = true;
            }
            if (car.getFuelConsumptionMixed() == null) {
                car.setFuelConsumptionMixed(7.5);
                changed = true;
            }
            if (car.getSeatCount() == null) {
                car.setSeatCount(5);
                changed = true;
            }
            if (car.getCity() == null || car.getCity().isBlank()) {
                car.setCity("Минск");
                changed = true;
            }
            if (car.getTransmission() == null || car.getTransmission().isBlank()) {
                car.setTransmission("auto");
                changed = true;
            }
            if (car.getBodyType() == null || car.getBodyType().isBlank()) {
                car.setBodyType("sedan");
                changed = true;
            }
            if (car.getEngineType() == null || car.getEngineType().isBlank()) {
                car.setEngineType("petrol");
                changed = true;
            }
            if (car.getDriveType() == null || car.getDriveType().isBlank()) {
                car.setDriveType("fwd");
                changed = true;
            }
        }
        if (changed) {
            carRepository.saveAll(cars);
            System.out.println("   Дозаполнены тех. поля и город для существующих машин (если были пустыми)");
        }
    }

    private void backfillCarPublishedAtIfMissing() {
        List<Car> cars = carRepository.findAll();
        boolean changed = false;
        Instant base = Instant.now();
        for (Car car : cars) {
            if (car.getPublishedAt() != null) {
                continue;
            }
            long daysAgo = car.getId() != null ? Math.min(365L, 5L + (car.getId() % 120)) : 30L;
            car.setPublishedAt(base.minus(daysAgo, ChronoUnit.DAYS));
            changed = true;
        }
        if (changed) {
            carRepository.saveAll(cars);
            System.out.println("   Заполнена дата публикации (published_at) для существующих объявлений");
        }
    }

    private void enrichSpecs(
            Car car,
            double engineVolumeL,
            int mileageKm,
            int powerHp,
            double fuelCity,
            double fuelHighway,
            double fuelMixed,
            int seatCount,
            String city,
            String transmission,
            String bodyType,
            String engineType,
            String driveType) {
        car.setEngineVolume(engineVolumeL);
        car.setMileage(mileageKm);
        car.setPowerHp(powerHp);
        car.setFuelConsumptionCity(fuelCity);
        car.setFuelConsumptionHighway(fuelHighway);
        car.setFuelConsumptionMixed(fuelMixed);
        car.setSeatCount(seatCount);
        car.setCity(city);
        car.setTransmission(transmission);
        car.setBodyType(bodyType);
        car.setEngineType(engineType);
        car.setDriveType(driveType);
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

