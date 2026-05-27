package org.chovy.canvas.engine.audience;

/**
 * Jdbc 人群计算组件。
 *
 * <p>负责把人群规则、数据源配置和计算任务转换为可执行查询或后台任务结果。
 * <p>该组件处于画布触发与 CDP 数据之间，需关注大数据量查询的边界和失败兜底。
 */
public record JdbcConfig(
        Long dataSourceId,
        String baseTable,
        String url,
        String username,
        String password,
        String userIdColumn,
        String driverClassName,
        Integer maxRows
) {
}
