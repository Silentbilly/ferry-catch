package com.ferrycatch.api.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ProdSnapshotDownloader {

    private static final String PROD_BASE_URL = "https://ferrynow.app";

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        RestTemplate restTemplate = new RestTemplate();

        downloadRoutesSnapshot(mapper, restTemplate);
        // downloadTripsSnapshots(mapper, restTemplate);
    }

    private static void downloadRoutesSnapshot(ObjectMapper mapper, RestTemplate restTemplate) throws Exception {
        ResponseEntity<String> response =
                restTemplate.getForEntity(PROD_BASE_URL + "/api/v1/routes", String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Failed to fetch routes from prod: " + response.getStatusCode());
        }

        JsonNode json = mapper.readTree(response.getBody());

        Path outPath = Path.of("src/test/resources/snapshots/routes-prod.json");
        Files.createDirectories(outPath.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(outPath.toFile(), json);
        System.out.println("Saved routes snapshot to " + outPath.toAbsolutePath());
    }
/*
    private static void downloadTripsSnapshots(ObjectMapper mapper, RestTemplate restTemplate) throws Exception {
        // Пример: список ключевых маршрутов/дат конфигурируем руками
        List<String> routes = List.of("ISTANBUL-BURSA", "ISTANBUL-YALOVA");
        List<String> dates = List.of("2026-03-01", "2026-03-02");

        for (String routeId : routes) {
            for (String date : dates) {
                String url = PROD_BASE_URL + "/api/v1/routes/" + routeId + "/trips?date=" + date;
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    System.err.println("Skip snapshot for " + routeId + " " + date +
                            " due to non-2xx status: " + response.getStatusCode());
                    continue;
                }
                JsonNode json = mapper.readTree(response.getBody());

                Path outPath = Path.of("src/test/resources/snapshots/trips/" + routeId + "/" + date + "-prod.json");
                Files.createDirectories(outPath.getParent());
                mapper.writerWithDefaultPrettyPrinter().writeValue(outPath.toFile(), json);
                System.out.printf("Saved trips snapshot [%s, %s] to %s%n",
                        routeId, date, outPath.toAbsolutePath());
            }
        }
    }*/
}