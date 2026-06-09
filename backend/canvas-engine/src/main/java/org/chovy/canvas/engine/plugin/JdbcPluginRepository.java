package org.chovy.canvas.engine.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * JdbcPluginRepository 参与 engine.plugin 场景的画布执行引擎处理。
 */
@Repository
public class JdbcPluginRepository implements PluginRegistryService.PluginRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建 JdbcPluginRepository 实例并注入 engine.plugin 场景依赖。
     * @param jdbcTemplate jdbc template 参数，用于 JdbcPluginRepository 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public JdbcPluginRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * list 查询 engine.plugin 场景的业务数据。
     * @return 返回符合条件的数据列表或视图。
     */
    @Override
    public List<PluginRegistryService.Plugin> list() {
        return jdbcTemplate.query("""
                SELECT plugin_key, display_name, extension_point, enabled,
                       compatibility_json, config_schema_json
                FROM built_in_plugin_registry
                ORDER BY extension_point ASC, plugin_key ASC
                """, (rs, rowNum) -> toPlugin(
                rs.getString("plugin_key"),
                rs.getString("extension_point"),
                rs.getString("display_name"),
                rs.getInt("enabled") == 1,
                rs.getString("compatibility_json"),
                rs.getString("config_schema_json")));
    }

    /**
     * get 查询 engine.plugin 场景的业务数据。
     * @param pluginKey 业务键，用于在同一租户下定位资源。
     * @return 返回 get 流程生成的业务结果。
     */
    @Override
    public PluginRegistryService.Plugin get(String pluginKey) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT plugin_key, display_name, extension_point, enabled,
                           compatibility_json, config_schema_json
                    FROM built_in_plugin_registry
                    WHERE plugin_key = ?
                    """, (rs, rowNum) -> toPlugin(
                    rs.getString("plugin_key"),
                    rs.getString("extension_point"),
                    rs.getString("display_name"),
                    rs.getInt("enabled") == 1,
                    rs.getString("compatibility_json"),
                    rs.getString("config_schema_json")), pluginKey);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    /**
     * setEnabled 处理 engine.plugin 场景的业务逻辑。
     * @param command 命令对象，描述本次业务动作及其参数。
     */
    @Override
    public void setEnabled(PluginRegistryService.EnableCommand command) {
        jdbcTemplate.update("""
                UPDATE built_in_plugin_registry
                SET enabled = ?
                WHERE plugin_key = ?
                """, command.enabled() ? 1 : 0, command.pluginKey());
    }

    /**
     * 将数据库行字段转换为插件注册表领域对象。
     *
     * @param pluginKey 插件 key
     * @param extensionPoint 扩展点
     * @param displayName 展示名称
     * @param enabled 是否启用
     * @param compatibilityJson 兼容性 JSON
     * @param configSchemaJson 配置 schema JSON
     * @return 插件注册表对象
     */
    private PluginRegistryService.Plugin toPlugin(
            String pluginKey,
            String extensionPoint,
            String displayName,
            boolean enabled,
            String compatibilityJson,
            String configSchemaJson) {
        return new PluginRegistryService.Plugin(
                pluginKey,
                extensionPoint,
                displayName,
                enabled,
                readJsonObject(compatibilityJson),
                readJsonObject(configSchemaJson));
    }

    /**
     * 读取插件注册表中的 JSON 对象字段。
     *
     * @param json 数据库中的 JSON 字符串
     * @return 解析后的对象映射，空值返回空 Map
     */
    private Map<String, Object> readJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("invalid built-in plugin registry JSON", e);
        }
    }
}
