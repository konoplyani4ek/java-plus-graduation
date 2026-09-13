package ewm.stats.analyzer.service;

import ewm.stats.analyzer.model.EventSimilarityEntity;
import ewm.stats.analyzer.model.UserActionEntity;
import ewm.stats.analyzer.repository.EventSimilarityRepository;
import ewm.stats.analyzer.repository.UserActionRepository;
import ewm.stats.analyzer.repository.UserActionRepository.EventWeightSum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    @Value("${analyzer.recommendations.recent-interactions-limit:10}")
    private int recentInteractionsLimit;

    @Value("${analyzer.recommendations.neighbors-count:3}")
    private int neighborsCount;

    @Transactional(readOnly = true)
    public List<RecommendedEvent> getSimilarEvents(long eventId, long userId, int maxResults) {
        return eventSimilarityRepository.findByEventAOrEventB(eventId, eventId).stream()
                .map(pair -> otherEventOf(pair, eventId))
                .filter(candidate -> !userActionRepository.existsByUserIdAndEventId(userId, candidate.eventId()))
                .sorted(Comparator.comparingDouble(RecommendedEvent::score).reversed())
                .limit(maxResults)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendedEvent> getInteractionsCount(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Double> sums = userActionRepository.sumWeightsByEventIds(eventIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        EventWeightSum::getEventId, EventWeightSum::getTotalWeight));
        return eventIds.stream()
                .map(id -> new RecommendedEvent(id, sums.getOrDefault(id, 0.0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendedEvent> getRecommendationsForUser(long userId, int maxResults) {
        Pageable recentLimit = PageRequest.of(0, recentInteractionsLimit);
        List<UserActionEntity> recentActions =
                userActionRepository.findByUserIdOrderByActionTimestampDesc(userId, recentLimit);
        if (recentActions.isEmpty()) {
            return List.of();
        }

        Set<Long> interactedEventIds = userActionRepository.findEventIdsByUserId(userId);

        Map<Long, Double> candidateBestSimilarity = new HashMap<>();
        for (UserActionEntity recent : recentActions) {
            for (EventSimilarityEntity pair : eventSimilarityRepository
                    .findByEventAOrEventB(recent.getEventId(), recent.getEventId())) {
                RecommendedEvent candidate = otherEventOf(pair, recent.getEventId());
                if (interactedEventIds.contains(candidate.eventId())) {
                    continue;
                }
                candidateBestSimilarity.merge(candidate.eventId(), candidate.score(), Math::max);
            }
        }

        List<Long> candidateIds = candidateBestSimilarity.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .toList();
        if (candidateIds.isEmpty()) {
            return List.of();
        }

        List<RecommendedEvent> predictions = new ArrayList<>();
        for (Long candidateId : candidateIds) {
            List<RecommendedEvent> neighbors = eventSimilarityRepository
                    .findByEventAOrEventB(candidateId, candidateId).stream()
                    .map(pair -> otherEventOf(pair, candidateId))
                    .filter(n -> interactedEventIds.contains(n.eventId()))
                    .sorted(Comparator.comparingDouble(RecommendedEvent::score).reversed())
                    .limit(neighborsCount)
                    .toList();
            if (neighbors.isEmpty()) {
                continue;
            }

            double weightedSum = 0.0;
            double similaritySum = 0.0;
            for (RecommendedEvent neighbor : neighbors) {
                double userWeight = userActionRepository
                        .findByUserIdAndEventId(userId, neighbor.eventId())
                        .map(UserActionEntity::getWeight)
                        .orElse(0.0);
                weightedSum += neighbor.score() * userWeight;
                similaritySum += neighbor.score();
            }
            if (similaritySum == 0.0) {
                continue;
            }
            predictions.add(new RecommendedEvent(candidateId, weightedSum / similaritySum));
        }

        return predictions.stream()
                .sorted(Comparator.comparingDouble(RecommendedEvent::score).reversed())
                .limit(maxResults)
                .toList();
    }

    private RecommendedEvent otherEventOf(EventSimilarityEntity pair, long knownEventId) {
        long otherId = pair.getEventA().equals(knownEventId) ? pair.getEventB() : pair.getEventA();
        return new RecommendedEvent(otherId, pair.getScore());
    }
}