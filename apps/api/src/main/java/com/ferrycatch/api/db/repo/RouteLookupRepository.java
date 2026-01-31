package com.ferrycatch.api.db.repo;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class RouteLookupRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public RouteLookupRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID findRouteId(String from, String to, String operatorOrNull) {
        var sql = """
          SELECT r.id
          FROM routes r
          JOIN operators o ON o.id = r.operator_id
          WHERE r."from" = :from
            AND r."to" = :to
            AND (:operator IS NULL OR o.name = :operator)
          ORDER BY r.id
          LIMIT 1
        """;

        var params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to)
                .addValue("operator", blankToNull(operatorOrNull));

        return jdbc.query(sql, params, rs -> rs.next() ? UUID.fromString(rs.getString("id")) : null);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        var t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
