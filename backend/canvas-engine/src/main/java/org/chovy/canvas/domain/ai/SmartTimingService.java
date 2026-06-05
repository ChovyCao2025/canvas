package org.chovy.canvas.domain.ai;

import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SmartTimingService {

    private final ChurnFeatureSnapshotService featureSnapshotService;
    private final AiPredictionProperties properties;

    public SmartTimingService(ChurnFeatureSnapshotService featureSnapshotService,
                              AiPredictionProperties properties) {
        this.featureSnapshotService = featureSnapshotService;
        this.properties = properties;
    }

    public int bestSendHour(String userId, LocalDate runDate) {
        List<EventLogDO> events = featureSnapshotService.recentEvents(userId, runDate).stream()
                .filter(event -> event.getCreatedAt() != null)
                .toList();
        if (events.size() < Math.max(1, properties.getSparseHistoryMinEvents())) {
            return normalizedHour(properties.getDefaultBestSendHour());
        }
        Map<Integer, Long> byHour = events.stream()
                .collect(Collectors.groupingBy(event -> normalizedHour(event.getCreatedAt().getHour()), Collectors.counting()));
        return byHour.entrySet().stream()
                .max(Comparator.<Map.Entry<Integer, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(entry -> -entry.getKey()))
                .map(Map.Entry::getKey)
                .orElse(normalizedHour(properties.getDefaultBestSendHour()));
    }

    private int normalizedHour(int hour) {
        if (hour < 0) {
            return 0;
        }
        if (hour > 23) {
            return 23;
        }
        return hour;
    }
}
