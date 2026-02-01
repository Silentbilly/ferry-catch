package com.ferrycatch.api.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Service
public class ScheduleImportService {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper om;

    public ScheduleImportService(NamedParameterJdbcTemplate jdbc, ObjectMapper om) {
        this.jdbc = jdbc;
        this.om = om;
    }

    @Transactional
    public void importFromClasspath(String resourcePath) throws Exception {
        ScheduleJson data;
        try (InputStream in = new ClassPathResource(resourcePath).getInputStream()) {
            data = om.readValue(in, ScheduleJson.class);
        }

        // 1) Clear old templates (child first, then parent)
        jdbc.update("DELETE FROM stop_time_templates", new MapSqlParameterSource());
        jdbc.update("DELETE FROM trip_templates", new MapSqlParameterSource());

        // 2) Import
        for (var r : data.routes()) {
            UUID operatorId = upsertOperator(r.operator());
            UUID routeId = upsertRoute(r.from(), r.to(), operatorId);

            for (var t : r.trips()) {
                // Validate flags
                boolean noSunHol = t.flags() != null && Boolean.TRUE.equals(t.flags().noSundaysAndHolidays());
                LocalDate activeUntil = (t.flags() != null && t.flags().activeUntil() != null && !t.flags().activeUntil().isBlank())
                        ? LocalDate.parse(t.flags().activeUntil())
                        : null;

                // Derive departure/arrival from trip fields
                LocalTime dep = LocalTime.parse(t.departure());
                LocalTime arr = LocalTime.parse(t.arrival());

                UUID templateId = insertTripTemplate(routeId, dep, arr, noSunHol, activeUntil);

                for (var s : t.stops()) {
                    insertStopTimeTemplate(templateId, s.seq(), s.name(), LocalTime.parse(s.time()));
                }
            }
        }
    }

    private UUID upsertOperator(String name) {
        jdbc.update(
                "INSERT INTO operators(name) VALUES (:name) ON CONFLICT (name) DO NOTHING",
                new MapSqlParameterSource("name", name)
        );
        return jdbc.query(
                "SELECT id FROM operators WHERE name = :name",
                new MapSqlParameterSource("name", name),
                rs -> { rs.next(); return UUID.fromString(rs.getString("id")); }
        );
    }

    private UUID upsertRoute(String from, String to, UUID operatorId) {
        jdbc.update(
                """
                INSERT INTO routes("from","to",operator_id)
                VALUES (:from,:to,:op)
                ON CONFLICT ("from","to",operator_id) DO NOTHING
                """,
                new MapSqlParameterSource()
                        .addValue("from", from)
                        .addValue("to", to)
                        .addValue("op", operatorId)
        );
        return jdbc.query(
                """
                SELECT id FROM routes
                WHERE "from"=:from AND "to"=:to AND operator_id=:op
                """,
                new MapSqlParameterSource()
                        .addValue("from", from)
                        .addValue("to", to)
                        .addValue("op", operatorId),
                rs -> { rs.next(); return UUID.fromString(rs.getString("id")); }
        );
    }

    private UUID insertTripTemplate(UUID routeId, LocalTime dep, LocalTime arr, boolean noSunHol, LocalDate activeUntil) {
        return jdbc.query(
                """
                INSERT INTO trip_templates(route_id, departure_local, arrival_local, no_sundays_and_holidays, active_until, active_from)
                VALUES (:routeId, :dep, :arr, :noSunHol, :activeUntil, NULL)
                RETURNING id
                """,
                new MapSqlParameterSource()
                        .addValue("routeId", routeId)
                        .addValue("dep", dep)
                        .addValue("arr", arr)
                        .addValue("noSunHol", noSunHol)
                        .addValue("activeUntil", activeUntil),
                rs -> { rs.next(); return UUID.fromString(rs.getString("id")); }
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
}
