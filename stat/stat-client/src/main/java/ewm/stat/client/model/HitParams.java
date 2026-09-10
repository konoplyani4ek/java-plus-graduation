package ewm.stat.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class HitParams {
    String uri;
    String ip;
    LocalDateTime timestamp;
}
