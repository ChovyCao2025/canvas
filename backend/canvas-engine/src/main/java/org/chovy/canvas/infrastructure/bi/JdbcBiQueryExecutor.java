package org.chovy.canvas.infrastructure.bi;

import org.chovy.canvas.domain.bi.query.BiCompiledQuery;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealth;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealthProvider;
import org.chovy.canvas.domain.bi.query.BiQueryExecutor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class JdbcBiQueryExecutor implements BiQueryExecutor, BiDatasourceHealthProvider {

    private final JdbcTemplate primaryJdbcTemplate;
    private final JdbcTemplate dorisJdbcTemplate;
    private final int queryTimeoutSeconds;
    private final int fetchSize;

    public JdbcBiQueryExecutor(
            @Qualifier("jdbcTemplate") ObjectProvider<JdbcTemplate> primaryJdbcTemplate,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            @Value("${canvas.bi.query.timeout-sec:30}") int queryTimeoutSeconds,
            @Value("${canvas.bi.query.fetch-size:1000}") int fetchSize) {
        this.primaryJdbcTemplate = primaryJdbcTemplate.getIfAvailable();
        this.dorisJdbcTemplate = dorisJdbcTemplate.getIfAvailable();
        this.queryTimeoutSeconds = Math.max(1, queryTimeoutSeconds);
        this.fetchSize = Math.max(1, fetchSize);
    }

    @Override
    public List<Map<String, Object>> execute(BiCompiledQuery query, BiDatasetSpec dataset) {
        JdbcTemplate jdbcTemplate = jdbcTemplate(dataset);
        if (jdbcTemplate == null) {
            throw new IllegalStateException("BI datasource is not available for dataset: " + dataset.datasetKey());
        }
        return jdbcTemplate.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(query.sql());
            statement.setQueryTimeout(queryTimeoutSeconds);
            statement.setFetchSize(fetchSize);
            for (int i = 0; i < query.parameters().size(); i++) {
                statement.setObject(i + 1, query.parameters().get(i));
            }
            return statement;
        }, resultSet -> {
            ResultSetMetaData metadata = resultSet.getMetaData();
            int columnCount = metadata.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int column = 1; column <= columnCount; column++) {
                    row.put(metadata.getColumnLabel(column), resultSet.getObject(column));
                }
                rows.add(row);
            }
            return rows;
        });
    }

    private JdbcTemplate jdbcTemplate(BiDatasetSpec dataset) {
        String tableExpression = dataset.tableExpression().toLowerCase(Locale.ROOT);
        if (tableExpression.startsWith("canvas_dws.") || tableExpression.startsWith("canvas_ods.")) {
            return dorisJdbcTemplate;
        }
        return primaryJdbcTemplate;
    }

    @Override
    public List<BiDatasourceHealth> health() {
        return List.of(
                health("primary", "MYSQL", primaryJdbcTemplate),
                health("doris", "DORIS", dorisJdbcTemplate));
    }

    private BiDatasourceHealth health(String sourceKey, String sourceType, JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate == null) {
            return new BiDatasourceHealth(sourceKey, sourceType, false, "disabled");
        }
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return new BiDatasourceHealth(sourceKey, sourceType, true, "available");
        } catch (RuntimeException e) {
            return new BiDatasourceHealth(sourceKey, sourceType, false, e.getMessage());
        }
    }
}
