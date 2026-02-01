package com.ferrycatch.api.db.repo;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StopsRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public StopsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> listStops(String operatorOrNull) {
        var sql = """
                  SELECT DISTINCT stt.stop_name
                  FROM stop_time_templates stt
                  JOIN trip_templates tt ON tt.id = stt.trip_template_id
                  JOIN routes r ON r.id = tt.route_id
                  JOIN operators o ON o.id = r.operator_id
                  WHERE (CAST(:operator AS text) IS NULL OR o.name = CAST(:operator AS text))
                  ORDER BY stt.stop_name
                """;

        var params = new MapSqlParameterSource()
                .addValue("operator", blankToNull(operatorOrNull));

        return jdbc.query(sql, params, (rs, i) -> rs.getString(1));
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        var t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
