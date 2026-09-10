package ewm.stat.server.storage;

import ewm.stat.dto.StatDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class StatRowMapper implements RowMapper<StatDto> {

    @Override
    public StatDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        return StatDto.builder()
                .app(rs.getString("app"))
                .uri(rs.getString("uri"))
                .hits(rs.getLong("hits")).build();
    }
}
