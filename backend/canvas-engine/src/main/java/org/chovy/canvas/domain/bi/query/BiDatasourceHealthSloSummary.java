package org.chovy.canvas.domain.bi.query;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BiDatasourceHealthSloSummary 数据记录。
 */
public record BiDatasourceHealthSloSummary(
        int totalChecks,
        int availableChecks,
        int unavailableChecks,
        double availabilityRate,
        List<SourceSlo> sources
) {

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param snapshots snapshots 参数，用于 from 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    public static BiDatasourceHealthSloSummary from(List<BiDatasourceHealthSnapshot> snapshots) {
        return from(snapshots, LocalDateTime.now(), 120);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param snapshots snapshots 参数，用于 from 流程中的校验、计算或对象转换。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param staleAfterMinutes stale after minutes 参数，用于 from 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    public static BiDatasourceHealthSloSummary from(List<BiDatasourceHealthSnapshot> snapshots,
                                                    LocalDateTime now,
                                                    int staleAfterMinutes) {
        // 准备本次处理所需的上下文和中间变量。
        List<BiDatasourceHealthSnapshot> rows = snapshots == null ? List.of() : snapshots;
        int total = rows.size();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        int available = (int) rows.stream().filter(BiDatasourceHealthSnapshot::available).count();
        Map<String, SourceAccumulator> bySource = new LinkedHashMap<>();
        LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
        int effectiveStaleAfterMinutes = Math.max(1, staleAfterMinutes);
        for (BiDatasourceHealthSnapshot row : rows) {
            String sourceKey = row.sourceKey() == null ? "-" : row.sourceKey();
            SourceAccumulator accumulator = bySource.computeIfAbsent(sourceKey,
                    ignored -> new SourceAccumulator(sourceKey, row.sourceType()));
            accumulator.add(row);
        }
        return new BiDatasourceHealthSloSummary(
                total,
                available,
                total - available,
                rate(available, total),
                bySource.values().stream()
                        .map(accumulator -> accumulator.toSlo(effectiveNow, effectiveStaleAfterMinutes))
                        .sorted((left, right) -> {
                            int rateCompare = Double.compare(left.availabilityRate(), right.availabilityRate());
                            if (rateCompare != 0) {
                                return rateCompare;
                            }
                            // 汇总前面计算出的状态和明细，返回给调用方。
                            return left.sourceKey().compareTo(right.sourceKey());
                        })
                        .toList());
    }

    /**
     * 执行 rate 流程，围绕 rate 完成校验、计算或结果组装。
     *
     * @param available available 参数，用于 rate 流程中的校验、计算或对象转换。
     * @param total total 参数，用于 rate 流程中的校验、计算或对象转换。
     * @return 返回 rate 计算得到的数量、金额或指标值。
     */
    private static double rate(int available, int total) {
        if (total <= 0) {
            return 100.0;
        }
        return Math.round((available * 10000.0) / total) / 100.0;
    }

    /**
     * SourceSlo 数据记录。
     */
    public record SourceSlo(
            String sourceKey,
            String sourceType,
            int totalChecks,
            int availableChecks,
            int unavailableChecks,
            double availabilityRate,
            LocalDateTime lastCheckedAt,
            String lastMessage,
            String riskLevel,
            String recommendedAction
    ) {
    }

    /**
     * SourceAccumulator 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class SourceAccumulator {
        private final String sourceKey;
        private final String sourceType;
        private int total;
        private int available;
        private LocalDateTime lastCheckedAt;
        private String lastMessage;

        /**
         * 执行 SourceAccumulator 流程，围绕 source accumulator 完成校验、计算或结果组装。
         *
         * @param sourceKey 业务键，用于在同一租户下定位资源。
         * @param sourceType 类型标识，用于选择对应处理分支。
         * @return 返回 SourceAccumulator 流程生成的业务结果。
         */
        private SourceAccumulator(String sourceKey, String sourceType) {
            this.sourceKey = sourceKey;
            this.sourceType = sourceType == null ? "-" : sourceType;
        }

        /**
         * 处理集合、映射或字段拷贝逻辑。
         *
         * @param row 持久化行数据，承载数据库记录内容。
         */
        private void add(BiDatasourceHealthSnapshot row) {
            total++;
            if (row.available()) {
                available++;
            }
            if (lastCheckedAt == null || (row.checkedAt() != null && row.checkedAt().isAfter(lastCheckedAt))) {
                lastCheckedAt = row.checkedAt();
                lastMessage = row.message();
            }
        }

        /**
         * 转换为接口返回或领域视图。
         *
         * @param now 时间参数，用于计算窗口、过期或审计时间。
         * @param staleAfterMinutes stale after minutes 参数，用于 toSlo 流程中的校验、计算或对象转换。
         * @return 返回组装或转换后的结果对象。
         */
        private SourceSlo toSlo(LocalDateTime now, int staleAfterMinutes) {
            // 准备本次处理所需的上下文和中间变量。
            double sourceRate = rate(available, total);
            boolean stale = lastCheckedAt != null && lastCheckedAt.plusMinutes(staleAfterMinutes).isBefore(now);
            String riskLevel = sourceRate < 100.0 ? "DEGRADED" : stale ? "STALE" : "NORMAL";
            String recommendedAction = switch (riskLevel) {
                case "DEGRADED" -> "请执行数据源连接测试并检查网络、凭据和仓库可用性";
                case "STALE" -> "请重新执行健康检查以刷新数据源可用性证据";
                default -> "数据源健康状态正常，保持现有巡检节奏";
            };
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new SourceSlo(
                    sourceKey,
                    sourceType,
                    total,
                    available,
                    total - available,
                    sourceRate,
                    lastCheckedAt,
                    lastMessage == null || lastMessage.isBlank() ? "-" : lastMessage,
                    riskLevel,
                    recommendedAction);
        }
    }
}
