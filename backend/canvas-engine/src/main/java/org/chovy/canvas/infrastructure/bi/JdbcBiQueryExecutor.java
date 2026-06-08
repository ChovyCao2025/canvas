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
/**
 * JdbcBiQueryExecutor 封装本模块的核心职责、输入输出结构和协作边界。
 */
public class JdbcBiQueryExecutor implements BiQueryExecutor, BiDatasourceHealthProvider {

    private final JdbcTemplate primaryJdbcTemplate;
    private final JdbcTemplate dorisJdbcTemplate;
    private final int queryTimeoutSeconds;
    private final int fetchSize;
    private final BiDatasourceHealthSnapshotMapper healthSnapshotMapper;
    private final Map<String, PreparedStatement> runningStatements = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<BiDatasourceHealthSnapshot> healthSnapshots = new ConcurrentLinkedDeque<>();
    private final int maxHealthSnapshots = 200;

    /**
     * 初始化 JdbcBiQueryExecutor 实例。
     *
     * @param primaryJdbcTemplate primary jdbc template 参数，用于 JdbcBiQueryExecutor 流程中的校验、计算或对象转换。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 JdbcBiQueryExecutor 流程中的校验、计算或对象转换。
     * @param queryTimeoutSeconds 时间参数，用于计算窗口、过期或审计时间。
     * @param fetchSize fetch size 参数，用于 JdbcBiQueryExecutor 流程中的校验、计算或对象转换。
     */
    public JdbcBiQueryExecutor(
            @Qualifier("jdbcTemplate") ObjectProvider<JdbcTemplate> primaryJdbcTemplate,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            @Value("${canvas.bi.query.timeout-sec:30}") int queryTimeoutSeconds,
            @Value("${canvas.bi.query.fetch-size:1000}") int fetchSize) {
        this(primaryJdbcTemplate, dorisJdbcTemplate, queryTimeoutSeconds, fetchSize,
                (BiDatasourceHealthSnapshotMapper) null);
    }

