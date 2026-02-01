package com.ferrycatch.api.importer;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
public class MaterializeService {
    private static final ZoneId IST = ZoneId.of("Europe/Istanbul");

    private final NamedParameterJdbcTemplate jdbc;

    public MaterializeService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record TemplateRow(
            UUID id,
            UUID routeId,
            LocalTime dep,
            LocalTime arr,
            boolean noSunHol,
            LocalDate activeUntil,
            LocalDate activeFrom
    ) {}

    @Transactional
    public void materialize(LocalDate startDate, int days) {
        LocalDate end = startDate.plusDays(days);

        // optional: clear existing materialized trips in range
        jdbc.update(
                "DELETE FROM stop_times st USING trips t WHERE st.trip_id=t.id AND t.service_date >= :s AND t.service_date < :e",
                new MapSqlParameterSource().addValue("s", startDate).addValue("e", end)
        );
        jdbc.update(
                "DELETE FROM trips WHERE service_date >= :s AND service_date < :e",
                new MapSqlParameterSource().addValue("s", startDate).addValue("e", end)
        );

        var templates = loadTemplates();
        for (LocalDate d = startDate; d.isBefore(end); d = d.plusDays(1)) {
            boolean isSunday = d.getDayOfWeek() == DayOfWeek.SUNDAY;
            boolean isHoliday = isHoliday(d);

            for (var tt : templates) {
                if (tt.activeFrom() != null && d.isBefore(tt.activeFrom())) continue;
                if (tt.activeUntil() != null && d.isAfter(tt.activeUntil())) continue;
                if (tt.noSunHol() && (isSunday || isHoliday)) continue;

                UUID tripId = UUID.randomUUID();

                OffsetDateTime dep = ZonedDateTime.of(d, tt.dep(), IST).toOffsetDateTime();
                OffsetDateTime arr = ZonedDateTime.of(d, tt.arr(), IST).toOffsetDateTime();
                // if arrival crosses midnight, handle it
                if (tt.arr().isBefore(tt.dep())) {
                    arr = ZonedDateTime.of(d.plusDays(1), tt.arr(), IST).toOffsetDateTime();
                }

                jdbc.update(
                        """
                        INSERT INTO trips(id, route_id, service_date, departure_time, arrival_time)
                        VALUES (:id,:route,:date,:dep,:arr)
                        """,
                        new MapSqlParameterSource()
                                .addValue("id", tripId)
                                .addValue("route", tt.routeId())
                                .addValue("date", d)
                                .addValue("dep", dep)
                                .addValue("arr", arr)
                );

                var stops = loadStopTemplates(tt.id());
                for (var s : stops) {
                    OffsetDateTime stTime = ZonedDateTime.of(d, s.timeLocal(), IST).toOffsetDateTime();
                    // if stop time crosses midnight relative to dep, you can refine later; for now assume same-day unless < dep
                    if (s.timeLocal().isBefore(tt.dep())) {
                        stTime = ZonedDateTime.of(d.plusDays(1), s.timeLocal(), IST).toOffsetDateTime();
                    }

                    jdbc.update(
                            """
                            INSERT INTO stop_times(trip_id, stop_sequence, stop_name, time)
                            VALUES (:trip,:seq,:name,:time)
                            """,
                            new MapSqlParameterSource()
                                    .addValue("trip", tripId)
                                    .addValue("seq", s.seq())
                                    .addValue("name", s.name())
                                    .addValue("time", stTime)
                    );
                }
            }
        }
    }

    private List<TemplateRow> loadTemplates() {
        return jdbc.query(
                """
                SELECT id, route_id, departure_local, arrival_local, no_sundays_and_holidays, active_until, active_from
                FROM trip_templates
                """,
                new MapSqlParameterSource(),
                (rs, i) -> new TemplateRow(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("route_id")),
                        rs.getObject("departure_local", LocalTime.class),
                        rs.getObject("arrival_local", LocalTime.class),
                        rs.getBoolean("no_sundays_and_holidays"),
                        rs.getObject("active_until", LocalDate.class),
                        rs.getObject("active_from", LocalDate.class)
                )
        );
    }

    public record StopTpl(int seq, String name, LocalTime timeLocal) {}

    private List<StopTpl> loadStopTemplates(UUID tripTemplateId) {
        return jdbc.query(
                """
                SELECT stop_sequence, stop_name, time_local
                FROM stop_time_templates
                WHERE trip_template_id = :id
                ORDER BY stop_sequence
                """,
                new MapSqlParameterSource("id", tripTemplateId),
                (rs, i) -> new StopTpl(
                        rs.getInt("stop_sequence"),
                        rs.getString("stop_name"),
                        rs.getObject("time_local", LocalTime.class)
                )
        );
    }

    private boolean isHoliday(LocalDate d) {
        Integer c = jdbc.query(
                "SELECT 1 FROM holidays WHERE day = :d LIMIT 1",
                new MapSqlParameterSource("d", d),
                rs -> rs.next() ? 1 : null
        );
        return c != null;
    }
}
