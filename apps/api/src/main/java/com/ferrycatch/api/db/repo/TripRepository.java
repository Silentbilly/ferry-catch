package com.ferrycatch.api.db.repo;

import com.ferrycatch.api.db.record.TripRow;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class TripRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public TripRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TripRow> findNextTrips(String from, String to, String operatorOrNull, int limit) {
        var sql = """
                  SELECT
                    t.id            AS trip_id,
                    o.name          AS operator,
                    r."from"        AS "from",
                    r."to"          AS "to",
                    t.departure_time,
                    t.arrival_time
                  FROM trips t
                  JOIN routes r    ON r.id = t.route_id
                  JOIN operators o ON o.id = r.operator_id
                  WHERE r."from" = :from
                    AND r."to" = :to
                    AND (:operator IS NULL OR o.name = :operator)
                    AND t.departure_time >= now()
                  ORDER BY t.departure_time
                  LIMIT :limit
                """;

        var params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to)
                .addValue("operator", operatorOrNull)
                .addValue("limit", limit);

        return jdbc.query(sql, params, (rs, i) -> new TripRow(
                UUID.fromString(rs.getString("trip_id")),
                rs.getString("operator"),
                rs.getString("from"),
                rs.getString("to"),
                rs.getObject("departure_time", java.time.OffsetDateTime.class),
                rs.getObject("arrival_time", java.time.OffsetDateTime.class)
        ));
    }

    public List<TripRow> findTripsForDate(String from, String to, String operatorOrNull, LocalDate date) {
        var sql = """
                  SELECT
                    t.id            AS trip_id,
                    o.name          AS operator,
                    r."from"        AS "from",
                    r."to"          AS "to",
                    t.departure_time,
                    t.arrival_time
                  FROM trips t
                  JOIN routes r    ON r.id = t.route_id
                  JOIN operators o ON o.id = r.operator_id
                  WHERE r."from" = :from
                    AND r."to" = :to
                    AND (:operator IS NULL OR o.name = :operator)
                    AND t.service_date = :date
                  ORDER BY t.departure_time
                """;

        var params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to)
                .addValue("operator", operatorOrNull)
                .addValue("date", date);

        return jdbc.query(sql, params, (rs, i) -> new TripRow(
                UUID.fromString(rs.getString("trip_id")),
                rs.getString("operator"),
                rs.getString("from"),
                rs.getString("to"),
                rs.getObject("departure_time", java.time.OffsetDateTime.class),
                rs.getObject("arrival_time", java.time.OffsetDateTime.class)
        ));
    }
}
