package com.ferrycatch.api.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleImportService {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper om;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public ScheduleImportService(NamedParameterJdbcTemplate jdbc, ObjectMapper om) {
        this.jdbc = jdbc;
        this.om = om;
    }

    @Transactional
    public void importFromClasspath(String resourcePath) throws Exception {
        String normalizedPath = normalizeResourcePath(resourcePath);
        List<ResourceWithPath> resources = resolveJsonResources(normalizedPath);

        if (resources.isEmpty()) {
            throw new IllegalArgumentException("No JSON resources found for path: " + normalizedPath);
        }

        List<ParsedResource> parsedResources = parseResources(resources);
        replaceScope(parsedResources);
        importParsedResources(parsedResources);
    }

    private List<ParsedResource> parseResources(List<ResourceWithPath> resources) throws Exception {
        List<ParsedResource> parsed = new ArrayList<>();

        for (ResourceWithPath resourceWithPath : resources) {
            ScheduleJson data;
            try (InputStream in = resourceWithPath.resource().getInputStream()) {
                data = om.readValue(in, ScheduleJson.class);
            }

            if (data == null) {
                throw new IllegalArgumentException("Schedule file is empty: " + resourceWithPath.path());
            }

            validateRoute(data, resourceWithPath.path());
            parsed.add(new ParsedResource(resourceWithPath.path(), data));
        }

        return parsed;
    }

    private void importParsedResources(List<ParsedResource> parsedResources) {
        for (ParsedResource parsed : parsedResources) {
            importParsedResource(parsed);
        }
    }

    private void importParsedResource(ParsedResource parsed) {
        String logicalPath = parsed.logicalPath();
        ScheduleJson data = parsed.schedule();

        UUID operatorId = upsertOperator(data.operator().trim());
        UUID routeId = upsertRoute(
                data.from().trim(),
                data.to().trim(),
                data.operator().trim(),
                data.variant(),
                operatorId
        );

        for (var t : data.tripTemplates()) {
            ValidatedTrip vt = validateTrip(data, t, logicalPath);

            UUID templateId = insertTripTemplate(
                    routeId,
                    vt.departure(),
                    vt.arrival(),
                    vt.noSundaysAndHolidays(),
                    vt.onlySundaysAndHolidays(),
                    vt.activeFrom(),
                    vt.activeUntil()
            );

            for (var s : vt.stops()) {
                insertStopTimeTemplate(templateId, s.seq(), s.name(), s.time());
            }
        }
    }

    private void replaceScope(List<ParsedResource> parsedResources) {
        LinkedHashSet<RouteKey> routeKeys = new LinkedHashSet<>();

        for (ParsedResource parsed : parsedResources) {
            ScheduleJson s = parsed.schedule();
            routeKeys.add(new RouteKey(
                    s.operator().trim(),
                    s.from().trim(),
                    s.to().trim(),
                    normalizeVariant(s.variant())
            ));
        }

        if (routeKeys.isEmpty()) {
            return;
        }

        List<UUID> routeIds = new ArrayList<>();

        for (RouteKey key : routeKeys) {
            UUID operatorId = upsertOperator(key.operatorName());
            UUID routeId = findRouteId(key.from(), key.to(), operatorId, key.variant());
            if (routeId != null) {
                routeIds.add(routeId);
            }
        }

        if (routeIds.isEmpty()) {
            return;
        }

        MapSqlParameterSource params = new MapSqlParameterSource("routeIds", routeIds);

        jdbc.update(
                """
                DELETE FROM stop_times
                WHERE trip_id IN (
                    SELECT t.id
                    FROM trips t
                    WHERE t.route_id IN (:routeIds)
                )
                """,
                params
        );

        jdbc.update(
                """
                DELETE FROM trips
                WHERE route_id IN (:routeIds)
                """,
                params
        );

        jdbc.update(
                """
                DELETE FROM stop_time_templates
                WHERE trip_template_id IN (
                    SELECT tt.id
                    FROM trip_templates tt
                    WHERE tt.route_id IN (:routeIds)
                )
                """,
                params
        );

        jdbc.update(
                """
                DELETE FROM trip_templates
                WHERE route_id IN (:routeIds)
                """,
                params
        );

        jdbc.update(
                """
                DELETE FROM routes
                WHERE id IN (:routeIds)
                """,
                params
        );
    }

    private List<ResourceWithPath> resolveJsonResources(String path) throws Exception {
        if (path.endsWith(".json")) {
            ClassPathResource single = new ClassPathResource(path);
            if (!single.exists()) {
                throw new IllegalArgumentException("Classpath resource not found: " + path);
            }
            return List.of(new ResourceWithPath(single, path));
        }

        String pattern = "classpath*:" + trimSlashes(path) + "/**/*.json";
        Resource[] found = resolver.getResources(pattern);

        if (found == null || found.length == 0) {
            return List.of();
        }

        return Arrays.stream(found)
                .filter(Resource::exists)
                .map(r -> new ResourceWithPath(r, extractLogicalPath(r)))
                .sorted(Comparator.comparing(ResourceWithPath::path))
                .collect(Collectors.toList());
    }

    private String extractLogicalPath(Resource resource) {
        try {
            String uri = resource.getURI().toString();
            int idx = uri.indexOf("/schedule/");
            if (idx >= 0) {
                return uri.substring(idx + 1);
            }
        } catch (Exception ignored) {
        }

        try {
            return resource.getURL().toString();
        } catch (Exception e) {
            return resource.getDescription();
        }
    }

    private void validateRoute(ScheduleJson route, String logicalPath) {
        if (isBlank(route.operator())) {
            throw new IllegalArgumentException("Route operator must not be blank in " + logicalPath);
        }
        if (isBlank(route.from())) {
            throw new IllegalArgumentException("Route from must not be blank in " + logicalPath);
        }
        if (isBlank(route.to())) {
            throw new IllegalArgumentException("Route to must not be blank in " + logicalPath);
        }
        if (route.tripTemplates() == null || route.tripTemplates().isEmpty()) {
            throw new IllegalArgumentException(
                    "Route must contain at least one tripTemplate in " + logicalPath + ": "
                            + route.operator() + " | " + route.from() + " -> " + route.to()
            );
        }

        String normalizedVariant = normalizeVariant(route.variant());
        if (route.from().equals(route.to()) && normalizedVariant == null) {
            throw new IllegalArgumentException(
                    "Loop route must have variant in " + logicalPath + ": "
                            + route.operator() + " | " + route.from() + " -> " + route.to()
            );
        }
    }

    private ValidatedTrip validateTrip(ScheduleJson route, ScheduleJson.TripJson trip, String logicalPath) {
        if (trip == null) {
            throw new IllegalArgumentException(routeLabel(route, logicalPath) + " contains null tripTemplate");
        }

        boolean noSunHol = trip.flags() != null && Boolean.TRUE.equals(trip.flags().noSundaysAndHolidays());
        boolean onlySunHol = trip.flags() != null && Boolean.TRUE.equals(trip.flags().onlySundaysAndHolidays());
        if (noSunHol && onlySunHol) {
            throw new IllegalArgumentException(routeLabel(route, logicalPath)
                    + " trip cannot have both noSundaysAndHolidays=true and onlySundaysAndHolidays=true");
        }

        LocalDate activeFrom =
                (route.service() != null && !isBlank(route.service().validFrom()))
                        ? LocalDate.parse(route.service().validFrom().trim())
                        : null;

        LocalDate activeUntil =
                (trip.flags() != null && !isBlank(trip.flags().activeUntil()))
                        ? LocalDate.parse(trip.flags().activeUntil().trim())
                        : (route.service() != null && !isBlank(route.service().validTo())
                                ? LocalDate.parse(route.service().validTo().trim())
                                : null);

        LocalTime departure = parseRequiredTime(
                trip.departure(),
                routeLabel(route, logicalPath) + " trip departure"
        );
        LocalTime arrival = parseRequiredTime(
                trip.arrival(),
                routeLabel(route, logicalPath) + " trip arrival"
        );

        if (trip.stops() == null || trip.stops().isEmpty()) {
            throw new IllegalArgumentException(routeLabel(route, logicalPath)
                    + " trip " + departure + " -> " + arrival + " must contain at least one stop");
        }

        List<ValidatedStop> stops = new ArrayList<>();
        Set<Integer> seenSeq = new HashSet<>();
        Integer prevSeq = null;

        for (var s : trip.stops()) {
            if (s == null) {
                throw new IllegalArgumentException(routeLabel(route, logicalPath)
                        + " trip " + departure + " -> " + arrival + " contains null stop");
            }
            if (s.seq() <= 0) {
                throw new IllegalArgumentException(routeLabel(route, logicalPath)
                        + " trip " + departure + " -> " + arrival + " has non-positive seq: " + s.seq());
            }
            if (!seenSeq.add(s.seq())) {
                throw new IllegalArgumentException(routeLabel(route, logicalPath)
                        + " trip " + departure + " -> " + arrival + " has duplicate seq: " + s.seq());
            }
            if (prevSeq != null && s.seq() <= prevSeq) {
                throw new IllegalArgumentException(routeLabel(route, logicalPath)
                        + " trip " + departure + " -> " + arrival + " has non-increasing seq order at: " + s.seq());
            }
            if (isBlank(s.name())) {
                throw new IllegalArgumentException(routeLabel(route, logicalPath)
                        + " trip " + departure + " -> " + arrival + " has blank stop name at seq " + s.seq());
            }

            LocalTime stopTime = parseRequiredTime(
                    s.time(),
                    routeLabel(route, logicalPath)
                            + " trip " + departure + " -> " + arrival
                            + " stop time for seq " + s.seq()
            );

            stops.add(new ValidatedStop(s.seq(), s.name().trim(), stopTime));
            prevSeq = s.seq();
        }

        boolean loopRoute = route.from().equals(route.to());

        if (loopRoute) {
            LocalTime firstStopTime = stops.get(0).time();
            LocalTime lastStopTime = stops.get(stops.size() - 1).time();

            if (!departure.equals(firstStopTime)) {
                throw new IllegalArgumentException(routeLabel(route, logicalPath)
                        + " loop trip departure " + departure
                        + " must equal first stop time " + firstStopTime);
            }

            if (!arrival.equals(lastStopTime)) {
                throw new IllegalArgumentException(routeLabel(route, logicalPath)
                        + " loop trip arrival " + arrival
                        + " must equal last stop time " + lastStopTime);
            }
        }

        return new ValidatedTrip(
                departure,
                arrival,
                noSunHol,
                onlySunHol,
                activeFrom,
                activeUntil,
                stops
        );
    }

    private LocalTime parseRequiredTime(String value, String label) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return LocalTime.parse(value.trim());
    }

    private UUID upsertOperator(String name) {
        jdbc.update(
                "INSERT INTO operators(name) VALUES (:name) ON CONFLICT (name) DO NOTHING",
                new MapSqlParameterSource("name", name)
        );
        return jdbc.query(
                "SELECT id FROM operators WHERE name = :name",
                new MapSqlParameterSource("name", name),
                rs -> {
                    rs.next();
                    return UUID.fromString(rs.getString("id"));
                }
        );
    }

    private UUID upsertRoute(String from, String to, String operator, String variant, UUID operatorId) {
        String normalizedVariant = normalizeVariant(variant);

        if (from.equals(to) && normalizedVariant == null) {
            throw new IllegalArgumentException(
                    "Loop route must have variant: " + operator + " | " + from + " -> " + to
            );
        }

        UUID existingId = findRouteId(from, to, operatorId, normalizedVariant);
        if (existingId != null) {
            return existingId;
        }

        jdbc.update(
                """
                INSERT INTO routes("from","to",operator_id,variant)
                VALUES (:from,:to,:op,:variant)
                """,
                new MapSqlParameterSource()
                        .addValue("from", from)
                        .addValue("to", to)
                        .addValue("op", operatorId)
                        .addValue("variant", normalizedVariant)
        );

        UUID insertedId = findRouteId(from, to, operatorId, normalizedVariant);
        if (insertedId == null) {
            throw new IllegalStateException(
                    "Failed to resolve route after insert: " + operator + " | " + from + " -> " + to
                            + (normalizedVariant != null ? " [" + normalizedVariant + "]" : "")
            );
        }

        return insertedId;
    }

    private UUID findRouteId(String from, String to, UUID operatorId, String normalizedVariant) {
        List<UUID> ids;

        if (normalizedVariant == null) {
            ids = jdbc.query(
                    """
                    SELECT id
                    FROM routes
                    WHERE "from" = :from
                      AND "to" = :to
                      AND operator_id = :op
                      AND variant IS NULL
                    """,
                    new MapSqlParameterSource()
                            .addValue("from", from)
                            .addValue("to", to)
                            .addValue("op", operatorId),
                    (rs, rowNum) -> UUID.fromString(rs.getString("id"))
            );
        } else {
            ids = jdbc.query(
                    """
                    SELECT id
                    FROM routes
                    WHERE "from" = :from
                      AND "to" = :to
                      AND operator_id = :op
                      AND variant = :variant
                    """,
                    new MapSqlParameterSource()
                            .addValue("from", from)
                            .addValue("to", to)
                            .addValue("op", operatorId)
                            .addValue("variant", normalizedVariant),
                    (rs, rowNum) -> UUID.fromString(rs.getString("id"))
            );
        }

        if (ids.isEmpty()) {
            return null;
        }
        if (ids.size() > 1) {
            throw new IllegalStateException(
                    "Multiple routes found for from=" + from
                            + ", to=" + to
                            + ", operatorId=" + operatorId
                            + ", variant=" + normalizedVariant
            );
        }
        return ids.get(0);
    }

    private UUID insertTripTemplate(UUID routeId,
                                    LocalTime dep,
                                    LocalTime arr,
                                    boolean noSunHol,
                                    boolean onlySunHol,
                                    LocalDate activeFrom,
                                    LocalDate activeUntil) {
        return jdbc.query(
                """
                INSERT INTO trip_templates(
                    route_id,
                    departure_local,
                    arrival_local,
                    no_sundays_and_holidays,
                    only_sundays_and_holidays,
                    active_from,
                    active_until
                )
                VALUES (:routeId, :dep, :arr, :noSunHol, :onlySunHol, :activeFrom, :activeUntil)
                RETURNING id
                """,
                new MapSqlParameterSource()
                        .addValue("routeId", routeId)
                        .addValue("dep", dep)
                        .addValue("arr", arr)
                        .addValue("noSunHol", noSunHol)
                        .addValue("onlySunHol", onlySunHol)
                        .addValue("activeFrom", activeFrom)
                        .addValue("activeUntil", activeUntil),
                rs -> {
                    rs.next();
                    return UUID.fromString(rs.getString("id"));
                }
        );
    }

    private void insertStopTimeTemplate(UUID tripTemplateId, int seq, String name, LocalTime time) {
        jdbc.update(
                """
                INSERT INTO stop_time_templates(trip_template_id, stop_sequence, stop_name, time_local)
                VALUES (:tt, :seq, :name, :time)
                """,
                new MapSqlParameterSource()
                        .addValue("tt", tripTemplateId)
                        .addValue("seq", seq)
                        .addValue("name", name)
                        .addValue("time", time)
        );
    }

    private String routeLabel(ScheduleJson route, String logicalPath) {
        String normalizedVariant = normalizeVariant(route.variant());
        return logicalPath + " :: "
                + route.operator() + " | " + route.from() + " -> " + route.to()
                + (normalizedVariant != null ? " [" + normalizedVariant + "]" : "");
    }

    private String normalizeResourcePath(String resourcePath) {
        if (isBlank(resourcePath)) {
            throw new IllegalArgumentException("resourcePath must not be blank");
        }
        return trimSlashes(resourcePath.trim());
    }

    private String trimSlashes(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String normalizeVariant(String variant) {
        if (variant == null) return null;
        String v = variant.trim();
        return v.isEmpty() ? null : v;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record ResourceWithPath(Resource resource, String path) {
    }

    private record ParsedResource(String logicalPath, ScheduleJson schedule) {
    }

    private record RouteKey(String operatorName, String from, String to, String variant) {
    }

    private record ValidatedTrip(
            LocalTime departure,
            LocalTime arrival,
            boolean noSundaysAndHolidays,
            boolean onlySundaysAndHolidays,
            LocalDate activeFrom,
            LocalDate activeUntil,
            List<ValidatedStop> stops
    ) {
    }

    private record ValidatedStop(
            int seq,
            String name,
            LocalTime time
    ) {
    }
}