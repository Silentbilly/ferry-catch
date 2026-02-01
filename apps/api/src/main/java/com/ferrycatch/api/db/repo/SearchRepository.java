package com.ferrycatch.api.db.repo;

import com.ferrycatch.api.db.record.TripSegmentRow;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class SearchRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public SearchRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public TripSegmentRow findBestNextSegment(String from, String to, String operatorOrNull) {
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
          WHERE (CAST(:operator AS text) IS NULL OR o.name = CAST(:operator AS text))
            AND st_from.time >= now()
          ORDER BY st_from.time
          LIMIT 1
        """;

        var params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to)
                .addValue("operator", blankToNull(operatorOrNull));

        List<TripSegmentRow> rows = jdbc.query(sql, params, (rs, i) -> new TripSegmentRow(
                UUID.fromString(rs.getString("trip_id")),
                UUID.fromString(rs.getString("route_id")),
                rs.getString("operator"),
                rs.getObject("segment_departure_time", OffsetDateTime.class),
                rs.getObject("segment_arrival_time", OffsetDateTime.class),
                rs.getInt("from_seq"),
                rs.getInt("to_seq")
        ));

        return rows.isEmpty() ? null : rows.get(0);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        var t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
