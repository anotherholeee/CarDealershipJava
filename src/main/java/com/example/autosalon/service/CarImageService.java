package com.example.autosalon.service;

import com.example.autosalon.entity.Car;
import com.example.autosalon.entity.CarImage;
import com.example.autosalon.entity.UserAccount;
import com.example.autosalon.dto.CarImageInfoDto;
import com.example.autosalon.repository.CarImageRepository;
import com.example.autosalon.repository.CarRepository;
import com.example.autosalon.util.CarImagePublicPaths;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarImageService {

    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]{1,180}$");
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    private final CarImageRepository carImageRepository;
    private final CarRepository carRepository;

    @Value("${app.upload-dir}")
    private Path uploadRoot;

    @Value("${app.max-images-per-car:10}")
    private int maxImagesPerCar;

    @Transactional(readOnly = true)
    public Resource loadImage(Long carId, String fileName) {
        if (!SAFE_FILENAME.matcher(fileName).matches()) {
            throw new IllegalArgumentException("Некорректное имя файла");
        }
        Path path = carDir(carId).resolve(fileName).normalize();
        if (!path.startsWith(carDir(carId).normalize())) {
            throw new IllegalArgumentException("Некорректный путь");
        }
        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("Файл не найден");
        }
        try {
            return new UrlResource(path.toUri());
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать файл", e);
        }
    }

    @Transactional
    public List<CarImageInfoDto> uploadImages(Long carId, UserAccount actor, MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Выберите хотя бы один файл");
        }
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException("Машина с id " + carId + " не найдена"));
        if (!canManageCarPhotos(car, actor)) {
            throw new IllegalStateException("Можно добавлять фото только к своим объявлениям");
        }
        int existing = carImageRepository.countByCarId(carId);
        if (existing + files.length > maxImagesPerCar) {
            throw new IllegalArgumentException(
                    "Не более " + maxImagesPerCar + " фотографий на объявление (сейчас " + existing + ")");
        }
        List<CarImageInfoDto> uploaded = new ArrayList<>();
        int sortNext = carImageRepository.findByCarIdOrderBySortOrderAsc(carId).stream()
                .mapToInt(CarImage::getSortOrder)
                .max()
                .orElse(0);
        try {
            Files.createDirectories(carDir(carId));
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать каталог для загрузок", e);
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String ext = extensionOf(file.getOriginalFilename(), file.getContentType());
            if (ext == null) {
                throw new IllegalArgumentException("Допустимы только изображения JPEG, PNG или WebP");
            }
            String storedName = UUID.randomUUID() + "." + ext;
            Path target = carDir(carId).resolve(storedName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IllegalStateException("Не удалось сохранить файл", e);
            }
            CarImage row = new CarImage();
            row.setCar(car);
            row.setFileName(storedName);
            row.setSortOrder(++sortNext);
            CarImage savedRow = carImageRepository.save(row);
            uploaded.add(new CarImageInfoDto(
                    savedRow.getId(),
                    CarImagePublicPaths.urlPath(carId, storedName)));
        }
        if (uploaded.isEmpty()) {
            throw new IllegalArgumentException("Нет файлов для загрузки");
        }
        return uploaded;
    }

    /**
     * Копирует изображения из classpath в каталог объявления и создаёт записи {@link CarImage},
     * только если у объявления ещё нет ни одного снимка (инициализация после сброса БД).
     *
     * @return число сохранённых снимков
     */
    @Transactional
    public int seedImagesIfAbsent(long carId, List<Resource> resources) throws IOException {
        if (resources == null || resources.isEmpty()) {
            return 0;
        }
        if (carImageRepository.countByCarId(carId) > 0) {
            return 0;
        }
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException("Машина с id " + carId + " не найдена"));
        Files.createDirectories(carDir(carId));
        int sortNext = 0;
        int saved = 0;
        for (Resource res : resources) {
            if (res == null || !res.exists() || !res.isReadable()) {
                continue;
            }
            String name = res.getFilename();
            String ext = extensionOf(name, null);
            if (ext == null) {
                log.warn("Пропуск seed-ресурса без известного расширения: {}", res);
                continue;
            }
            String storedName = UUID.randomUUID() + "." + ext;
            Path target = carDir(carId).resolve(storedName);
            try (InputStream in = res.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            CarImage row = new CarImage();
            row.setCar(car);
            row.setFileName(storedName);
            row.setSortOrder(++sortNext);
            carImageRepository.save(row);
            saved++;
        }
        if (saved > 0) {
            log.info("Seed фото: carId={} добавлено {} файлов", carId, saved);
        }
        return saved;
    }

    /**
     * Если нет ни одного снимка и не удалось подставить файлы из classpath — создаёт простые PNG-заглушки.
     */
    @Transactional
    public int seedPlaceholderImagesIfAbsent(long carId, int count) throws IOException {
        if (count <= 0 || carImageRepository.countByCarId(carId) > 0) {
            return 0;
        }
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException("Машина с id " + carId + " не найдена"));
        Files.createDirectories(carDir(carId));
        int sortNext = 0;
        int hueStep = Math.max(1, 360 / Math.max(count, 1));
        for (int i = 0; i < count; i++) {
            String storedName = UUID.randomUUID() + ".png";
            Path target = carDir(carId).resolve(storedName);
            BufferedImage img = new BufferedImage(1200, 760, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.getHSBColor((i * hueStep) / 360f, 0.22f, 0.45f));
            g.fillRect(0, 0, 1200, 760);
            g.setColor(Color.WHITE);
            g.drawString("Demo " + (i + 1) + "  #" + carId, 48, 72);
            g.dispose();
            try (OutputStream os = Files.newOutputStream(target)) {
                ImageIO.write(img, "png", os);
            }
            CarImage row = new CarImage();
            row.setCar(car);
            row.setFileName(storedName);
            row.setSortOrder(++sortNext);
            carImageRepository.save(row);
        }
        log.info("Seed placeholder фото: carId={} добавлено {} PNG", carId, count);
        return count;
    }

    @Transactional
    public void deleteImage(Long carId, Long imageId, UserAccount actor) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException("Машина с id " + carId + " не найдена"));
        if (!canManageCarPhotos(car, actor)) {
            throw new IllegalStateException("Можно удалять фото только у своих объявлений");
        }
        CarImage img = carImageRepository.findByIdAndCarId(imageId, carId)
                .orElseThrow(() -> new IllegalArgumentException("Фотография не найдена"));
        Path path = carDir(carId).resolve(img.getFileName());
        carImageRepository.delete(img);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Не удалось удалить файл с диска: {}", path, e);
        }
    }

    @Transactional
    public void deleteAllFilesForCar(Long carId) {
        List<CarImage> list = carImageRepository.findByCarIdOrderBySortOrderAsc(carId);
        Path dir = carDir(carId);
        for (CarImage img : list) {
            try {
                Files.deleteIfExists(dir.resolve(img.getFileName()));
            } catch (IOException e) {
                log.warn("Не удалось удалить файл {}", img.getFileName(), e);
            }
        }
        carImageRepository.deleteByCarId(carId);
        try {
            if (Files.isDirectory(dir)) {
                try (var stream = Files.list(dir)) {
                    stream.forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ex) {
                            log.warn("Не удалось удалить {}", p, ex);
                        }
                    });
                }
                Files.deleteIfExists(dir);
            }
        } catch (IOException e) {
            log.warn("Не удалось очистить каталог {}", dir, e);
        }
    }

    private Path carDir(Long carId) {
        return uploadRoot.resolve("cars").resolve(String.valueOf(carId));
    }

    private static boolean canManageCarPhotos(Car car, UserAccount actor) {
        if (AuthService.isAdmin(actor)) {
            return true;
        }
        return car.getOwner() != null && car.getOwner().getId().equals(actor.getId());
    }

    private static String extensionOf(String originalName, String contentType) {
        String ext = null;
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (ext == null || ext.isBlank() || !ALLOWED_EXT.contains(ext)) {
            if (contentType == null) {
                return null;
            }
            return switch (contentType.toLowerCase(Locale.ROOT)) {
                case "image/jpeg", "image/jpg" -> "jpg";
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                default -> null;
            };
        }
        return ext.equals("jpeg") ? "jpg" : ext;
    }
}
