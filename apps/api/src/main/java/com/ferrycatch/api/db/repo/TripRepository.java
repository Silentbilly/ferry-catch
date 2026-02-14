package com.ferrycatch.api.db.repo;

import com.ferrycatch.api.db.record.TripSegmentRow;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class TripRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public TripRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // NEW: next trips for any segment (works for circular and mid-start trips)
    public List<TripSegmentRow> findNextTripSegments(
            String from,
            String to,
            String operatorOrNull,
            int limit
    ) {
        boolean hasOperator = operatorOrNull != null && !operatorOrNull.trim().isEmpty();

        var sql = getNextTripSegmentsSql(hasOperator);

        var params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to)
                .addValue("limit", limit);

        if (hasOperator) {
            params.addValue("operator", operatorOrNull.trim());
        }

        return jdbc.query(sql, params, (rs, i) -> new TripSegmentRow(
                UUID.fromString(rs.getString("trip_id")),
                UUID.fromString(rs.getString("route_id")),
                rs.getString("operator"),
                rs.getObject("segment_departure_time", OffsetDateTime.class),
                rs.getObject("segment_arrival_time", OffsetDateTime.class),
                rs.getInt("from_seq"),
                rs.getInt("to_seq")
        ));
    }

    private static String getNextTripSegmentsSql(boolean hasOperator) {
        var sql = """
                  SELECT
                    t.id AS trip_id,
                    t.route_id,
                    o.name AS operator,
                    st_from.time AS segment_departure_time,
                    st_to.time   AS segment_arrival_time,
                    st_from.stop_sequence AS from_seq,
                    st_to.stop_sequence   AS to_seq
                  FROM trips t
                  JOIN routes r    ON r.id = t.route_id
                  JOIN operators o ON o.id = r.operator_id
                  JOIN stop_times st_from
                    ON st_from.trip_id = t.id
                   AND st_from.stop_name = :from
                  JOIN stop_times st_to
                    ON st_to.trip_id = t.id
                   AND st_to.stop_name = :to
                   AND st_to.stop_sequence > st_from.stop_sequence
                  WHERE st_from.time >= now()
                    AND st_from.time < now() + INTERVAL '1 day'
                """;

        if (hasOperator) {
            sql += "\n  AND o.name = :operator";
        }

        sql += """
                  ORDER BY st_from.time
                  LIMIT :limit
                """;
        return sql;
    }


    public List<TripSegmentRow> findTripSegmentsForDate(String from, String to, String operatorOrNull,
                                                        java.time.LocalDate date) {
        var sql = """
                  SELECT
                    t.id AS trip_id,
                    t.route_id,
                    o.name AS operator,
                    st_from.time AS segment_departure_time,
                    st_to.time   AS segment_arrival_time,
                    st_from.stop_sequence AS from_seq,
                    st_to.stop_sequence   AS to_seq
                  FROM trips t
                  JOIN routes r    ON r.id = t.route_id
                  JOIN operators o ON o.id = r.operator_id
                  JOIN stop_times st_from
                    ON st_from.trip_id = t.id
                   AND st_from.stop_name = CAST(:from AS text)
                  JOIN stop_times st_to
                    ON st_to.trip_id = t.id
                   AND st_to.stop_name = CAST(:to AS text)
                   AND st_to.stop_sequence > st_from.stop_sequence
                  WHERE t.service_date = :date
                    AND (CAST(:operator AS text) IS NULL OR o.name = CAST(:operator AS text))
                  ORDER BY st_from.time
                """;

        var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to)
                .addValue("operator", blankToNull(operatorOrNull))
                .addValue("date", date);

        return jdbc.query(sql, params, (rs, i) -> new com.ferrycatch.api.db.record.TripSegmentRow(
                java.util.UUID.fromString(rs.getString("trip_id")),
                java.util.UUID.fromString(rs.getString("route_id")),
                rs.getString("operator"),
                rs.getObject("segment_departure_time", java.time.OffsetDateTime.class),
                rs.getObject("segment_arrival_time", java.time.OffsetDateTime.class),
                rs.getInt("from_seq"),
                rs.getInt("to_seq")
        ));
    }


    private static String blankToNull(String s) {
        if (s == null) return null;
        var t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
