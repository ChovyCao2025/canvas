package org.chovy.canvas.domain.bi.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BiQueryResult record.
 * @param datasetKey 查询所属数据集 key，用于缓存归属和结果解释.
 * @param columns 返回列元数据，保持请求维度和指标的展示顺序.
 * @param rows 查询结果行；构造器会复制为不可变 Map，避免缓存后被调用方篡改.
 * @param rowCount 查询返回的行数口径，通常等于 rows 大小或后端裁剪后的结果数.
 * @param durationMs 查询耗时毫秒；缓存命中时代表缓存读取耗时.
 * @param sqlHash 编译 SQL 和参数的哈希，用作查询缓存键和审计线索.
 * @param cached 是否来自查询缓存.
 */
public record BiQueryResult(
        String datasetKey,
        List<BiQueryColumn> columns,
        List<Map<String, Object>> rows,
        int rowCount,
        long durationMs,
        String sqlHash,
        boolean cached
) {
    public BiQueryResult {
        columns = List.copyOf(columns);
        rows = rows.stream()
                .map(row -> Collections.unmodifiableMap(new LinkedHashMap<>(row)))
                .toList();
    }

    /**
     * 创建 BiQueryResult 实例并注入 domain.bi.query 场景依赖。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param columns columns 参数，用于 BiQueryResult 流程中的校验、计算或对象转换。
     * @param rows rows 参数，用于 BiQueryResult 流程中的校验、计算或对象转换。
     * @param rowCount row count 参数，用于 BiQueryResult 流程中的校验、计算或对象转换。
     * @param durationMs duration ms 参数，用于 BiQueryResult 流程中的校验、计算或对象转换。
     * @param sqlHash sql hash 参数，用于 BiQueryResult 流程中的校验、计算或对象转换。
     */
    public BiQueryResult(
            String datasetKey,
            List<BiQueryColumn> columns,
            List<Map<String, Object>> rows,
            int rowCount,
            long durationMs,
            String sqlHash) {
        this(datasetKey, columns, rows, rowCount, durationMs, sqlHash, false);
    }

    /**
     * 基于当前结果创建缓存命中视图。
     *
     * @param durationMs 本次缓存读取耗时，覆盖原始查询耗时
     * @return 标记为 cached 的结果副本，列、行和 SQL 哈希保持不变
     */
    public BiQueryResult asCached(long durationMs) {
        return new BiQueryResult(datasetKey, columns, rows, rowCount, durationMs, sqlHash, true);
    }
}
