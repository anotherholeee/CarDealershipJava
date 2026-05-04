package com.example.autosalon.catalog;

import java.util.List;

/**
 * Опции из формы подачи объявления (совпадают с чипами на фронте). При старте приложения
 * недостающие имена добавляются в таблицу {@code features}.
 */
public final class UiFeatureCatalog {

    public record NamedFeature(String category, String name) {}

    public static final List<NamedFeature> ENTRIES = List.of(
            new NamedFeature("Системы безопасности", "ABS"),
            new NamedFeature("Системы безопасности", "ESP"),
            new NamedFeature("Системы безопасности", "Антипробуксовочная"),
            new NamedFeature("Системы безопасности", "Иммобилайзер"),
            new NamedFeature("Системы безопасности", "Сигнализация"),
            new NamedFeature("Системы безопасности", "Подушки передние"),
            new NamedFeature("Системы безопасности", "Подушки боковые"),
            new NamedFeature("Системы безопасности", "Подушки задние"),
            new NamedFeature("Системы безопасности", "Датчики давления в шинах"),
            new NamedFeature("Системы безопасности", "Isofix"),
            new NamedFeature("Системы безопасности", "Обнаружение пешеходов"),
            new NamedFeature("Системы безопасности", "Подушки коленные"),
            new NamedFeature("Системы безопасности", "Система экстренного торможения"),
            new NamedFeature("Системы помощи", "Камера заднего вида"),
            new NamedFeature("Системы помощи", "Парктроники"),
            new NamedFeature("Системы помощи", "Контроль слепых зон"),
            new NamedFeature("Системы помощи", "Ассистент удержания полосы"),
            new NamedFeature("Системы помощи", "Помощь при старте в гору"),
            new NamedFeature("Экстерьер", "Легкосплавные диски"),
            new NamedFeature("Экстерьер", "Рейлинги"),
            new NamedFeature("Экстерьер", "Спойлер"),
            new NamedFeature("Экстерьер", "Заводская тонировка"),
            new NamedFeature("Экстерьер", "Панорамная крыша"),
            new NamedFeature("Интерьер", "Кожаный салон"),
            new NamedFeature("Интерьер", "Электропривод сидений"),
            new NamedFeature("Интерьер", "Память сидений"),
            new NamedFeature("Интерьер", "Люк"),
            new NamedFeature("Интерьер", "Подогрев руля"),
            new NamedFeature("Оптика и свет", "LED-фары"),
            new NamedFeature("Оптика и свет", "Ксенон"),
            new NamedFeature("Оптика и свет", "Противотуманные фары"),
            new NamedFeature("Оптика и свет", "Омыватель фар"),
            new NamedFeature("Оптика и свет", "ДХО"),
            new NamedFeature("Климат", "Климат-контроль"),
            new NamedFeature("Климат", "Кондиционер"),
            new NamedFeature("Климат", "Обогрев сидений"),
            new NamedFeature("Климат", "Вентиляция сидений"),
            new NamedFeature("Климат", "Обогрев лобового"),
            new NamedFeature("Мультимедиа", "Bluetooth"),
            new NamedFeature("Мультимедиа", "USB"),
            new NamedFeature("Мультимедиа", "AUX"),
            new NamedFeature("Мультимедиа", "Apple CarPlay"),
            new NamedFeature("Мультимедиа", "Android Auto"),
            new NamedFeature("Мультимедиа", "Навигация"),
            new NamedFeature("Комфорт", "Круиз-контроль"),
            new NamedFeature("Комфорт", "Бесключевой доступ"),
            new NamedFeature("Комфорт", "Электростеклоподъемники"),
            new NamedFeature("Комфорт", "Старт-стоп"),
            new NamedFeature("Комфорт", "Розетка 12V")
    );

    private UiFeatureCatalog() {}
}
