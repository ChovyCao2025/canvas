package org.chovy.canvas.domain.bi.query;

import java.util.List;
import java.util.Map;

/**
 * BiQueryExecutor 定义 domain.bi.query 场景中的扩展契约。
 */
@FunctionalInterface
public interface BiQueryExecutor {

    /**
     * 执行核心业务处理流程。
     *
     * @param query query 参数，用于 execute 流程中的校验、计算或对象转换。
     * @param dataset dataset 参数，用于 execute 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    List<Map<String, Object>> execute(BiCompiledQuery query, BiDatasetSpec dataset);

    /**
     * 执行核心业务处理流程。
     *
     * @param query query 参数，用于 execute 流程中的校验、计算或对象转换。
     * @param dataset dataset 参数，用于 execute 流程中的校验、计算或对象转换。
     * @param sqlHash sql hash 参数，用于 execute 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    default List<Map<String, Object>> execute(BiCompiledQuery query, BiDatasetSpec dataset, String sqlHash) {
        return execute(query, dataset);
    }

    /**
     * 执行 explain 流程，围绕 explain 完成校验、计算或结果组装。
     *
     * @param query query 参数，用于 explain 流程中的校验、计算或对象转换。
     * @param dataset dataset 参数，用于 explain 流程中的校验、计算或对象转换。
     * @return 返回 explain 汇总后的集合、分页或映射视图。
     */
    default List<String> explain(BiCompiledQuery query, BiDatasetSpec dataset) {
        return List.of("SQL: " + query.sql());
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param sqlHash sql hash 参数，用于 cancel 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    default boolean cancel(String sqlHash) {
        return false;
    }
}
