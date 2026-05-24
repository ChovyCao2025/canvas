package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JdbcConfigResolver {

    private final ObjectMapper objectMapper;
    private final DataSourceConfigMapper dataSourceConfigMapper;

    public JdbcConfig resolve(String configJson) throws Exception {
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException("dataSourceConfig is required for JDBC");
        }
        Map<String, Object> config = objectMapper.readValue(configJson, new TypeReference<>() {});
        Long dataSourceId = longValue(config, "dataSourceId");
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

    private static String stringValue(Map<String, Object> config, String key) {
        String value = stringValue(config, key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing JDBC config field: " + key);
        }
        return value;
    }

    private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static String requireDataSourceField(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing data source field: " + key);
        }
        return value;
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