    @Autowired
    /**
     * 初始化 JdbcBiQueryExecutor 实例。
     *
     * @param primaryJdbcTemplate primary jdbc template 参数，用于 JdbcBiQueryExecutor 流程中的校验、计算或对象转换。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 JdbcBiQueryExecutor 流程中的校验、计算或对象转换。
     * @param queryTimeoutSeconds 时间参数，用于计算窗口、过期或审计时间。
     * @param fetchSize fetch size 参数，用于 JdbcBiQueryExecutor 流程中的校验、计算或对象转换。
     * @param healthSnapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public JdbcBiQueryExecutor(
            @Qualifier("jdbcTemplate") ObjectProvider<JdbcTemplate> primaryJdbcTemplate,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            @Value("${canvas.bi.query.timeout-sec:30}") int queryTimeoutSeconds,
            @Value("${canvas.bi.query.fetch-size:1000}") int fetchSize,
            ObjectProvider<BiDatasourceHealthSnapshotMapper> healthSnapshotMapper) {
        this(primaryJdbcTemplate, dorisJdbcTemplate, queryTimeoutSeconds, fetchSize,
                healthSnapshotMapper == null ? null : healthSnapshotMapper.getIfAvailable());
    }

    /**
     * 初始化 JdbcBiQueryExecutor 实例。
     *
     * @param primaryJdbcTemplate primary jdbc template 参数，用于 JdbcBiQueryExecutor 流程中的校验、计算或对象转换。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 JdbcBiQueryExecutor 流程中的校验、计算或对象转换。
     * @param queryTimeoutSeconds 时间参数，用于计算窗口、过期或审计时间。
     * @param fetchSize fetch size 参数，用于 JdbcBiQueryExecutor 流程中的校验、计算或对象转换。
     * @param healthSnapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param query query 参数，用于 execute 流程中的校验、计算或对象转换。
     * @param dataset dataset 参数，用于 execute 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public List<Map<String, Object>> execute(BiCompiledQuery query, BiDatasetSpec dataset) {
        return execute(query, dataset, null);
    }

    @Override
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param query query 参数，用于 execute 流程中的校验、计算或对象转换。
     * @param dataset dataset 参数，用于 execute 流程中的校验、计算或对象转换。
     * @param sqlHash sql hash 参数，用于 execute 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public List<Map<String, Object>> execute(BiCompiledQuery query, BiDatasetSpec dataset, String sqlHash) {
        JdbcTemplate jdbcTemplate = jdbcTemplate(dataset);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (jdbcTemplate == null) {
            throw new IllegalStateException("BI datasource is not available for dataset: " + dataset.datasetKey());
        }
        try {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            return jdbcTemplate.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(query.sql());
            track(sqlHash, statement);
            statement.setQueryTimeout(queryTimeoutSeconds);
            statement.setFetchSize(fetchSize);
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param query query 参数，用于 explain 流程中的校验、计算或对象转换。
     * @param dataset dataset 参数，用于 explain 流程中的校验、计算或对象转换。
     * @return 返回 explain 汇总后的集合、分页或映射视图。
     */
    public List<String> explain(BiCompiledQuery query, BiDatasetSpec dataset) {
        JdbcTemplate jdbcTemplate = jdbcTemplate(dataset);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (jdbcTemplate == null) {
            throw new IllegalStateException("BI datasource is not available for dataset: " + dataset.datasetKey());
        }
        String explainSql = "EXPLAIN " + query.sql();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return jdbcTemplate.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(explainSql);
            statement.setQueryTimeout(queryTimeoutSeconds);
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param sqlHash sql hash 参数，用于 cancel 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    public boolean cancel(String sqlHash) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (sqlHash == null || sqlHash.isBlank()) {
            return false;
        }
        PreparedStatement statement = runningStatements.get(sqlHash);
        if (statement == null) {
            return false;
        }
        try {
            statement.cancel();
            // 汇总前面计算出的状态和明细，返回给调用方。
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to cancel BI query: " + sqlHash, e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param sqlHash sql hash 参数，用于 track 流程中的校验、计算或对象转换。
     * @param statement statement 参数，用于 track 流程中的校验、计算或对象转换。
     */
    private void track(String sqlHash, PreparedStatement statement) {
        if (sqlHash != null && !sqlHash.isBlank()) {
            runningStatements.put(sqlHash, statement);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param sqlHash sql hash 参数，用于 untrack 流程中的校验、计算或对象转换。
     */
    private void untrack(String sqlHash) {
        if (sqlHash != null && !sqlHash.isBlank()) {
            runningStatements.remove(sqlHash);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dataset dataset 参数，用于 jdbcTemplate 流程中的校验、计算或对象转换。
     * @return 返回 jdbcTemplate 流程生成的业务结果。
     */
    private JdbcTemplate jdbcTemplate(BiDatasetSpec dataset) {
        String tableExpression = dataset.tableExpression().toLowerCase(Locale.ROOT);
        if (tableExpression.startsWith("canvas_dws.") || tableExpression.startsWith("canvas_ods.")) {
            return dorisJdbcTemplate;
        }
        return primaryJdbcTemplate;
    }

    @Override
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 health 汇总后的集合、分页或映射视图。
     */
    public List<BiDatasourceHealth> health() {
        List<BiDatasourceHealth> rows = List.of(
                health("primary", "MYSQL", primaryJdbcTemplate),
                health("doris", "DORIS", dorisJdbcTemplate));
        recordHealthSnapshots(rows);
        return rows;
    }

    @Override
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 health history 汇总后的集合、分页或映射视图。
     */
    public List<BiDatasourceHealthSnapshot> healthHistory(int limit) {
        int boundedLimit = Math.max(0, limit);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return healthSnapshots.stream()
                .sorted(Comparator.comparing(BiDatasourceHealthSnapshot::checkedAt).reversed())
                .limit(boundedLimit)
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param sourceKey 业务键，用于在同一租户下定位资源。
     * @param sourceType 类型标识，用于选择对应处理分支。
     * @param jdbcTemplate jdbc template 参数，用于 health 流程中的校验、计算或对象转换。
     * @return 返回 health 流程生成的业务结果。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param rows rows 参数，用于 recordHealthSnapshots 流程中的校验、计算或对象转换。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 persisted health history 汇总后的集合、分页或映射视图。
     */
    private List<BiDatasourceHealthSnapshot> persistedHealthHistory(int limit) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (healthSnapshotMapper == null) {
            return List.of();
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<BiDatasourceHealthSnapshotDO> rows = healthSnapshotMapper.selectList(
                new LambdaQueryWrapper<BiDatasourceHealthSnapshotDO>()
                        .orderByDesc(BiDatasourceHealthSnapshotDO::getCheckedAt)
                        .last("LIMIT " + Math.max(1, Math.min(limit, 500))));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param snapshot snapshot 参数，用于 persistHealthSnapshot 流程中的校验、计算或对象转换。
     */
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
