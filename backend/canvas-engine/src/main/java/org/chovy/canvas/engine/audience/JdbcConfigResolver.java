package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Jdbc Config Resolver 人群计算组件。
 *
 * <p>负责把人群规则、数据源配置和计算任务转换为可执行查询或后台任务结果。
 * <p>该组件处于画布触发与 CDP 数据之间，需关注大数据量查询的边界和失败兜底。
 */
@Component
@RequiredArgsConstructor
public class JdbcConfigResolver {

    /** JSON 解析器，用于读取人群 JDBC 配置。 */
    private final ObjectMapper objectMapper;
    /** 数据源配置 Mapper，用于按 ID 读取真实连接信息。 */
    private final DataSourceConfigMapper dataSourceConfigMapper;

    /**
     * 构建、解析或转换 resolve 相关的业务数据。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param configJson configJson 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    public JdbcConfig resolve(String configJson) throws Exception {
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException("dataSourceConfig is required for JDBC");
        }
        Map<String, Object> config = objectMapper.readValue(configJson, new TypeReference<>() {});
        Long dataSourceId = longValue(config, "dataSourceId");
        // 人群配置只保存数据源引用和表级参数，真实连接信息从统一数据源表读取。
        DataSourceConfigDO dataSource = dataSourceConfigMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new IllegalArgumentException("Data source not found: " + dataSourceId);
        }
        if (dataSource.getEnabled() == null || dataSource.getEnabled() == 0) {
            throw new IllegalArgumentException("Data source disabled: " + dataSourceId);
        }
        if (!"JDBC".equals(dataSource.getType())) {
            throw new IllegalArgumentException("Data source is not JDBC: " + dataSourceId);
        }

        String baseTable = stringValue(config, "baseTable");
        String userIdColumn = stringValue(config, "userIdColumn", "user_id");
        Integer maxRows = config.get("maxRows") instanceof Number number ? number.intValue() : null;
        // 表名/列名无法用 JDBC 参数绑定，只允许简单标识符，阻断通过配置注入 SQL 片段。
        if (!baseTable.matches("[A-Za-z_][A-Za-z0-9_]*") || !userIdColumn.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Illegal table or column name in JDBC config");
        }
        return new JdbcConfig(
                dataSourceId,
                baseTable,
                requireDataSourceField(dataSource.getUrl(), "url"),
                requireDataSourceField(dataSource.getUsername(), "username"),
                requireDataSourceField(dataSource.getPassword(), "password"),
                userIdColumn,
                defaultIfBlank(dataSource.getDriverClassName(), "com.mysql.cj.jdbc.Driver"),
                maxRows
        );
    }

    /**
     * 执行 long Value 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param key key 对应的缓存键、配置键或业务键
     * @return 计算得到的数值结果
     */
    private static Long longValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        throw new IllegalArgumentException("Missing JDBC config field: " + key);
    }

    /**
     * 执行 string Value 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param key key 对应的缓存键、配置键或业务键
     * @return 转换或查询得到的字符串结果
     */
    private static String stringValue(Map<String, Object> config, String key) {
        String value = stringValue(config, key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing JDBC config field: " + key);
        }
        return value;
    }

    /**
     * 执行 string Value 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param key key 对应的缓存键、配置键或业务键
     * @param defaultValue defaultValue 待写入、比较或转换的业务值
     * @return 转换或查询得到的字符串结果
     */
    private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    /**
     * 执行 require Data Source Field 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param key key 对应的缓存键、配置键或业务键
     * @return 转换或查询得到的字符串结果
     */
    private static String requireDataSourceField(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing data source field: " + key);
        }
        return value;
    }

    /**
     * 执行 default If Blank 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param defaultValue defaultValue 待写入、比较或转换的业务值
     * @return 转换或查询得到的字符串结果
     */
    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
