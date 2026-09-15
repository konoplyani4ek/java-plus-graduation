package ewm.stats.collector.service;

import ewm.stats.collector.mapper.UserActionMapper;
import ewm.stats.proto.UserActionProto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserActionProducerService {

    private final KafkaTemplate<Long, UserActionAvro> userActionKafkaTemplate;

    @Value("${collector.kafka.topic.user-actions}")
    private String topic;

    public void collect(UserActionProto action) {
        UserActionAvro avro = UserActionMapper.toAvro(action);
        userActionKafkaTemplate.send(topic, avro.getUserId(), avro);
        log.debug("Отправлено действие: пользователь={}, событие={}, тип={}, топик={}",
                avro.getUserId(), avro.getEventId(), avro.getActionType(), topic);
    }
}