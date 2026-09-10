package ewm.stat.server.storage;

import ewm.stat.dto.HitDto;
import ewm.stat.dto.StatDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class StatDbStorage implements StatStorage {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    RowMapper<StatDto> statMapper;

    public StatDbStorage(NamedParameterJdbcTemplate jdbcTemplate, RowMapper<StatDto> statMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.statMapper = statMapper;
    }

    @Override
    public void save(HitDto dto) {
        String sql = "INSERT INTO hits (app, uri, ip, created_at) " +
                "VALUES (:app, :uri, :ip, :timestamp)";

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("app", dto.getApp())
                .addValue("uri", dto.getUri())
                .addValue("ip", dto.getIp())
                .addValue("timestamp", dto.getTimestamp());

        jdbcTemplate.update(sql, params);
    }

    @Override
    public List<StatDto> getStats(LocalDateTime start, LocalDateTime end, boolean unique, List<String> uris) {

        String sqlTemplate = """
                SELECT h.app,
                       h.uri,
                       [hits] AS hits
                FROM hits AS h
                WHERE h.created_at BETWEEN :start AND :end
                [urisCondition]
                GROUP BY h.app, h.uri
                ORDER BY [hits] DESC, h.uri
                """;

        String hitsExpression = unique ?
                "COUNT(DISTINCT h.ip)"
                : "COUNT(*)";

        boolean hasUris = (uris != null && !uris.isEmpty());

        String urisCondition =  hasUris ?
                "AND h.uri IN (:uris)"
                : "";

        String query = sqlTemplate
                .replace("[hits]", hitsExpression)
                .replace("[urisCondition]", urisCondition);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", start)
                .addValue("end", end);

        if (hasUris) {
            params.addValue("uris", uris);
        }

        return jdbcTemplate.query(
                query,
                params,
                statMapper
        );
    }

}
