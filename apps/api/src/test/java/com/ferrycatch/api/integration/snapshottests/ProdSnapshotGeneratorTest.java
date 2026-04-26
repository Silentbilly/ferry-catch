package com.ferrycatch.api.integration.snapshottests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ferrycatch.api.integration.config.SnapshotProperties;
import com.ferrycatch.api.integration.config.SnapshotTestConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SnapshotTestConfig.class)
@TestPropertySource(properties = {
        "ferrycatch.snapshot.prod-base-url=https://ferrynow.app",
        "ferrycatch.snapshot.date=2026-03-11",
        "ferrycatch.snapshot.output-dir=src/test/resources/snapshots"
})
@Tag("integration")
class ProdSnapshotGeneratorTest {

    @Autowired
    private RestTemplate prodRestTemplate;

    @Autowired
    private SnapshotProperties props;

    private final ObjectMapper om = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void generateSnapshotsFromProd() throws IOException {
        if (props.prodBaseUrl() == null || props.prodBaseUrl().isBlank()) {
            throw new IllegalStateException("ferrycatch.snapshot.prod-base-url is not configured");
        }

        Path snapshotRoot = Path.of(props.outputDir());
        Path timetablesDir = snapshotRoot.resolve("timetables");

        Files.createDirectories(snapshotRoot);
        Files.createDirectories(timetablesDir);

        RouteApi[] routesResponse = prodRestTemplate.getForObject("/api/v1/routes", RouteApi[].class);

        if (routesResponse == null || routesResponse.length == 0) {
            throw new IllegalStateException("Prod /api/v1/routes returned empty list");
        }

        List<RouteSnapshot> routes = Arrays.stream(routesResponse)
                .map(r -> new RouteSnapshot(
                        decode(r.operator()),
                        decode(r.from()),
                        decode(r.to()),
                        blankToNull(decode(r.variant()))
                ))
                .sorted(Comparator.comparing(RouteSnapshot::operator)
                        .thenComparing(RouteSnapshot::from)
                        .thenComparing(RouteSnapshot::to)
                        .thenComparing(r -> r.variant() == null ? "" : r.variant()))
                .toList();

        om.writeValue(snapshotRoot.resolve("routes-prod.json").toFile(), routes);

        for (RouteSnapshot route : routes) {
            String url = UriComponentsBuilder.fromPath("/api/v1/timetable")
                    .queryParam("from", route.from())
                    .queryParam("to", route.to())
                    .queryParamIfPresent("operator", optional(route.operator()))
                    .queryParam("date", props.date())
                    .toUriString();

            TimetableApiResponse body = prodRestTemplate.getForObject(url, TimetableApiResponse.class);
            String raw = prodRestTemplate.getForObject(url, String.class);

            System.out.println(raw);
            System.out.println(url);

            if (body == null) {
                throw new IllegalStateException("Null timetable for " + routeKey(route));
            }

            TimetableSnapshot snapshot = normalize(body);
            om.writeValue(timetablesDir.resolve(snapshotFileName(route)).toFile(), snapshot);
        }
    }

    private TimetableSnapshot normalize(TimetableApiResponse body) {
        RouteSnapshot route = new RouteSnapshot(
                body.route().operator(),
                body.route().from(),
                body.route().to(),
                blankToNull(body.route().variant())
        );

        List<TripSnapshot> trips = body.trips().stream()
                .map(t -> new TripSnapshot(
                        t.departureTime(),
                        t.arrivalTime(),
                        t.stops().stream()
                                .map(s -> new StopSnapshot(s.stopName(), s.sequence(), s.time()))
                                .sorted(Comparator.comparingInt(StopSnapshot::sequence))
                                .toList()
                ))
                .sorted(Comparator.comparing(TripSnapshot::departureTime)
                        .thenComparing(TripSnapshot::arrivalTime))
                .toList();

        return new TimetableSnapshot(route, trips);
    }

    private String decode(String s) {
        if (s == null || s.isBlank()) {
            return s;
        }
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private Optional<String> optional(String s) {
        return (s == null || s.isBlank()) ? Optional.empty() : Optional.of(s);
    }

    private String blankToNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private String snapshotFileName(RouteSnapshot route) {
        String base = slug(route.operator()) + "__" + slug(route.from()) + "__" + slug(route.to());
        if (route.variant() != null) {
            base += "__" + slug(route.variant());
        }
        return base + ".json";
    }

    private String routeKey(RouteSnapshot route) {
        return route.operator() + "|" + route.from() + "|" + route.to()
                + (route.variant() == null ? "" : "|" + route.variant());
    }

    private String slug(String s) {
        return s.toLowerCase()
                .replace("ş", "s")
                .replace("ı", "i")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ö", "o")
                .replace("ç", "c")
                .replace(" ", "-")
                .replace("→", "-")
                .replaceAll("[^a-z0-9-]+", "")
                .replaceAll("-+", "-");
    }

    public record RouteSnapshot(String operator, String from, String to, String variant) {}
    public record TimetableSnapshot(RouteSnapshot route, List<TripSnapshot> trips) {}
    public record TripSnapshot(String departureTime, String arrivalTime, List<StopSnapshot> stops) {}
    public record StopSnapshot(String stopName, int sequence, String time) {}

    public record RouteApi(String id, String from, String to, String operator, String variant) {}
    public record TimetableApiResponse(RouteApi route, String date, List<TripApi> trips) {}
    public record TripApi(String tripId, String operator, String from, String to,
                          String departureTime, String arrivalTime, List<StopApi> stops) {}
    public record StopApi(String stopName, int sequence, String time) {}
}