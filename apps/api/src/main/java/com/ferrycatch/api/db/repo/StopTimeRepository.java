package com.ferrycatch.api.db.repo;

import com.ferrycatch.api.db.record.StopTimeRow;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class StopTimeRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public StopTimeRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<StopTimeRow> findByTripId(UUID tripId) {
        var sql = """
      SELECT stop_sequence, stop_name, time
      FROM stop_times
      WHERE trip_id = :tripId
      ORDER BY stop_sequence
    """;

        var params = new MapSqlParameterSource().addValue("tripId", tripId);

        return jdbc.query(sql, params, (rs, i) -> new StopTimeRow(
                rs.getInt("stop_sequence"),
                rs.getString("stop_name"),
                rs.getObject("time", java.time.OffsetDateTime.class)
        ));
    }
}
