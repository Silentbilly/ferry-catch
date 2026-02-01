package com.ferrycatch.api.db.repo;

import com.ferrycatch.api.dto.FerryDtos;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class RouteWithNextRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public RouteWithNextRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<FerryDtos.RouteWithNextDto> listRoutesWithNext(String from, String to, String operator) {
        var sql = """
                  SELECT
                    r.id AS route_id,
                    r."from",
                    r."to",
                    o.name AS operator,
                    nt.departure_time AS next_departure_time
                  FROM routes r
                  JOIN operators o ON o.id = r.operator_id
                  LEFT JOIN LATERAL (
                    SELECT t.departure_time
                    FROM trips t
                    WHERE t.route_id = r.id
                      AND t.departure_time >= now()
                    ORDER BY t.departure_time
                    LIMIT 1
                  ) nt ON true
                  WHERE (CAST(:from AS text) IS NULL OR r."from" = CAST(:from AS text))
                    AND (CAST(:to AS text) IS NULL OR r."to" = CAST(:to AS text))
                    AND (CAST(:operator AS text) IS NULL OR o.name = CAST(:operator AS text))
                  ORDER BY r."from", r."to", o.name
                """;

        var params = new MapSqlParameterSource()
                .addValue("from", blankToNull(from))
                .addValue("to", blankToNull(to))
                .addValue("operator", blankToNull(operator));

        return jdbc.query(sql, params, (rs, i) -> {
            UUID routeId = UUID.fromString(rs.getString("route_id"));

            OffsetDateTime dep = rs.getObject("next_departure_time", OffsetDateTime.class);
            String depStr = dep == null ? null : dep.toString();

            Integer minutes = null;
            if (dep != null) {
                minutes = (int) Math.max(0, Duration.between(OffsetDateTime.now(), dep).toMinutes());
            }

            return new FerryDtos.RouteWithNextDto(
                    routeId,
                    rs.getString("from"),
                    rs.getString("to"),
                    rs.getString("operator"),
                    minutes,
                    depStr
            );
        });
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        var t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
