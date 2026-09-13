package ewm.stats.analyzer.kafka;

import ewm.stats.analyzer.model.EventSimilarityEntity;
import ewm.stats.analyzer.repository.EventSimilarityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventSimilarityListener {

    private final EventSimilarityRepository eventSimilarityRepository;

    @KafkaListener(
            topics = "${analyzer.kafka.topic.events-similarity}",
            containerFactory = "eventSimilarityListenerContainerFactory")
    @Transactional
    public void onMessage(EventSimilarityAvro message) {
        long eventA = Math.min(message.getEventA(), message.getEventB());
        long eventB = Math.max(message.getEventA(), message.getEventB());

        EventSimilarityEntity entity = eventSimilarityRepository.findByEventAAndEventB(eventA, eventB)
                .orElseGet(() -> EventSimilarityEntity.builder()
                        .eventA(eventA)
                        .eventB(eventB)
                        .build());

        entity.setScore(message.getScore());
        entity.setSimilarityTimestamp(message.getTimestamp());

        eventSimilarityRepository.save(entity);
        log.debug("Обновлено сходство: eventA={}, eventB={}, score={}", eventA, eventB, entity.getScore());
    }
}