package ewm.stat.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class GetStatsParams {
    LocalDateTime start;
    LocalDateTime end;
    List<String> uris;
    Boolean unique;
}
