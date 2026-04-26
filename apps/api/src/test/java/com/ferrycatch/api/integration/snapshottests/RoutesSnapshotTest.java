package com.ferrycatch.api.integration.snapshottests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ferrycatch.api.dto.FerryDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RoutesSnapshotTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void routesShouldMatchProdSnapshotIgnoringIds() throws IOException {
        List<FerryDtos.RouteDto> prodRoutes = loadSnapshot(
                "snapshots/routes-prod.json",
                new TypeReference<>() {}
        );

        ResponseEntity<FerryDtos.RouteDto[]> response =
                restTemplate.getForEntity("/api/v1/routes", FerryDtos.RouteDto[].class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        List<FerryDtos.RouteDto> localRoutes = List.of(
                Objects.requireNonNull(response.getBody())
        );

        assertThat(localRoutes)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .ignoringFields("id")
                .isEqualTo(prodRoutes);
    }


    private <T> T loadSnapshot(String path, TypeReference<T> type) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, type);
        }
    }
}
