package org.chovy.canvas.engine.audience;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将人群定义解析为具体用户 ID，供定时批量 fan-out 使用。
 */
@Service
public class AudienceUserResolver {

    private static final Pattern NAMED_PARAMETER = Pattern.compile(":([A-Za-z_][A-Za-z0-9_]*)");

    private final AudienceDefinitionMapper definitionMapper;
    private final CdpAudienceSourceService cdpAudienceSourceService;
    private final JdbcConfigResolver jdbcConfigResolver;
    private final SqlWhereGenerator sqlWhereGenerator;
    private final DataSourceFactory dataSourceFactory;

    /**
     * 创建 AudienceUserResolver 实例并注入 engine.audience 场景依赖。
     * @param definitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param cdpAudienceSourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param jdbcConfigResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param sqlWhereGenerator sql where generator 参数，用于 AudienceUserResolver 流程中的校验、计算或对象转换。
     */
    @Autowired
    public AudienceUserResolver(
            AudienceDefinitionMapper definitionMapper,
            CdpAudienceSourceService cdpAudienceSourceService,
            JdbcConfigResolver jdbcConfigResolver,
            SqlWhereGenerator sqlWhereGenerator) {
        this(definitionMapper, cdpAudienceSourceService, jdbcConfigResolver, sqlWhereGenerator, AudienceUserResolver::buildDataSource);
    }

    /**
     * 使用可替换数据源工厂创建人群用户解析器。
     *
     * @param definitionMapper 人群定义 Mapper
     * @param cdpAudienceSourceService CDP 人群来源服务
     * @param jdbcConfigResolver JDBC 配置解析器
     * @param sqlWhereGenerator 规则 SQL 生成器
     * @param dataSourceFactory 数据源工厂，测试可替换
     */
    AudienceUserResolver(
            AudienceDefinitionMapper definitionMapper,
            CdpAudienceSourceService cdpAudienceSourceService,
            JdbcConfigResolver jdbcConfigResolver,
            SqlWhereGenerator sqlWhereGenerator,
            DataSourceFactory dataSourceFactory) {
        this.definitionMapper = definitionMapper;
        this.cdpAudienceSourceService = cdpAudienceSourceService;
        this.jdbcConfigResolver = jdbcConfigResolver;
        this.sqlWhereGenerator = sqlWhereGenerator;
        this.dataSourceFactory = dataSourceFactory;
    }

    /**
     * 将人群定义解析为完整用户 ID 列表。
     *
     * <p>方法内部复用 {@link #resolveAndProcess(Long, Consumer)}，会读取启用的人群定义并按来源类型解析用户；
     * 对 JDBC 人群会执行只读查询。由于会把所有用户 ID 收集到内存，适合小批量或快照调用。</p>
     *
     * @param audienceId 人群定义 ID
     * @return 解析出的非空用户 ID 列表
     */
    public List<String> resolve(Long audienceId) {
        List<String> userIds = new ArrayList<>();
        resolveAndProcess(audienceId, userIds::add);
        return userIds;
    }

    /**
     * 流式解析人群用户并逐个交给处理器。
     *
     * <p>方法会校验人群存在且启用；CDP 来源委托 CDP 人群源服务，JDBC 来源解析数据源配置和规则 SQL 后执行查询，
     * 每读到一个非空 userId 就调用 processor。处理器中的副作用由调用方控制，方法返回成功处理的用户数量。</p>
     *
     * @param audienceId 人群定义 ID
     * @param processor 用户 ID 处理回调
     * @return 已传递给 processor 的用户 ID 数量
     */
    public int resolveAndProcess(Long audienceId, Consumer<String> processor) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        AudienceDefinitionDO definition = definitionMapper.selectById(audienceId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (definition == null || definition.getEnabled() == null || definition.getEnabled() == 0) {
            throw new IllegalArgumentException("Audience not found or disabled: " + audienceId);
        }
        String sourceType = definition.getDataSourceType();
        if (cdpAudienceSourceService.supports(sourceType)) {
            int count = 0;
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (String userId : cdpAudienceSourceService.resolveUserIds(sourceType, definition.getRuleJson())) {
                processor.accept(userId);
                count++;
            }
            return count;
        }
        if ("JDBC".equals(sourceType)) {
            return resolveJdbc(definition, processor);
        }
        throw new IllegalStateException("Unsupported audience source for scheduled fan-out: " + sourceType);
    }

