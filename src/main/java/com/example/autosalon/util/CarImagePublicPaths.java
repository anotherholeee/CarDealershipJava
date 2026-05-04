package com.example.autosalon.util;

public final class CarImagePublicPaths {

    private CarImagePublicPaths() {
    }

    public static String urlPath(Long carId, String fileName) {
        return "/api/cars/" + carId + "/photos/" + fileName;
    }
}
