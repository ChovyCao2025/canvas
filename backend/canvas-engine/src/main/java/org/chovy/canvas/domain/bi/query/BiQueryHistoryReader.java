package org.chovy.canvas.domain.bi.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * BiQueryHistoryReader 定义 domain.bi.query 场景中的扩展契约。
 */
@FunctionalInterface
public interface BiQueryHistoryReader {

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    List<BiQueryHistoryItem> recent(Long tenantId, int limit);

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param historyId 业务对象 ID，用于定位具体记录。
     * @return 返回 detail 流程生成的业务结果。
     */
    default Optional<BiQueryHistoryDetail> detail(Long tenantId, Long historyId) {
        return Optional.empty();
    }

    /**
     * 执行 governanceSummary 流程，围绕 governance summary 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 governanceSummary 流程生成的业务结果。
     */
    default BiQueryGovernanceSummary governanceSummary(Long tenantId, int limit) {
        return governanceSummary(tenantId, limit, BiQueryGovernancePolicy.defaults());
    }

    /**
     * 执行 governanceSummary 流程，围绕 governance summary 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param policy policy 参数，用于 governanceSummary 流程中的校验、计算或对象转换。
     * @return 返回 governanceSummary 流程生成的业务结果。
     */
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

    /**
     * 执行 empty 流程，围绕 empty 完成校验、计算或结果组装。
     *
     * @return 返回 empty 流程生成的业务结果。
     */
    static BiQueryHistoryReader empty() {
        return (tenantId, limit) -> List.of();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param entry entry 参数，用于 datasetStats 流程中的校验、计算或对象转换。
     * @param policy policy 参数，用于 datasetStats 流程中的校验、计算或对象转换。
     * @return 返回 datasetStats 流程生成的业务结果。
     */
    private static BiQueryGovernanceSummary.DatasetQueryStats datasetStats(
            Map.Entry<String, List<BiQueryHistoryItem>> entry,
            BiQueryGovernancePolicy policy) {
        // 准备本次处理所需的上下文和中间变量。
        List<BiQueryHistoryItem> items = entry.getValue();
        BiQueryGovernancePolicy.DatasetPolicy datasetPolicy = policy.datasetPolicy(entry.getKey());
        int total = items.size();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<BiQueryHistoryItem> slowItems = items.stream()
                .filter(item -> item.durationMs() >= datasetPolicy.timeoutMs())
                .toList();
        long averageDuration = total == 0
                ? 0L
                : Math.round(items.stream().mapToLong(BiQueryHistoryItem::durationMs).average().orElse(0D));
        long maxDurationMs = items.stream().mapToLong(BiQueryHistoryItem::durationMs).max().orElse(0L);
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 判断业务条件是否成立。
     *
     * @param item item 参数，用于 isSlow 流程中的校验、计算或对象转换。
     * @param policy policy 参数，用于 isSlow 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private static boolean isSlow(BiQueryHistoryItem item, BiQueryGovernancePolicy policy) {
        return item.durationMs() >= policy.datasetPolicy(item.datasetKey()).timeoutMs();
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param item item 参数，用于 isFailed 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private static boolean isFailed(BiQueryHistoryItem item) {
        return "FAILED".equalsIgnoreCase(item.status()) || "BLOCKED".equalsIgnoreCase(item.status());
    }
}
