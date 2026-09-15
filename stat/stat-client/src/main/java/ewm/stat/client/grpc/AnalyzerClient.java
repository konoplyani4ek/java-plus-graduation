package ewm.stat.client.grpc;

import ewm.stats.proto.RecommendedEventProto;

import java.util.List;
import java.util.stream.Stream;

public interface AnalyzerClient {

    Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults);

    Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults);

    Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds);
}