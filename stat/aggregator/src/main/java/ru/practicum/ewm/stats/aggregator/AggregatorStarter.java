package ru.practicum.ewm.stats.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.aggregator.service.EventSimilarityService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

/**
 * Запускается сразу после старта Spring-контекста и блокирует основной поток до остановки
 * приложения — это ожидаемо для сервиса, живущего вокруг единственного poll-цикла Kafka.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AggregatorStarter implements ApplicationRunner {

    private static final Duration POLL_TIMEOUT = Duration.ofMillis(1000);

    private final KafkaConsumer<Long, UserActionAvro> userActionConsumer;
    private final KafkaTemplate<Long, EventSimilarityAvro> eventSimilarityKafkaTemplate;
    private final EventSimilarityService eventSimilarityService;

    @Value("${aggregator.kafka.topic.user-actions}")
    private String userActionsTopic;

    @Value("${aggregator.kafka.topic.events-similarity}")
    private String similarityTopic;

    private volatile boolean running = true;

    @Override
    public void run(ApplicationArguments args) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "aggregator-shutdown"));
        userActionConsumer.subscribe(List.of(userActionsTopic));
        log.info("Aggregator подписан на топик {}", userActionsTopic);

        try {
            while (running) {
                ConsumerRecords<Long, UserActionAvro> records = userActionConsumer.poll(POLL_TIMEOUT);
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    handleRecord(record);
                }
                if (!records.isEmpty()) {
                    userActionConsumer.commitSync();
                }
            }
        } catch (WakeupException e) {
            log.info("Aggregator получил сигнал остановки");
        } catch (Exception e) {
            log.error("Aggregator завершился с ошибкой", e);
        } finally {
            userActionConsumer.close();
        }
    }

    private void handleRecord(ConsumerRecord<Long, UserActionAvro> record) {
        UserActionAvro action = record.value();
        List<EventSimilarityAvro> similarities = eventSimilarityService.handle(action);
        for (EventSimilarityAvro similarity : similarities) {
            eventSimilarityKafkaTemplate.send(similarityTopic, similarity.getEventA(), similarity);
        }
    }

    /** Вызывается из shutdown hook — wakeup() единственный потокобезопасный метод KafkaConsumer. */
    public void stop() {
        running = false;
        userActionConsumer.wakeup();
    }
}
