package org.chovy.canvas.infrastructure.bi;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiDatasourceHealthSnapshotDO;
import org.chovy.canvas.dal.mapper.BiDatasourceHealthSnapshotMapper;
import org.chovy.canvas.domain.bi.query.BiCompiledQuery;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealth;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealthProvider;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealthSnapshot;
import org.chovy.canvas.domain.bi.query.BiQueryExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JdbcBiQueryExecutor implements BiQueryExecutor, BiDatasourceHealthProvider {

    private final JdbcTemplate primaryJdbcTemplate;
    private final JdbcTemplate dorisJdbcTemplate;
    private final int queryTimeoutSeconds;
    private final int fetchSize;
    private final BiDatasourceHealthSnapshotMapper healthSnapshotMapper;
    private final Map<String, PreparedStatement> runningStatements = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<BiDatasourceHealthSnapshot> healthSnapshots = new ConcurrentLinkedDeque<>();
    private final int maxHealthSnapshots = 200;

    public JdbcBiQueryExecutor(
            @Qualifier("jdbcTemplate") ObjectProvider<JdbcTemplate> primaryJdbcTemplate,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            @Value("${canvas.bi.query.timeout-sec:30}") int queryTimeoutSeconds,
            @Value("${canvas.bi.query.fetch-size:1000}") int fetchSize) {
        this(primaryJdbcTemplate, dorisJdbcTemplate, queryTimeoutSeconds, fetchSize,
                (BiDatasourceHealthSnapshotMapper) null);
    }

    @Autowired
    public JdbcBiQueryExecutor(
            @Qualifier("jdbcTemplate") ObjectProvider<JdbcTemplate> primaryJdbcTemplate,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            @Value("${canvas.bi.query.timeout-sec:30}") int queryTimeoutSeconds,
            @Value("${canvas.bi.query.fetch-size:1000}") int fetchSize,
            ObjectProvider<BiDatasourceHealthSnapshotMapper> healthSnapshotMapper) {
        this(primaryJdbcTemplate, dorisJdbcTemplate, queryTimeoutSeconds, fetchSize,
                healthSnapshotMapper == null ? null : healthSnapshotMapper.getIfAvailable());
    }

    public JdbcBiQueryExecutor(
            @Qualifier("jdbcTemplate") ObjectProvider<JdbcTemplate> primaryJdbcTemplate,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            @Value("${canvas.bi.query.timeout-sec:30}") int queryTimeoutSeconds,
            @Value("${canvas.bi.query.fetch-size:1000}") int fetchSize,
            BiDatasourceHealthSnapshotMapper healthSnapshotMapper) {
        this.primaryJdbcTemplate = primaryJdbcTemplate.getIfAvailable();
        this.dorisJdbcTemplate = dorisJdbcTemplate.getIfAvailable();
        this.queryTimeoutSeconds = Math.max(1, queryTimeoutSeconds);
        this.fetchSize = Math.max(1, fetchSize);
        this.healthSnapshotMapper = healthSnapshotMapper;
    }

    @Override
    public List<Map<String, Object>> execute(BiCompiledQuery query, BiDatasetSpec dataset) {
        return execute(query, dataset, null);
    }

    @Override
    public List<Map<String, Object>> execute(BiCompiledQuery query, BiDatasetSpec dataset, String sqlHash) {
        JdbcTemplate jdbcTemplate = jdbcTemplate(dataset);
        if (jdbcTemplate == null) {
            throw new IllegalStateException("BI datasource is not available for dataset: " + dataset.datasetKey());
        }
        try {
            return jdbcTemplate.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(query.sql());
            track(sqlHash, statement);
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
        } finally {
            untrack(sqlHash);
        }
    }

    @Override
    public List<String> explain(BiCompiledQuery query, BiDatasetSpec dataset) {
        JdbcTemplate jdbcTemplate = jdbcTemplate(dataset);
        if (jdbcTemplate == null) {
            throw new IllegalStateException("BI datasource is not available for dataset: " + dataset.datasetKey());
        }
        String explainSql = "EXPLAIN " + query.sql();
        return jdbcTemplate.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(explainSql);
            statement.setQueryTimeout(queryTimeoutSeconds);
            for (int i = 0; i < query.parameters().size(); i++) {
                statement.setObject(i + 1, query.parameters().get(i));
            }
            return statement;
        }, resultSet -> {
            ResultSetMetaData metadata = resultSet.getMetaData();
            int columnCount = metadata.getColumnCount();
            List<String> steps = new ArrayList<>();
            while (resultSet.next()) {
                List<String> values = new ArrayList<>();
                for (int column = 1; column <= columnCount; column++) {
                    Object value = resultSet.getObject(column);
                    if (value != null) {
                        values.add(String.valueOf(value));
                    }
                }
                if (!values.isEmpty()) {
                    steps.add(String.join(" | ", values));
                }
            }
            return steps;
        });
    }

    @Override
    public boolean cancel(String sqlHash) {
        if (sqlHash == null || sqlHash.isBlank()) {
            return false;
        }
        PreparedStatement statement = runningStatements.get(sqlHash);
        if (statement == null) {
            return false;
        }
        try {
            statement.cancel();
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to cancel BI query: " + sqlHash, e);
        }
    }

    private void track(String sqlHash, PreparedStatement statement) {
        if (sqlHash != null && !sqlHash.isBlank()) {
            runningStatements.put(sqlHash, statement);
        }
    }

    private void untrack(String sqlHash) {
        if (sqlHash != null && !sqlHash.isBlank()) {
            runningStatements.remove(sqlHash);
        }
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
        List<BiDatasourceHealth> rows = List.of(
                health("primary", "MYSQL", primaryJdbcTemplate),
                health("doris", "DORIS", dorisJdbcTemplate));
        recordHealthSnapshots(rows);
        return rows;
    }

    @Override
    public List<BiDatasourceHealthSnapshot> healthHistory(int limit) {
        int boundedLimit = Math.max(0, limit);
        if (boundedLimit == 0) {
            return List.of();
        }
        if (healthSnapshots.isEmpty()) {
            health();
        }
        List<BiDatasourceHealthSnapshot> persisted = persistedHealthHistory(boundedLimit);
        if (!persisted.isEmpty()) {
            return persisted;
        }
        return healthSnapshots.stream()
                .sorted(Comparator.comparing(BiDatasourceHealthSnapshot::checkedAt).reversed())
                .limit(boundedLimit)
                .toList();
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

    private void recordHealthSnapshots(List<BiDatasourceHealth> rows) {
        LocalDateTime checkedAt = LocalDateTime.now();
        for (int i = rows.size() - 1; i >= 0; i--) {
            BiDatasourceHealth row = rows.get(i);
            healthSnapshots.addFirst(new BiDatasourceHealthSnapshot(
                row.sourceKey(),
                row.sourceType(),
                row.available(),
                row.message(),
                checkedAt));
            persistHealthSnapshot(new BiDatasourceHealthSnapshot(
                    row.sourceKey(),
                    row.sourceType(),
                    row.available(),
                    row.message(),
                    checkedAt));
        }
        while (healthSnapshots.size() > maxHealthSnapshots) {
            healthSnapshots.pollLast();
        }
    }

    private List<BiDatasourceHealthSnapshot> persistedHealthHistory(int limit) {
        if (healthSnapshotMapper == null) {
            return List.of();
        }
        List<BiDatasourceHealthSnapshotDO> rows = healthSnapshotMapper.selectList(
                new LambdaQueryWrapper<BiDatasourceHealthSnapshotDO>()
                        .orderByDesc(BiDatasourceHealthSnapshotDO::getCheckedAt)
                        .last("LIMIT " + Math.max(1, Math.min(limit, 500))));
        return (rows == null ? List.<BiDatasourceHealthSnapshotDO>of() : rows).stream()
                .sorted(Comparator.comparing(BiDatasourceHealthSnapshotDO::getCheckedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(row -> new BiDatasourceHealthSnapshot(
                        row.getSourceKey(),
                        row.getSourceType(),
                        Boolean.TRUE.equals(row.getAvailable()),
                        row.getMessage(),
                        row.getCheckedAt()))
                .toList();
    }

    private void persistHealthSnapshot(BiDatasourceHealthSnapshot snapshot) {
        if (healthSnapshotMapper == null) {
            return;
        }
        BiDatasourceHealthSnapshotDO row = new BiDatasourceHealthSnapshotDO();
        row.setSourceKey(snapshot.sourceKey());
        row.setSourceType(snapshot.sourceType());
        row.setAvailable(snapshot.available());
        row.setMessage(snapshot.message());
        row.setCheckedAt(snapshot.checkedAt());
        try {
            healthSnapshotMapper.insert(row);
        } catch (RuntimeException ignored) {
            // Datasource health checks must stay available when history persistence is temporarily down.
        }
    }
}
