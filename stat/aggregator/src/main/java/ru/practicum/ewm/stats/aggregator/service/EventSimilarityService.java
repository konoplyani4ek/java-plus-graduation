package ru.practicum.ewm.stats.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Инкрементальный расчёт косинусного сходства мероприятий по формуле
 * similarity(A, B) = S_min(A, B) / (sqrt(S_A) * sqrt(S_B)),
 * где S_min(A, B) — сумма минимумов весов действий пользователей, взаимодействовавших
 * с обоими мероприятиями, а S_A / S_B — суммы весов действий с мероприятием A / B.
 * <p>
 * Состояние (все три отображения) хранится только в памяти этого инстанса и рассчитано
 * на единственного потребителя в группе {@code aggregator} — при перезапуске сервиса
 * с чистого состояния историю потребуется переиграть с начала топика.
 */
@Service
@Slf4j
public class EventSimilarityService {

    @Value("${aggregator.weights.view}")
    private double viewWeight;

    @Value("${aggregator.weights.register}")
    private double registerWeight;

    @Value("${aggregator.weights.like}")
    private double likeWeight;

    /** eventId -> (userId -> максимальный вес среди всех действий пользователя с этим мероприятием). */
    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();

    /** eventId -> сумма весов действий всех пользователей с этим мероприятием (S_event). */
    private final Map<Long, Double> eventWeightsSum = new HashMap<>();

    /** min(eventA,eventB) -> max(eventA,eventB) -> сумма минимальных весов (S_min). */
    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();

    /**
     * Обрабатывает очередное действие пользователя и возвращает список пересчитанных
     * коэффициентов сходства для всех пар (event, otherEvent), затронутых этим действием.
     * Если максимальный вес пользователя для мероприятия не изменился — пересчёт не нужен,
     * возвращается пустой список.
     */
    public List<EventSimilarityAvro> handle(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        double newWeight = weightOf(action.getActionType());

        Map<Long, Double> usersOfEvent = eventUserWeights.computeIfAbsent(eventId, id -> new HashMap<>());
        double oldWeight = usersOfEvent.getOrDefault(userId, 0.0);

        if (newWeight <= oldWeight) {
            log.debug("Вес действия пользователя {} с мероприятием {} не увеличился ({} <= {}), пропускаем",
                    userId, eventId, newWeight, oldWeight);
            return List.of();
        }

        usersOfEvent.put(userId, newWeight);
        eventWeightsSum.merge(eventId, newWeight - oldWeight, Double::sum);

        List<EventSimilarityAvro> result = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Double>> other : eventUserWeights.entrySet()) {
            long otherEventId = other.getKey();
            if (otherEventId == eventId) {
                continue;
            }

            Double otherUserWeight = other.getValue().get(userId);
            if (otherUserWeight == null) {
                // пользователь не взаимодействовал со вторым мероприятием — вклада в пару нет
                continue;
            }

            double oldMin = Math.min(oldWeight, otherUserWeight);
            double newMin = Math.min(newWeight, otherUserWeight);
            double delta = newMin - oldMin;
            if (delta != 0.0) {
                addMinWeightsSum(eventId, otherEventId, delta);
            }

            double similarity = computeSimilarity(eventId, otherEventId);

            long eventA = Math.min(eventId, otherEventId);
            long eventB = Math.max(eventId, otherEventId);
            result.add(EventSimilarityAvro.newBuilder()
                    .setEventA(eventA)
                    .setEventB(eventB)
                    .setScore(similarity)
                    .setTimestamp(action.getTimestamp())
                    .build());
        }
        return result;
    }

    private double computeSimilarity(long eventA, long eventB) {
        double sMin = getMinWeightsSum(eventA, eventB);
        double sA = eventWeightsSum.getOrDefault(eventA, 0.0);
        double sB = eventWeightsSum.getOrDefault(eventB, 0.0);
        if (sA == 0.0 || sB == 0.0) {
            return 0.0;
        }
        return sMin / (Math.sqrt(sA) * Math.sqrt(sB));
    }

    private void addMinWeightsSum(long eventA, long eventB, double delta) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minWeightsSums.computeIfAbsent(first, id -> new HashMap<>())
                .merge(second, delta, Double::sum);
    }

    private double getMinWeightsSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return minWeightsSums.getOrDefault(first, Map.of()).getOrDefault(second, 0.0);
    }

    private double weightOf(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> viewWeight;
            case REGISTER -> registerWeight;
            case LIKE -> likeWeight;
        };
    }
}
