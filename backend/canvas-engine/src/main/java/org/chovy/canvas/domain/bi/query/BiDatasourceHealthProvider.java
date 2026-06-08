package org.chovy.canvas.domain.bi.query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BiDatasourceHealthProvider 定义 domain.bi.query 场景中的扩展契约。
 */
public interface BiDatasourceHealthProvider {

    /**
     * 执行 health 流程，围绕 health 完成校验、计算或结果组装。
     *
     * @return 返回 health 汇总后的集合、分页或映射视图。
     */
    List<BiDatasourceHealth> health();

    /**
     * 执行 healthHistory 流程，围绕 health history 完成校验、计算或结果组装。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 health history 汇总后的集合、分页或映射视图。
     */
    default List<BiDatasourceHealthSnapshot> healthHistory(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        LocalDateTime checkedAt = LocalDateTime.now();
        return health().stream()
                .map(item -> new BiDatasourceHealthSnapshot(
                        item.sourceKey(),
                        item.sourceType(),
                        item.available(),
                        item.message(),
                        checkedAt))
                .limit(boundedLimit)
                .toList();
    }

    /**
     * 执行 healthSlo 流程，围绕 health slo 完成校验、计算或结果组装。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 healthSlo 流程生成的业务结果。
     */
    default BiDatasourceHealthSloSummary healthSlo(int limit) {
        return BiDatasourceHealthSloSummary.from(healthHistory(limit));
    }

    /**
     * 执行 empty 流程，围绕 empty 完成校验、计算或结果组装。
     *
     * @return 返回 empty 流程生成的业务结果。
     */
    static BiDatasourceHealthProvider empty() {
        return List::of;
    }
}
