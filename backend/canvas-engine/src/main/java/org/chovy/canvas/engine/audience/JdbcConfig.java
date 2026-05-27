package org.chovy.canvas.engine.audience;

/**
 * Jdbc 人群计算组件。
 *
 * <p>负责把人群规则、数据源配置和计算任务转换为可执行查询或后台任务结果。
 * <p>该组件处于画布触发与 CDP 数据之间，需关注大数据量查询的边界和失败兜底。
 */
public record JdbcConfig(
        /** 数据源配置 ID。 */
        Long dataSourceId,
        /** 人群计算查询的基础表名。 */
        String baseTable,
        /** JDBC 连接地址。 */
        String url,
        /** JDBC 连接用户名。 */
        String username,
        /** JDBC 连接密码。 */
        String password,
        /** 用户 ID 字段名。 */
        String userIdColumn,
        /** JDBC 驱动类名。 */
        String driverClassName,
        /** 单次计算允许读取的最大行数。 */
        Integer maxRows
) {
}
