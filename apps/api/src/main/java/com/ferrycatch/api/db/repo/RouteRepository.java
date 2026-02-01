package com.ferrycatch.api.db.repo;

import com.ferrycatch.api.db.record.RouteRow;
import com.ferrycatch.api.dto.FerryDtos;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class RouteRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public RouteRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<RouteRow> listRoutes(String from, String to, String operator) {
        var sql = """
                    SELECT r.id, r."from", r."to", o.name AS operator
                    FROM routes r
                    JOIN operators o ON o.id = r.operator_id
                    WHERE (:from IS NULL OR r."from" = :from)
                      AND (:to IS NULL OR r."to" = :to)
                      AND (:operator IS NULL OR o.name = :operator)
                    ORDER BY r."from", r."to", o.name
                """;

        var params = new MapSqlParameterSource()
                .addValue("from", blankToNull(from))
                .addValue("to", blankToNull(to))
                .addValue("operator", blankToNull(operator));

        return jdbc.query(sql, params, (rs, i) -> new RouteRow(
                UUID.fromString(rs.getString("id")),
                rs.getString("from"),
                rs.getString("to"),
                rs.getString("operator")
        ));
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        var t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public List<FerryDtos.RouteDto> listAll() {
        var sql = """
      SELECT r.id, r."from", r."to", o.name AS operator
      FROM routes r
      JOIN operators o ON o.id = r.operator_id
      ORDER BY o.name, r."from", r."to"
    """;

        return jdbc.query(sql, new MapSqlParameterSource(), (rs, i) -> new FerryDtos.RouteDto(
                UUID.fromString(rs.getString("id")),
                rs.getString("from"),
                rs.getString("to"),
                rs.getString("operator")
        ));
    }

}
