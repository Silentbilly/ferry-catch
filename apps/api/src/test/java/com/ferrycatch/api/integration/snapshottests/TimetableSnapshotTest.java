package com.ferrycatch.api.integration.snapshottests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("integration")
class TimetableSnapshotTest {

    private static final Path SNAPSHOT_ROOT =
            Paths.get("src/main/resources/schedule/winter_2026");

    private static final String TEST_DATE = "2026-03-11";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    static Stream<Arguments> timetableSnapshots() throws IOException {
        return Files.walk(SNAPSHOT_ROOT)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .map(path -> Arguments.of(Named.of(SNAPSHOT_ROOT.relativize(path).toString(), path)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("timetableSnapshots")
    @DisplayName("Timetable matches local schedule resource")
    void timetableMatchesSnapshot(Path snapshotPath) throws Exception {
        ScheduleFile scheduleFile = objectMapper.readValue(snapshotPath.toFile(), ScheduleFile.class);
        TimetableSnapshot expected = expectedFromScheduleFile(scheduleFile);

        String url = UriComponentsBuilder
                .fromUriString("http://localhost:" + port + "/api/v1/timetable")
                .queryParam("from", expected.route().from())
                .queryParam("to", expected.route().to())
                .queryParamIfPresent("operator", optional(expected.route().operator()))
                .queryParam("date", TEST_DATE)
                .toUriString();

        ResponseEntity<TimetableApiResponse> response =
                restTemplate.getForEntity(url, TimetableApiResponse.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        TimetableSnapshot actual = normalize(response.getBody());

        assertThat(actual.route.operator).isEqualTo(expected.route.operator);
    }

    private TimetableSnapshot expectedFromScheduleFile(ScheduleFile file) {
        RouteSnapshot route = new RouteSnapshot(
                file.operator(),
                file.from(),
                file.to(),
                null
        );

        List<TripSnapshot> trips = file.tripTemplates().stream()
                .map(t -> new TripSnapshot(
                        t.departure(),
                        t.arrival(),
                        t.stops().stream()
                                .map(s -> new StopSnapshot(s.name(), s.seq(), s.time()))
                                .sorted(Comparator.comparingInt(StopSnapshot::sequence))
                                .toList()
                ))
                .sorted(Comparator.comparing(TripSnapshot::departureTime)
                        .thenComparing(TripSnapshot::arrivalTime))
                .toList();

        return new TimetableSnapshot(route, trips);
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

    private Optional<String> optional(String s) {
        return (s == null || s.isBlank()) ? Optional.empty() : Optional.of(s);
    }

    private String blankToNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    public record ScheduleFile(
            String operator,
            String from,
            String to,
            Service service,
            List<TripTemplate> tripTemplates
    ) {}

    public record Service(
            String season,
            String validFrom,
            String validTo
    ) {}

    public record TripTemplate(
            String departure,
            String arrival,
            Flags flags,
            List<ScheduleStop> stops
    ) {}

    public record Flags(
            Boolean noSundaysAndHolidays,
            String activeUntil
    ) {}

    public record ScheduleStop(
            int seq,
            String name,
            String time
    ) {}

    public record RouteSnapshot(String operator, String from, String to, String variant) {}
    public record TimetableSnapshot(RouteSnapshot route, List<TripSnapshot> trips) {}
    public record TripSnapshot(String departureTime, String arrivalTime, List<StopSnapshot> stops) {}
    public record StopSnapshot(String stopName, int sequence, String time) {}

    public record RouteApi(String id, String from, String to, String operator, String variant) {}
    public record TimetableApiResponse(RouteApi route, String date, List<TripApi> trips) {}
    public record TripApi(
            String tripId,
            String operator,
            String from,
            String to,
            String departureTime,
            String arrivalTime,
            List<StopApi> stops
    ) {}
    public record StopApi(String stopName, int sequence, String time) {}
}