    /**
     * 通过 JDBC 人群规则查询并流式输出用户 ID。
     *
     * @param definition 人群定义
     * @param processor 用户 ID 处理回调
     * @return 已处理用户数量
     */
    private int resolveJdbc(AudienceDefinitionDO definition, Consumer<String> processor) {
        DataSource dataSource = null;
        try {
            JdbcConfig jdbcConfig = jdbcConfigResolver.resolve(definition.getDataSourceConfig());
            dataSource = dataSourceFactory.create(jdbcConfig);
            SqlWhereGenerator.SqlWhere where = sqlWhereGenerator.generate(definition.getRuleJson());
            String sql = "SELECT " + jdbcConfig.userIdColumn() + " FROM " + jdbcConfig.baseTable() +
                    " WHERE " + where.sql();
            var params = where.params();
            if (jdbcConfig.maxRows() != null) {
                sql += " LIMIT :__maxRows";
                params.addValue("__maxRows", jdbcConfig.maxRows());
            }
            BoundSql boundSql = bindNamedParameters(sql, params);
            int count = 0;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(
                         boundSql.sql(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                ps.setFetchSize(Integer.MIN_VALUE);
                for (int i = 0; i < boundSql.values().size(); i++) {
                    ps.setObject(i + 1, boundSql.values().get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String userId = rs.getString(1);
                        if (userId != null && !userId.isBlank()) {
                            processor.accept(userId);
                            count++;
                        }
                    }
                }
            }
            return count;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("Resolve JDBC audience users failed for audience "
                    + definition.getId() + ": " + e.getMessage(), e);
        } finally {
            if (dataSource instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
                } catch (Exception ignored) {
                    // 外部数据源关闭失败不影响已完成的人群解析结果。
                }
            }
        }
    }

    /**
     * 根据 JDBC 配置构造数据源。
     *
     * @param jdbcConfig JDBC 连接配置
     * @return 可用于只读查询的数据源
     */
    private static DataSource buildDataSource(JdbcConfig jdbcConfig) {
        return DataSourceBuilder.create()
                .driverClassName(jdbcConfig.driverClassName())
                .url(jdbcConfig.url())
                .username(jdbcConfig.username())
                .password(jdbcConfig.password())
                .build();
    }

    /**
     * 将命名参数 SQL 转换为 PreparedStatement SQL。
     *
     * @param sql 含命名参数的 SQL
     * @param params 命名参数集合
     * @return 使用问号占位符和顺序参数的 SQL
     */
    private static BoundSql bindNamedParameters(String sql, MapSqlParameterSource params) {
        Matcher matcher = NAMED_PARAMETER.matcher(sql);
        StringBuilder preparedSql = new StringBuilder();
        List<Object> values = new ArrayList<>();
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!params.hasValue(name)) {
                throw new IllegalArgumentException("Missing SQL parameter: " + name);
            }
            Object value = params.getValue(name);
            matcher.appendReplacement(preparedSql, Matcher.quoteReplacement(placeholders(value, values)));
        }
        matcher.appendTail(preparedSql);
        return new BoundSql(preparedSql.toString(), values);
    }

    /**
     * 为单值、集合或数组参数生成 PreparedStatement 占位符。
     *
     * @param value 参数值
     * @param values 顺序参数收集列表
     * @return 一个或多个问号占位符
     */
    private static String placeholders(Object value, List<Object> values) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                throw new IllegalArgumentException("Empty collection SQL parameter is not supported");
            }
            values.addAll(collection);
            return "?,".repeat(collection.size() - 1) + "?";
        }
        if (value instanceof Object[] array) {
            if (array.length == 0) {
                throw new IllegalArgumentException("Empty array SQL parameter is not supported");
            }
            values.addAll(List.of(array));
            return "?,".repeat(array.length - 1) + "?";
        }
        values.add(value);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "?";
    }

    /**
     * 已绑定命名参数后的 SQL。
     *
     * @param sql PreparedStatement SQL
     * @param values 顺序参数值
     */
    private record BoundSql(String sql, List<Object> values) {
    }

    /**
     * JDBC 数据源创建工厂。
     */
    @FunctionalInterface
    interface DataSourceFactory {
        /**
         * 根据 JDBC 配置创建数据源。
         *
         * @param jdbcConfig JDBC 连接配置
         * @return 数据源实例
         */
        DataSource create(JdbcConfig jdbcConfig);
    }
}
