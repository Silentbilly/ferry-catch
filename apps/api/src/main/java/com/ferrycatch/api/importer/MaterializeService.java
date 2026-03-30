package com.ferrycatch.api.importer;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
            boolean onlySunHol,
            LocalDate activeUntil,
            LocalDate activeFrom
    ) {
    }

    public record StopTpl(
            UUID tripTemplateId,
            int seq,
            String name,
            LocalTime timeLocal
    ) {
    }

    @Transactional
    public void refreshFromToday(int days) {
        LocalDate today = LocalDate.now(IST);
        refreshRange(today, days);
    }

    @Transactional
    public void refreshRange(LocalDate startDate, int days) {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate must not be null");
        }
        if (days <= 0) {
            throw new IllegalArgumentException("days must be > 0");
        }

        LocalDate endExclusive = startDate.plusDays(days);

        clearFutureFrom(startDate);
        materializeRange(startDate, endExclusive);
    }

    private void materializeRange(LocalDate startInclusive, LocalDate endExclusive) {
        List<TemplateRow> templates = loadTemplates();
        if (templates.isEmpty()) {
            return;
        }

        Map<UUID, List<StopTpl>> stopsByTemplateId = loadAllStopTemplates().stream()
                .collect(Collectors.groupingBy(
                        StopTpl::tripTemplateId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Set<LocalDate> holidays = loadHolidaySet(startInclusive, endExclusive);

        for (LocalDate d = startInclusive; d.isBefore(endExclusive); d = d.plusDays(1)) {
            boolean isSunday = d.getDayOfWeek() == DayOfWeek.SUNDAY;
            boolean isHoliday = holidays.contains(d);
            boolean isSunHol = isSunday || isHoliday;

            for (TemplateRow tt : templates) {
                if (tt.activeFrom() != null && d.isBefore(tt.activeFrom())) {
                    continue;
                }
                if (tt.activeUntil() != null && d.isAfter(tt.activeUntil())) {
                    continue;
                }
                if (tt.noSunHol() && isSunHol) {
                    continue;
                }
                if (tt.onlySunHol() && !isSunHol) {
                    continue;
                }

                List<StopTpl> stops = stopsByTemplateId.getOrDefault(tt.id(), List.of());
                if (stops.isEmpty()) {
                    throw new IllegalStateException("No stop_time_templates found for trip_template_id=" + tt.id());
                }

                UUID tripId = UUID.randomUUID();

                OffsetDateTime dep = ZonedDateTime.of(d, tt.dep(), IST).toOffsetDateTime();

                int arrivalDayShift = tt.arr().isBefore(tt.dep()) ? 1 : 0;
                OffsetDateTime arr = ZonedDateTime
                        .of(d.plusDays(arrivalDayShift), tt.arr(), IST)
                        .toOffsetDateTime();

                jdbc.update(
                        """
                        INSERT INTO trips(id, route_id, service_date, departure_time, arrival_time)
                        VALUES (:id, :route, :date, :dep, :arr)
                        """,
                        new MapSqlParameterSource()
                                .addValue("id", tripId)
                                .addValue("route", tt.routeId())
                                .addValue("date", d)
                                .addValue("dep", dep)
                                .addValue("arr", arr)
                );

                int dayShift = 0;
                LocalTime prevStopTime = null;

                for (StopTpl s : stops) {
                    if (prevStopTime != null && s.timeLocal().isBefore(prevStopTime)) {
                        dayShift++;
                    }

                    OffsetDateTime stTime = ZonedDateTime
                            .of(d.plusDays(dayShift), s.timeLocal(), IST)
                            .toOffsetDateTime();

                    jdbc.update(
                            """
                            INSERT INTO stop_times(trip_id, stop_sequence, stop_name, time)
                            VALUES (:trip, :seq, :name, :time)
                            """,
                            new MapSqlParameterSource()
                                    .addValue("trip", tripId)
                                    .addValue("seq", s.seq())
                                    .addValue("name", s.name())
                                    .addValue("time", stTime)
                    );

                    prevStopTime = s.timeLocal();
                }
            }
        }
    }

    private void clearFutureFrom(LocalDate startInclusive) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("s", startInclusive);

        jdbc.update(
                """
                DELETE FROM stop_times st
                USING trips t
                WHERE st.trip_id = t.id
                  AND t.service_date >= :s
                """,
                params
        );

        jdbc.update(
                """
                DELETE FROM trips
                WHERE service_date >= :s
                """,
                params
        );
    }

    private List<TemplateRow> loadTemplates() {
        return jdbc.query(
                """
                SELECT id,
                       route_id,
                       departure_local,
                       arrival_local,
                       no_sundays_and_holidays,
                       only_sundays_and_holidays,
                       active_until,
                       active_from
                FROM trip_templates
                """,
                new MapSqlParameterSource(),
                (rs, i) -> new TemplateRow(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("route_id")),
                        rs.getObject("departure_local", LocalTime.class),
                        rs.getObject("arrival_local", LocalTime.class),
                        rs.getBoolean("no_sundays_and_holidays"),
                        rs.getBoolean("only_sundays_and_holidays"),
                        rs.getObject("active_until", LocalDate.class),
                        rs.getObject("active_from", LocalDate.class)
                )
        );
    }

    private List<StopTpl> loadAllStopTemplates() {
        return jdbc.query(
                """
                SELECT trip_template_id, stop_sequence, stop_name, time_local
                FROM stop_time_templates
                ORDER BY trip_template_id, stop_sequence
                """,
                new MapSqlParameterSource(),
                (rs, i) -> new StopTpl(
                        UUID.fromString(rs.getString("trip_template_id")),
                        rs.getInt("stop_sequence"),
                        rs.getString("stop_name"),
                        rs.getObject("time_local", LocalTime.class)
                )
        );
    }

    private Set<LocalDate> loadHolidaySet(LocalDate startInclusive, LocalDate endExclusive) {
        List<LocalDate> days = jdbc.query(
                """
                SELECT day
                FROM holidays
                WHERE day >= :s
                  AND day < :e
                """,
                new MapSqlParameterSource()
                        .addValue("s", startInclusive)
                        .addValue("e", endExclusive),
                (rs, i) -> rs.getObject("day", LocalDate.class)
        );

        return new HashSet<>(days);
    }
}