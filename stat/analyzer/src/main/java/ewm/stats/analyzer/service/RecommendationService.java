package ewm.stats.analyzer.service;

import ewm.stats.analyzer.model.EventSimilarityEntity;
import ewm.stats.analyzer.model.UserActionEntity;
import ewm.stats.analyzer.repository.EventSimilarityRepository;
import ewm.stats.analyzer.repository.UserActionRepository;
import ewm.stats.analyzer.repository.UserActionRepository.EventWeight;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        List<RecommendedEvent> candidates = eventSimilarityRepository.findByEventAOrEventB(eventId, eventId).stream()
                .map(pair -> otherEventOf(pair, eventId))
                .toList();

        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Long> candidateIds = candidates.stream().map(RecommendedEvent::eventId).toList();
        Set<Long> alreadyInteracted = userActionRepository.findEventIdsByUserIdAndEventIdIn(userId, candidateIds);

        return candidates.stream()
                .filter(candidate -> !alreadyInteracted.contains(candidate.eventId()))
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
                .collect(Collectors.toMap(EventWeightSum::getEventId, EventWeightSum::getTotalWeight));
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

        List<Long> recentEventIds = recentActions.stream()
                .map(UserActionEntity::getEventId)
                .distinct()
                .toList();
        Set<Long> recentEventIdSet = new LinkedHashSet<>(recentEventIds);

        Map<Long, Double> candidateBestSimilarity = new HashMap<>();
        for (EventSimilarityEntity pair : eventSimilarityRepository.findByEventAInOrEventBIn(recentEventIds)) {
            considerCandidate(pair.getEventA(), pair.getEventB(), pair.getScore(),
                    recentEventIdSet, interactedEventIds, candidateBestSimilarity);
            considerCandidate(pair.getEventB(), pair.getEventA(), pair.getScore(),
                    recentEventIdSet, interactedEventIds, candidateBestSimilarity);
        }

        List<Long> candidateIds = candidateBestSimilarity.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .toList();
        if (candidateIds.isEmpty()) {
            return List.of();
        }
        Set<Long> candidateIdSet = new LinkedHashSet<>(candidateIds);

        Map<Long, List<RecommendedEvent>> neighborsByCandidate = new HashMap<>();
        for (EventSimilarityEntity pair : eventSimilarityRepository.findByEventAInOrEventBIn(candidateIds)) {
            collectNeighbor(pair.getEventA(), pair.getEventB(), pair.getScore(),
                    candidateIdSet, interactedEventIds, neighborsByCandidate);
            collectNeighbor(pair.getEventB(), pair.getEventA(), pair.getScore(),
                    candidateIdSet, interactedEventIds, neighborsByCandidate);
        }

        Map<Long, List<RecommendedEvent>> topNeighborsByCandidate = new HashMap<>();
        Set<Long> allNeededNeighborIds = new LinkedHashSet<>();
        for (Long candidateId : candidateIds) {
            List<RecommendedEvent> topNeighbors = neighborsByCandidate
                    .getOrDefault(candidateId, List.of()).stream()
                    .sorted(Comparator.comparingDouble(RecommendedEvent::score).reversed())
                    .limit(neighborsCount)
                    .toList();
            topNeighborsByCandidate.put(candidateId, topNeighbors);
            topNeighbors.forEach(n -> allNeededNeighborIds.add(n.eventId()));
        }

        Map<Long, Double> userWeightByEventId = userActionRepository
                .findWeightsByUserIdAndEventIdIn(userId, new ArrayList<>(allNeededNeighborIds)).stream()
                .collect(Collectors.toMap(EventWeight::getEventId, EventWeight::getWeight));

        List<RecommendedEvent> predictions = new ArrayList<>();
        for (Long candidateId : candidateIds) {
            List<RecommendedEvent> topNeighbors = topNeighborsByCandidate.get(candidateId);
            if (topNeighbors.isEmpty()) {
                continue;
            }

            double weightedSum = 0.0;
            double similaritySum = 0.0;
            for (RecommendedEvent neighbor : topNeighbors) {
                double userWeight = userWeightByEventId.getOrDefault(neighbor.eventId(), 0.0);
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

    private void considerCandidate(long anchor, long other, double score,
                                   Set<Long> anchorSet, Set<Long> excludeSet,
                                   Map<Long, Double> bestScoreByCandidate) {
        if (anchorSet.contains(anchor) && !excludeSet.contains(other)) {
            bestScoreByCandidate.merge(other, score, Math::max);
        }
    }

    private void collectNeighbor(long anchor, long other, double score,
                                 Set<Long> anchorSet, Set<Long> includeSet,
                                 Map<Long, List<RecommendedEvent>> neighborsByAnchor) {
        if (anchorSet.contains(anchor) && includeSet.contains(other)) {
            neighborsByAnchor.computeIfAbsent(anchor, id -> new ArrayList<>())
                    .add(new RecommendedEvent(other, score));
        }
    }

    private RecommendedEvent otherEventOf(EventSimilarityEntity pair, long knownEventId) {
        long otherId = pair.getEventA().equals(knownEventId) ? pair.getEventB() : pair.getEventA();
        return new RecommendedEvent(otherId, pair.getScore());
    }
}