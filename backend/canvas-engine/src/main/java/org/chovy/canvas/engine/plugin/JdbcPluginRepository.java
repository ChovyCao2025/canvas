package org.chovy.canvas.engine.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class JdbcPluginRepository implements PluginRegistryService.PluginRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcPluginRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

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
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    @Override
    public void setEnabled(PluginRegistryService.EnableCommand command) {
        jdbcTemplate.update("""
                UPDATE built_in_plugin_registry
                SET enabled = ?
                WHERE plugin_key = ?
                """, command.enabled() ? 1 : 0, command.pluginKey());
    }

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

    private Map<String, Object> readJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("invalid built-in plugin registry JSON", e);
        }
    }
}
