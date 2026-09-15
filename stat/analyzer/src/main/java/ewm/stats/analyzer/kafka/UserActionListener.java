package ewm.stats.analyzer.kafka;

import ewm.stats.analyzer.model.UserActionEntity;
import ewm.stats.analyzer.repository.UserActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserActionListener {

    private final UserActionRepository userActionRepository;

    @Value("${analyzer.weights.view}")
    private double viewWeight;

    @Value("${analyzer.weights.register}")
    private double registerWeight;

    @Value("${analyzer.weights.like}")
    private double likeWeight;

    @KafkaListener(
            topics = "${analyzer.kafka.topic.user-actions}",
            containerFactory = "userActionListenerContainerFactory")
    @Transactional
    public void onMessage(UserActionAvro action) {
        double weight = weightOf(action.getActionType());

        UserActionEntity entity = userActionRepository
                .findByUserIdAndEventId(action.getUserId(), action.getEventId())
                .orElseGet(() -> UserActionEntity.builder()
                        .userId(action.getUserId())
                        .eventId(action.getEventId())
                        .weight(0.0)
                        .actionTimestamp(action.getTimestamp())
                        .build());

        if (weight > entity.getWeight()) {
            entity.setWeight(weight);
        }
        if (entity.getActionTimestamp() == null || action.getTimestamp().isAfter(entity.getActionTimestamp())) {
            entity.setActionTimestamp(action.getTimestamp());
        }

        userActionRepository.save(entity);
        log.debug("Обновлено действие: пользователь={}, событие={}, итоговый вес={}",
                entity.getUserId(), entity.getEventId(), entity.getWeight());
    }

    private double weightOf(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> viewWeight;
            case REGISTER -> registerWeight;
            case LIKE -> likeWeight;
        };
    }
}