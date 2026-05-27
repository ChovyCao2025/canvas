package org.chovy.canvas.engine.audience;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.dataobject.AudienceComputeRunDO;
import org.chovy.canvas.dal.mapper.AudienceComputeRunMapper;
import org.chovy.canvas.dal.dataobject.AudienceStatDO;
import org.chovy.canvas.dal.mapper.AudienceStatMapper;
import org.chovy.canvas.engine.rule.AudienceDefinitionRuleValidator;
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
    private final AudienceComputeRunMapper computeRunMapper;
    private final RuleEvaluatorRouter ruleEvaluatorRouter;
    private final AudienceBitmapStore bitmapStore;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SqlWhereGenerator sqlWhereGenerator;
    private final AudienceEvaluationContextFetcher contextFetcher;
    private final JdbcConfigResolver jdbcConfigResolver;
    private final AudienceDefinitionRuleValidator audienceRuleValidator;

    @Value("${canvas.integration.tagger-service-url}")
    private String taggerUrl;

    public AudienceComputeResult compute(Long audienceId) {
        return compute(audienceId, null, null);
    }

    public AudienceComputeResult compute(Long audienceId, String perfRunId, String perfInputId) {
        AudienceComputeRunDO run = startRun(audienceId, perfRunId, perfInputId);
        String lockKey = COMPUTE_LOCK_PREFIX + audienceId;
        boolean locked = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofHours(2)));
        if (!locked) {
            log.warn("[AUDIENCE] compute skipped because lock exists audienceId={}", audienceId);
            finishRun(run, "SKIPPED_LOCK", null, null, "已有计算任务正在运行");
            if (run != null) {
                return AudienceComputeResult.failed(audienceId, safeAudienceName(audienceId), "已有计算任务正在运行");
            }
            return AudienceComputeResult.inProgress(audienceId, safeAudienceName(audienceId), "已有计算任务正在运行");
        }

        String audienceName = fallbackAudienceName(audienceId);
        try {
            updateStat(audienceId, "COMPUTING", null, null, null);
            AudienceDefinitionDO definition = definitionMapper.selectById(audienceId);
            if (definition == null || definition.getEnabled() == null || definition.getEnabled() == 0) {
                throw new IllegalArgumentException("Audience not found or disabled: " + audienceId);
            }
            audienceName = displayName(definition.getName(), audienceId);

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
            long estimatedSize = bitmap.getCardinality();
            int bitmapSizeKb = bos.size() / 1024;
            updateStat(audienceId, "READY", estimatedSize, bitmapSizeKb, null);
            finishRun(run, "READY", estimatedSize, bitmapSizeKb, null);
            return AudienceComputeResult.ready(audienceId, audienceName, estimatedSize, bitmapSizeKb);
        } catch (Exception e) {
            String error = trimError(e.getMessage());
            log.error("[AUDIENCE] compute failed audienceId={}: {}", audienceId, e.getMessage(), e);
            try {
                updateStat(audienceId, "FAILED", null, null, error);
            } catch (Exception statException) {
                log.error("[AUDIENCE] failed to update compute failure stat audienceId={}: {}",
                        audienceId, statException.getMessage(), statException);
            }
            finishRun(run, "FAILED", null, null, error);
            return AudienceComputeResult.failed(audienceId, audienceName, error);
        } finally {
            releaseLock(lockKey, audienceId);
        }
    }

    public AudienceDefinitionDO create(AudienceDefinitionDO definition) {
        audienceRuleValidator.validateForSave(definition);
        definitionMapper.insert(definition);
        return definition;
    }

    public boolean update(AudienceDefinitionDO definition) {
        audienceRuleValidator.validateForSave(definition);
        return definitionMapper.updateById(definition) > 0;
    }

    public void delete(Long audienceId) {
        definitionMapper.deleteById(audienceId);
        statMapper.deleteById(audienceId);
        bitmapStore.delete(audienceId);
    }

    private RoaringBitmap computeViaJdbc(AudienceDefinitionDO definition) throws Exception {
        JdbcConfig jdbcConfig = jdbcConfigResolver.resolve(definition.getDataSourceConfig());
        DataSource dataSource = DataSourceBuilder.create()
                .driverClassName(jdbcConfig.driverClassName())
                .url(jdbcConfig.url())
                .username(jdbcConfig.username())
                .password(jdbcConfig.password())
                .build();
        try {
            NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
            jdbcTemplate.getJdbcTemplate().setFetchSize(PAGE_SIZE);
            SqlWhereGenerator.SqlWhere where = sqlWhereGenerator.generate(definition.getRuleJson());
            String sql = "SELECT " + jdbcConfig.userIdColumn() + " FROM " + jdbcConfig.baseTable() +
                    " WHERE " + where.sql();
            org.springframework.jdbc.core.namedparam.MapSqlParameterSource params = where.params();
            if (jdbcConfig.maxRows() != null) {
                sql += " LIMIT :__maxRows";
                params.addValue("__maxRows", jdbcConfig.maxRows());
            }
            RoaringBitmap bitmap = new RoaringBitmap();
            jdbcTemplate.query(sql, params, rs -> {
                String userId = rs.getString(1);
                if (userId != null && !userId.isBlank()) {
                    bitmap.add(AudienceBitmapStore.toUid(userId));
                }
            });
            return bitmap;
        } finally {
            closeDataSource(dataSource);
        }
    }

    private void closeDataSource(DataSource dataSource) {
        if (dataSource instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                log.warn("[AUDIENCE] failed to close JDBC DataSource: {}", e.getMessage(), e);
            }
        }
    }

    private RoaringBitmap computeViaTaggerApi(AudienceDefinitionDO definition) throws Exception {
        Map<String, Object> config = objectMapper.readValue(
                definition.getDataSourceConfig() == null || definition.getDataSourceConfig().isBlank()
                        ? "{}"
                        : definition.getDataSourceConfig(),
                new com.fasterxml.jackson.core.type.TypeReference<>() {}
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

    private void updateStat(Long audienceId, String status, Long size, Integer sizeKb, String errorMsg) {
        AudienceStatDO stat = statMapper.selectById(audienceId);
        boolean exists = stat != null;
        if (!exists) {
            stat = new AudienceStatDO();
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

    private AudienceComputeRunDO startRun(Long audienceId, String perfRunId, String perfInputId) {
        if (perfRunId == null || perfRunId.isBlank()) {
            return null;
        }
        AudienceComputeRunDO run = new AudienceComputeRunDO();
        run.setAudienceId(audienceId);
        run.setPerfRunId(perfRunId);
        run.setPerfInputId(perfInputId);
        run.setStatus("COMPUTING");
        run.setCreatedAt(LocalDateTime.now());
        run.setUpdatedAt(run.getCreatedAt());
        computeRunMapper.insert(run);
        return run;
    }

    private void finishRun(AudienceComputeRunDO run, String status, Long size, Integer sizeKb, String errorMsg) {
        if (run == null) {
            return;
        }
        run.setStatus(status);
        run.setEstimatedSize(size);
        run.setBitmapSizeKb(sizeKb);
        run.setErrorMsg(errorMsg);
        run.setUpdatedAt(LocalDateTime.now());
        computeRunMapper.updateById(run);
    }

    private String trimError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private String safeAudienceName(Long audienceId) {
        try {
            AudienceDefinitionDO definition = definitionMapper.selectById(audienceId);
            return displayName(definition == null ? null : definition.getName(), audienceId);
        } catch (Exception e) {
            log.warn("[AUDIENCE] failed to fetch audience name audienceId={}: {}", audienceId, e.getMessage(), e);
            return fallbackAudienceName(audienceId);
        }
    }

    private String displayName(String audienceName, Long audienceId) {
        return audienceName == null || audienceName.isBlank() ? fallbackAudienceName(audienceId) : audienceName;
    }

    private String fallbackAudienceName(Long audienceId) {
        return "人群 " + audienceId;
    }

    private void releaseLock(String lockKey, Long audienceId) {
        try {
            redis.delete(lockKey);
        } catch (Exception e) {
            log.error("[AUDIENCE] failed to release compute lock audienceId={}: {}", audienceId, e.getMessage(), e);
        }
    }

    public List<AudienceDefinitionDO> listReadyDefinitions() {
        List<AudienceStatDO> readyStats = statMapper.selectList(new LambdaQueryWrapper<AudienceStatDO>()
                .eq(AudienceStatDO::getStatus, "READY"));
        if (readyStats.isEmpty()) {
            return List.of();
        }
        List<Long> audienceIds = readyStats.stream().map(AudienceStatDO::getAudienceId).toList();
        return definitionMapper.selectList(new LambdaQueryWrapper<AudienceDefinitionDO>()
                .in(AudienceDefinitionDO::getId, audienceIds)
                .eq(AudienceDefinitionDO::getEnabled, 1)
                .orderByAsc(AudienceDefinitionDO::getId));
    }
}
