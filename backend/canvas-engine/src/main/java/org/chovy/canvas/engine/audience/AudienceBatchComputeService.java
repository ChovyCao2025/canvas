package org.chovy.canvas.engine.audience;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.audience.AudienceDataSource;
import org.chovy.canvas.domain.audience.AudienceDataSourceMapper;
import org.chovy.canvas.domain.audience.AudienceDefinition;
import org.chovy.canvas.domain.audience.AudienceDefinitionMapper;
import org.chovy.canvas.domain.audience.AudienceStat;
import org.chovy.canvas.domain.audience.AudienceStatMapper;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudienceBatchComputeService {

    private static final String COMPUTE_LOCK_PREFIX = "audience:compute:lock:";
    private static final int PAGE_SIZE = 1000;

    private final AudienceDefinitionMapper definitionMapper;
    private final AudienceStatMapper statMapper;
    private final RuleEvaluatorRouter ruleEvaluatorRouter;
    private final AudienceBitmapStore bitmapStore;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SqlWhereGenerator sqlWhereGenerator;
    private final AudienceEvaluationContextFetcher contextFetcher;
    private final AudienceDataSourceMapper dataSourceMapper;

    @Value("${canvas.integration.tagger-service-url}")
    private String taggerUrl;

    public void compute(Long audienceId) {
        String lockKey = COMPUTE_LOCK_PREFIX + audienceId;
        boolean locked = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofHours(2)));
        if (!locked) {
            log.warn("[AUDIENCE] compute skipped because lock exists audienceId={}", audienceId);
            return;
        }

        updateStat(audienceId, "COMPUTING", null, null, null);
        try {
            AudienceDefinition definition = definitionMapper.selectById(audienceId);
            if (definition == null || definition.getEnabled() == null || definition.getEnabled() == 0) {
                throw new IllegalArgumentException("Audience not found or disabled: " + audienceId);
            }

            RoaringBitmap bitmap = switch (definition.getDataSourceType()) {
                case "JDBC" -> computeViaJdbc(definition);
                case "TAGGER_API" -> computeViaTaggerApi(definition);
                default -> throw new IllegalStateException("Unsupported data source: " + definition.getDataSourceType());
            };

            bitmapStore.save(audienceId, bitmap);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (DataOutputStream dos = new DataOutputStream(bos)) {
                bitmap.serialize(dos);
                dos.flush();
            }
            updateStat(audienceId, "READY", (long) bitmap.getCardinality(), bos.size() / 1024, null);
        } catch (Exception e) {
            log.error("[AUDIENCE] compute failed audienceId={}: {}", audienceId, e.getMessage(), e);
            updateStat(audienceId, "FAILED", null, null, trimError(e.getMessage()));
        } finally {
            redis.delete(lockKey);
        }
    }

    public AudienceDefinition create(AudienceDefinition definition) {
        validateWritableDataSource(definition);
        definitionMapper.insert(definition);
        compute(definition.getId());
        return definition;
    }

    public void update(AudienceDefinition definition) {
        validateWritableDataSource(definition);
        definitionMapper.updateById(definition);
        compute(definition.getId());
    }

    public void delete(Long audienceId) {
        definitionMapper.deleteById(audienceId);
        statMapper.deleteById(audienceId);
        bitmapStore.delete(audienceId);
    }

    private RoaringBitmap computeViaJdbc(AudienceDefinition definition) throws Exception {
        if (definition.getDataSourceId() == null) {
            throw new IllegalArgumentException("dataSourceId is required for JDBC");
        }
        AudienceDataSource audienceDataSource = dataSourceMapper.selectById(definition.getDataSourceId());
        if (audienceDataSource == null) {
            throw new IllegalArgumentException("Audience data source not found: " + definition.getDataSourceId());
        }
        JdbcConfig jdbcConfig = parseJdbcConfig(definition, audienceDataSource);
        DataSource jdbcDataSource = DataSourceBuilder.create()
                .driverClassName(jdbcConfig.driverClassName())
                .url(jdbcConfig.url())
                .username(jdbcConfig.username())
                .password(jdbcConfig.password())
                .build();
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(jdbcDataSource);
        SqlWhereGenerator.SqlWhere where = sqlWhereGenerator.generate(definition.getRuleJson());
        String sql = "SELECT " + jdbcConfig.userIdColumn() + " FROM " + jdbcConfig.baseTable() +
                " WHERE " + where.sql();
        if (jdbcConfig.maxRows() != null) {
            sql += " LIMIT " + jdbcConfig.maxRows();
        }
        List<String> userIds = jdbcTemplate.query(sql, where.params(), (rs, rowNum) -> rs.getString(1));
        RoaringBitmap bitmap = new RoaringBitmap();
        for (String userId : userIds) {
            if (userId != null && !userId.isBlank()) {
                bitmap.add(AudienceBitmapStore.toUid(userId));
            }
        }
        return bitmap;
    }

    private RoaringBitmap computeViaTaggerApi(AudienceDefinition definition) throws Exception {
        Map<String, Object> config = objectMapper.readValue(
                definition.getDataSourceConfig() == null || definition.getDataSourceConfig().isBlank()
                        ? "{}"
                        : definition.getDataSourceConfig(),
                new TypeReference<>() {}
        );
        String seedTagCode = String.valueOf(config.getOrDefault("seedTagCode", ""));
        if (seedTagCode.isBlank()) {
            throw new IllegalArgumentException("seedTagCode is required for TAGGER_API");
        }

        WebClient client = WebClient.builder().baseUrl(taggerUrl).build();
        RoaringBitmap bitmap = new RoaringBitmap();
        int page = 1;
        while (true) {
            int currentPage = page;
            Map<String, Object> response = client.get()
                    .uri(uriBuilder -> uriBuilder.path("/offline/users")
                            .queryParam("tagCode", seedTagCode)
                            .queryParam("page", currentPage)
                            .queryParam("size", PAGE_SIZE)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            List<String> userIds = extractUserIds(response);
            if (userIds.isEmpty()) {
                break;
            }
            for (String userId : userIds) {
                Map<String, Object> context = contextFetcher.fetch(client, userId, definition.getRuleJson());
                if (ruleEvaluatorRouter.evaluate(definition.getEngineType(), definition.getRuleJson(), context)) {
                    bitmap.add(AudienceBitmapStore.toUid(userId));
                }
            }
            if (userIds.size() < PAGE_SIZE) {
                break;
            }
            page++;
        }
        return bitmap;
    }

    private List<String> extractUserIds(Map<String, Object> response) {
        Object raw = response == null ? null : response.get("userIds");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> userIds = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item != null) {
                userIds.add(String.valueOf(item));
            }
        }
        return userIds;
    }

    void validateWritableDataSource(AudienceDefinition definition) {
        if (definition == null || !"JDBC".equals(definition.getDataSourceType())) {
            return;
        }
        if (definition.getDataSourceId() == null) {
            throw new IllegalArgumentException("dataSourceId is required for JDBC");
        }
        AudienceDataSource dataSource = dataSourceMapper.selectById(definition.getDataSourceId());
        if (dataSource == null) {
            throw new IllegalArgumentException("Audience data source not found: " + definition.getDataSourceId());
        }
        if (dataSource.getEnabled() != null && dataSource.getEnabled() == 0) {
            throw new IllegalArgumentException("Audience data source is disabled: " + definition.getDataSourceId());
        }
    }

    JdbcConfig parseJdbcConfig(AudienceDefinition definition, AudienceDataSource dataSource) throws Exception {
        Map<String, Object> config = objectMapper.readValue(
                definition.getDataSourceConfig() == null || definition.getDataSourceConfig().isBlank()
                        ? "{}"
                        : definition.getDataSourceConfig(),
                new TypeReference<>() {}
        );
        String baseTable = stringValue(config, "baseTable");
        String url = requiredDataSourceValue(dataSource.getUrl(), "url");
        String username = requiredDataSourceValue(dataSource.getUsername(), "username");
        String password = requiredDataSourceValue(dataSource.getPassword(), "password");
        String userIdColumn = stringValue(config, "userIdColumn", "user_id");
        String driverClassName = optionalDataSourceValue(dataSource.getDriverClassName(), "com.mysql.cj.jdbc.Driver");
        Integer maxRows = config.get("maxRows") instanceof Number number ? number.intValue() : null;
        if (!baseTable.matches("[A-Za-z_][A-Za-z0-9_]*") || !userIdColumn.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Illegal table or column name in JDBC config");
        }
        return new JdbcConfig(baseTable, url, username, password, userIdColumn, driverClassName, maxRows);
    }

    private String stringValue(Map<String, Object> config, String key) {
        String value = stringValue(config, key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing JDBC config field: " + key);
        }
        return value;
    }

    private String stringValue(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private String requiredDataSourceValue(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing JDBC config field: " + key);
        }
        return value;
    }

    private String optionalDataSourceValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private void updateStat(Long audienceId, String status, Long size, Integer sizeKb, String errorMsg) {
        AudienceStat stat = statMapper.selectById(audienceId);
        boolean exists = stat != null;
        if (!exists) {
            stat = new AudienceStat();
            stat.setAudienceId(audienceId);
        }
        stat.setStatus(status);
        stat.setComputedAt(LocalDateTime.now());
        if (size != null) {
            stat.setEstimatedSize(size);
        }
        if (sizeKb != null) {
            stat.setBitmapSizeKb(sizeKb);
        }
        stat.setErrorMsg(errorMsg);
        if (exists) {
            statMapper.updateById(stat);
        } else {
            statMapper.insert(stat);
        }
    }

    private String trimError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    public List<AudienceDefinition> listReadyDefinitions() {
        List<AudienceStat> readyStats = statMapper.selectList(new LambdaQueryWrapper<AudienceStat>()
                .eq(AudienceStat::getStatus, "READY"));
        if (readyStats.isEmpty()) {
            return List.of();
        }
        List<Long> audienceIds = readyStats.stream().map(AudienceStat::getAudienceId).toList();
        return definitionMapper.selectList(new LambdaQueryWrapper<AudienceDefinition>()
                .in(AudienceDefinition::getId, audienceIds)
                .eq(AudienceDefinition::getEnabled, 1)
                .orderByAsc(AudienceDefinition::getId));
    }

    record JdbcConfig(
            String baseTable,
            String url,
            String username,
            String password,
            String userIdColumn,
            String driverClassName,
            Integer maxRows
    ) {
    }
}
