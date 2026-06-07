package org.chovy.canvas.domain.bi.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@FunctionalInterface
public interface BiQueryHistoryReader {

    List<BiQueryHistoryItem> recent(Long tenantId, int limit);

    default Optional<BiQueryHistoryDetail> detail(Long tenantId, Long historyId) {
        return Optional.empty();
    }

    default BiQueryGovernanceSummary governanceSummary(Long tenantId, int limit) {
        return governanceSummary(tenantId, limit, BiQueryGovernancePolicy.defaults());
    }

    default BiQueryGovernanceSummary governanceSummary(Long tenantId, int limit, BiQueryGovernancePolicy policy) {
        BiQueryGovernancePolicy effectivePolicy = policy == null ? BiQueryGovernancePolicy.defaults() : policy;
        List<BiQueryHistoryItem> items = recent(tenantId, limit);
        int total = items.size();
        int slow = (int) items.stream().filter(item -> isSlow(item, effectivePolicy)).count();
        int failed = (int) items.stream().filter(BiQueryHistoryReader::isFailed).count();
        int cacheHits = (int) items.stream().filter(item -> "CACHE_HIT".equalsIgnoreCase(item.status())).count();
        long averageDuration = total == 0
                ? 0L
                : Math.round(items.stream().mapToLong(BiQueryHistoryItem::durationMs).average().orElse(0D));
        List<BiQueryGovernanceSummary.DatasetQueryStats> datasets = items.stream()
                .collect(Collectors.groupingBy(item -> item.datasetKey() == null ? "unknown" : item.datasetKey()))
                .entrySet()
                .stream()
                .map(entry -> datasetStats(entry, effectivePolicy))
                .sorted((left, right) -> Long.compare(right.maxDurationMs(), left.maxDurationMs()))
                .toList();
        return new BiQueryGovernanceSummary(
                total,
                slow,
                failed,
                cacheHits,
                averageDuration,
                effectivePolicy.defaultTimeoutMs(),
                effectivePolicy.defaultQuotaRows(),
                datasets);
    }

    static BiQueryHistoryReader empty() {
        return (tenantId, limit) -> List.of();
    }

    private static BiQueryGovernanceSummary.DatasetQueryStats datasetStats(
            Map.Entry<String, List<BiQueryHistoryItem>> entry,
            BiQueryGovernancePolicy policy) {
        List<BiQueryHistoryItem> items = entry.getValue();
        BiQueryGovernancePolicy.DatasetPolicy datasetPolicy = policy.datasetPolicy(entry.getKey());
        int total = items.size();
        List<BiQueryHistoryItem> slowItems = items.stream()
                .filter(item -> item.durationMs() >= datasetPolicy.timeoutMs())
                .toList();
        long averageDuration = total == 0
                ? 0L
                : Math.round(items.stream().mapToLong(BiQueryHistoryItem::durationMs).average().orElse(0D));
        long maxDurationMs = items.stream().mapToLong(BiQueryHistoryItem::durationMs).max().orElse(0L);
        return new BiQueryGovernanceSummary.DatasetQueryStats(
                entry.getKey(),
                total,
                slowItems.size(),
                (int) items.stream().filter(BiQueryHistoryReader::isFailed).count(),
                (int) items.stream().filter(item -> "CACHE_HIT".equalsIgnoreCase(item.status())).count(),
                averageDuration,
                maxDurationMs,
                datasetPolicy.timeoutMs(),
                datasetPolicy.quotaRows(),
                (int) slowItems.stream().filter(BiQueryHistoryReader::isFailed).count(),
                (int) slowItems.stream().filter(item -> !"CACHE_HIT".equalsIgnoreCase(item.status())).count(),
                Math.max(0L, maxDurationMs - datasetPolicy.timeoutMs()),
                items.stream().mapToInt(BiQueryHistoryItem::rowCount).max().orElse(0));
    }

    private static boolean isSlow(BiQueryHistoryItem item, BiQueryGovernancePolicy policy) {
        return item.durationMs() >= policy.datasetPolicy(item.datasetKey()).timeoutMs();
    }

    private static boolean isFailed(BiQueryHistoryItem item) {
        return "FAILED".equalsIgnoreCase(item.status()) || "BLOCKED".equalsIgnoreCase(item.status());
    }
}
