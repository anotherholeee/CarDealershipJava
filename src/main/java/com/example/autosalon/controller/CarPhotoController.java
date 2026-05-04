package com.example.autosalon.controller;

import com.example.autosalon.dto.CarImageInfoDto;
import com.example.autosalon.entity.UserAccount;
import com.example.autosalon.service.AuthService;
import com.example.autosalon.service.CarImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.util.CollectionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cars/{carId}/photos")
@RequiredArgsConstructor
@Tag(name = "Car photos", description = "Фотографии объявлений")
public class CarPhotoController {

    private final CarImageService carImageService;
    private final AuthService authService;

    @GetMapping("/{fileName:.+}")
    @Operation(summary = "Скачать фото объявления")
    public ResponseEntity<Resource> getPhoto(
            @PathVariable Long carId,
            @PathVariable String fileName) {
        Resource resource = carImageService.loadImage(carId, fileName);
        MediaType mediaType = mediaTypeForFileName(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(mediaType)
                .body(resource);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить фото (один или несколько файлов)")
    public ResponseEntity<List<CarImageInfoDto>> uploadPhotos(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long carId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        UserAccount user = authService.requireUserByToken(authorization);
        MultipartFile[] arr = !CollectionUtils.isEmpty(files)
                ? files.toArray(MultipartFile[]::new)
                : new MultipartFile[0];
        List<CarImageInfoDto> created = carImageService.uploadImages(carId, user, arr);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Удалить фото по id записи")
    public ResponseEntity<Void> deletePhoto(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long carId,
            @PathVariable Long imageId) {
        UserAccount user = authService.requireUserByToken(authorization);
        carImageService.deleteImage(carId, imageId, user);
        return ResponseEntity.noContent().build();
    }

    private static MediaType mediaTypeForFileName(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
