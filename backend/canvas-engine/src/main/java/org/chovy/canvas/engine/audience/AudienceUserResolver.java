package org.chovy.canvas.engine.audience;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
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
 * Resolves an audience definition into concrete user IDs for scheduled batch fan-out.
 */
@Service
public class AudienceUserResolver {

    private static final Pattern NAMED_PARAMETER = Pattern.compile(":([A-Za-z_][A-Za-z0-9_]*)");

    private final AudienceDefinitionMapper definitionMapper;
    private final CdpAudienceSourceService cdpAudienceSourceService;
    private final JdbcConfigResolver jdbcConfigResolver;
    private final SqlWhereGenerator sqlWhereGenerator;
    private final DataSourceFactory dataSourceFactory;

    public AudienceUserResolver(
            AudienceDefinitionMapper definitionMapper,
            CdpAudienceSourceService cdpAudienceSourceService,
            JdbcConfigResolver jdbcConfigResolver,
            SqlWhereGenerator sqlWhereGenerator) {
        this(definitionMapper, cdpAudienceSourceService, jdbcConfigResolver, sqlWhereGenerator, AudienceUserResolver::buildDataSource);
    }

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

    public List<String> resolve(Long audienceId) {
        List<String> userIds = new ArrayList<>();
        resolveAndProcess(audienceId, userIds::add);
        return userIds;
    }

    public int resolveAndProcess(Long audienceId, Consumer<String> processor) {
        AudienceDefinitionDO definition = definitionMapper.selectById(audienceId);
        if (definition == null || definition.getEnabled() == null || definition.getEnabled() == 0) {
            throw new IllegalArgumentException("Audience not found or disabled: " + audienceId);
        }
        String sourceType = definition.getDataSourceType();
        if (cdpAudienceSourceService.supports(sourceType)) {
            int count = 0;
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
        } catch (Exception e) {
            throw new IllegalStateException("Resolve JDBC audience users failed for audience "
                    + definition.getId() + ": " + e.getMessage(), e);
        } finally {
            if (dataSource instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception ignored) {
                    // best effort
                }
            }
        }
    }

    private static DataSource buildDataSource(JdbcConfig jdbcConfig) {
        return DataSourceBuilder.create()
                .driverClassName(jdbcConfig.driverClassName())
                .url(jdbcConfig.url())
                .username(jdbcConfig.username())
                .password(jdbcConfig.password())
                .build();
    }

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

    private static String placeholders(Object value, List<Object> values) {
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
        return "?";
    }

    private record BoundSql(String sql, List<Object> values) {
    }

    @FunctionalInterface
    interface DataSourceFactory {
        DataSource create(JdbcConfig jdbcConfig);
    }
}
