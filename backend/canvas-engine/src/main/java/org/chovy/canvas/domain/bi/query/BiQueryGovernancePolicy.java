package org.chovy.canvas.domain.bi.query;

import java.util.Map;

/**
 * BiQueryGovernancePolicy record.
 * @param defaultTimeoutMs 默认查询超时时间，单位毫秒，用于保护交互式 BI 查询.
 * @param defaultQuotaRows 默认最大返回或扫描行数配额，用于限制单次查询资源占用.
 * @param datasets 数据集级治理覆盖策略，key 为数据集业务 key.
 */
public record BiQueryGovernancePolicy(
        long defaultTimeoutMs,
        int defaultQuotaRows,
        Map<String, DatasetPolicy> datasets
) {
    public static final long DEFAULT_TIMEOUT_MS = 30_000L;
    public static final int DEFAULT_QUOTA_ROWS = 1_000_000;

    public BiQueryGovernancePolicy {
        defaultTimeoutMs = defaultTimeoutMs <= 0 ? DEFAULT_TIMEOUT_MS : defaultTimeoutMs;
        defaultQuotaRows = defaultQuotaRows <= 0 ? DEFAULT_QUOTA_ROWS : defaultQuotaRows;
        datasets = datasets == null ? Map.of() : Map.copyOf(datasets);
    }

    /**
     * 构造平台默认的查询治理策略。
     *
     * @return 默认 30 秒超时、100 万行配额且无数据集覆盖的策略
     */
    public static BiQueryGovernancePolicy defaults() {
        return new BiQueryGovernancePolicy(DEFAULT_TIMEOUT_MS, DEFAULT_QUOTA_ROWS, Map.of());
    }

    /**
     * 计算数据集的生效查询治理策略。
     *
     * @param datasetKey 数据集业务 key
     * @return 数据集覆盖策略；未配置时使用默认超时和默认行数配额
     */
    public DatasetPolicy datasetPolicy(String datasetKey) {
        DatasetPolicy configured = datasets.get(datasetKey);
        return configured == null
                /**
                 * 执行 DatasetPolicy 流程，围绕 dataset policy 完成校验、计算或结果组装。
                 *
                 * @param defaultTimeoutMs 时间参数，用于计算窗口、过期或审计时间。
                 * @param defaultQuotaRows default quota rows 参数，用于 DatasetPolicy 流程中的校验、计算或对象转换。
                 * @return 返回 DatasetPolicy 流程生成的业务结果。
                 */
                ? new DatasetPolicy(defaultTimeoutMs, defaultQuotaRows)
                : configured.withDefaults(defaultTimeoutMs, defaultQuotaRows);
    }

    /**
     * DatasetPolicy record.
     * @param timeoutMs 数据集级查询超时时间，非法值继承默认超时.
     * @param quotaRows 数据集级行数配额，非法值继承默认配额.
     */
    public record DatasetPolicy(
        long timeoutMs,
        int quotaRows
    ) {
        /**
         * 执行 withDefaults 流程，围绕 with defaults 完成校验、计算或结果组装。
         *
         * @param defaultTimeoutMs 时间参数，用于计算窗口、过期或审计时间。
         * @param defaultQuotaRows default quota rows 参数，用于 withDefaults 流程中的校验、计算或对象转换。
         * @return 返回 withDefaults 流程生成的业务结果。
         */
        DatasetPolicy withDefaults(long defaultTimeoutMs, int defaultQuotaRows) {
            return new DatasetPolicy(
                    timeoutMs <= 0 ? defaultTimeoutMs : timeoutMs,
                    quotaRows <= 0 ? defaultQuotaRows : quotaRows);
        }
    }
}
