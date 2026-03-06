package com.example.autosalon;

import com.example.autosalon.entity.*;
import com.example.autosalon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

/*@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final FeatureRepository featureRepository;
    private final CarRepository carRepository;

    @Override
    public void run(String... args) throws Exception {
        // Создаем особенности, если их нет
        if (featureRepository.count() == 0) {
            Feature feature1 = new Feature();
            feature1.setName("Полный привод");
            feature1.setCategory("Технологии");

            Feature feature2 = new Feature();
            feature2.setName("Панорамная крыша");
            feature2.setCategory("Комфорт");

            Feature feature3 = new Feature();
            feature3.setName("Автопилот");
            feature3.setCategory("Технологии");

            featureRepository.saveAll(List.of(feature1, feature2, feature3));
            System.out.println("✅ Созданы тестовые особенности");
        }

        // Создаем машины с особенностями, если их нет
        if (carRepository.count() == 0) {
            List<Feature> features = featureRepository.findAll();

            if (features.size() >= 3) {
                // Машина 1: Toyota
                Car car1 = new Car();
                car1.setBrand("Toyota");
                car1.setModel("Camry");
                car1.setYear(2020);
                car1.setColor("Silver");
                car1.setPrice(25000);
                car1.setFeatures(List.of(features.get(0), features.get(1)));

                // Машина 2: BMW
                Car car2 = new Car();
                car2.setBrand("BMW");
                car2.setModel("X5");
                car2.setYear(2021);
                car2.setColor("Black");
                car2.setPrice(45000);
                car2.setFeatures(List.of(features.get(0), features.get(2)));

                // Машина 3: Audi
                Car car3 = new Car();
                car3.setBrand("Audi");
                car3.setModel("A6");
                car3.setYear(2022);
                car3.setColor("White");
                car3.setPrice(35000);
                car3.setFeatures(List.of(features.get(1), features.get(2)));

                carRepository.saveAll(List.of(car1, car2, car3));
                System.out.println("✅ Созданы тестовые машины с особенностями");
            }
        }
    }
}*/