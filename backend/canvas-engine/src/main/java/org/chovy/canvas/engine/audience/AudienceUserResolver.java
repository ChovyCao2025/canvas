package org.chovy.canvas.engine.audience;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves an audience definition into concrete user IDs for scheduled batch fan-out.
 */
@Service
@RequiredArgsConstructor
public class AudienceUserResolver {

    private final AudienceDefinitionMapper definitionMapper;
    private final CdpAudienceSourceService cdpAudienceSourceService;
    private final JdbcConfigResolver jdbcConfigResolver;
    private final SqlWhereGenerator sqlWhereGenerator;

    public List<String> resolve(Long audienceId) {
        AudienceDefinitionDO definition = definitionMapper.selectById(audienceId);
        if (definition == null || definition.getEnabled() == null || definition.getEnabled() == 0) {
            throw new IllegalArgumentException("Audience not found or disabled: " + audienceId);
        }
        String sourceType = definition.getDataSourceType();
        if (cdpAudienceSourceService.supports(sourceType)) {
            return cdpAudienceSourceService.resolveUserIds(sourceType, definition.getRuleJson());
        }
        if ("JDBC".equals(sourceType)) {
            return resolveJdbc(definition);
        }
        throw new IllegalStateException("Unsupported audience source for scheduled fan-out: " + sourceType);
    }

    private List<String> resolveJdbc(AudienceDefinitionDO definition) {
        DataSource dataSource = null;
        try {
            JdbcConfig jdbcConfig = jdbcConfigResolver.resolve(definition.getDataSourceConfig());
            dataSource = DataSourceBuilder.create()
                    .driverClassName(jdbcConfig.driverClassName())
                    .url(jdbcConfig.url())
                    .username(jdbcConfig.username())
                    .password(jdbcConfig.password())
                    .build();
            NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
            jdbcTemplate.getJdbcTemplate().setFetchSize(1000);
            SqlWhereGenerator.SqlWhere where = sqlWhereGenerator.generate(definition.getRuleJson());
            String sql = "SELECT " + jdbcConfig.userIdColumn() + " FROM " + jdbcConfig.baseTable() +
                    " WHERE " + where.sql();
            var params = where.params();
            if (jdbcConfig.maxRows() != null) {
                sql += " LIMIT :__maxRows";
                params.addValue("__maxRows", jdbcConfig.maxRows());
            }
            List<String> userIds = new ArrayList<>();
            jdbcTemplate.query(sql, params, rs -> {
                String userId = rs.getString(1);
                if (userId != null && !userId.isBlank()) {
                    userIds.add(userId);
                }
            });
            return userIds;
        } catch (Exception e) {
            throw new IllegalStateException("Resolve JDBC audience users failed: " + e.getMessage(), e);
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
}
