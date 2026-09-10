package ewm.stat.client.mapper;

import ewm.stat.client.model.HitParams;
import ewm.stat.dto.HitDto;

public class HitMapper {
    public static HitDto fromHitParams(HitParams params, String app) {

        return HitDto.builder()
                .uri(params.getUri())
                .timestamp(params.getTimestamp())
                .ip(params.getIp())
                .app(app)
                .build();
    }
}
