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

/**
 * Audience Batch Compute 人群计算组件。
 *
 * <p>负责把人群规则、数据源配置和计算任务转换为可执行查询或后台任务结果。
 * <p>该组件处于画布触发与 CDP 数据之间，需关注大数据量查询的边界和失败兜底。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudienceBatchComputeService {

    /** 人群计算分布式锁前缀，防止同一人群并发重复计算。 */
    private static final String COMPUTE_LOCK_PREFIX = "audience:compute:lock:";
    /** 批量读取或计算时的默认分页大小。 */
    private static final int PAGE_SIZE = 1000;

    /** 人群定义 Mapper。 */
    private final AudienceDefinitionMapper definitionMapper;
    /** 人群统计 Mapper。 */
    private final AudienceStatMapper statMapper;
    /** 人群计算运行记录 Mapper。 */
    private final AudienceComputeRunMapper computeRunMapper;
    /** 规则求值路由器，根据引擎类型选择具体求值器。 */
    private final RuleEvaluatorRouter ruleEvaluatorRouter;
    /** 人群 Bitmap 存储组件，用于保存计算结果。 */
    private final AudienceBitmapStore bitmapStore;
    /** 阻塞式 Redis 模板，用于锁、去重、票据或跨实例通知。 */
    private final StringRedisTemplate redis;
    /** Jackson ObjectMapper，用于 JSON 序列化和反序列化。 */
    private final ObjectMapper objectMapper;
    /** SQL 条件生成器，用于 JDBC 人群计算。 */
    private final SqlWhereGenerator sqlWhereGenerator;
    /** 人群计算上下文拉取器，用于按用户批量读取字段。 */
    private final AudienceEvaluationContextFetcher contextFetcher;
    /** JDBC 数据源配置解析器。 */
    private final JdbcConfigResolver jdbcConfigResolver;

    /** Tagger 服务地址。 */
    @Value("${canvas.integration.tagger-service-url}")
    private String taggerUrl;

    /** 执行人群计算并返回计算结果。 */
    public AudienceComputeResult compute(Long audienceId) {
        return compute(audienceId, null, null);
    }

    /** 执行人群计算并返回计算结果。 */
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

    /** 创建新记录，并执行必要的唯一性、格式和默认值处理。 */
    public AudienceDefinitionDO create(AudienceDefinitionDO definition) {
        definitionMapper.insert(definition);
        return definition;
    }

    /** 更新已有记录，仅修改允许变更的字段。 */
    public boolean update(AudienceDefinitionDO definition) {
        return definitionMapper.updateById(definition) > 0;
    }

    /** 删除人群定义、统计和已持久化的 Bitmap 结果。 */
    public void delete(Long audienceId) {
        definitionMapper.deleteById(audienceId);
        statMapper.deleteById(audienceId);
        bitmapStore.delete(audienceId);
    }

    /** 通过 JDBC 数据源执行规则 SQL 并转换为用户 Bitmap。 */
    private RoaringBitmap computeViaJdbc(AudienceDefinitionDO definition) throws Exception {
        JdbcConfig jdbcConfig = jdbcConfigResolver.resolve(definition.getDataSourceConfig());
        DataSource dataSource = DataSourceBuilder.create()
                .driverClassName(jdbcConfig.driverClassName())
                .url(jdbcConfig.url())
                .username(jdbcConfig.username())
                .password(jdbcConfig.password())
                .build();
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
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

    /** 通过 Tagger API 分页拉取种子用户并按规则二次过滤生成 Bitmap。 */
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

    /** 从 Tagger API 响应中安全提取用户 ID 列表。 */
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

    /** 更新或创建人群计算状态统计记录。 */
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

    /** 记录性能压测场景下的人群计算开始信息。 */
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

    /** 回写性能压测计算运行记录的最终状态和结果指标。 */
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

    /** 截断计算错误信息，避免统计字段过长。 */
    private String trimError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    /** 容错读取人群名称，读取失败时返回兜底名称。 */
    private String safeAudienceName(Long audienceId) {
        try {
            AudienceDefinitionDO definition = definitionMapper.selectById(audienceId);
            return displayName(definition == null ? null : definition.getName(), audienceId);
        } catch (Exception e) {
            log.warn("[AUDIENCE] failed to fetch audience name audienceId={}: {}", audienceId, e.getMessage(), e);
            return fallbackAudienceName(audienceId);
        }
    }

    /** 返回人群展示名称，空名称时使用兜底文案。 */
    private String displayName(String audienceName, Long audienceId) {
        return audienceName == null || audienceName.isBlank() ? fallbackAudienceName(audienceId) : audienceName;
    }

    /** 构造人群名称兜底文案。 */
    private String fallbackAudienceName(Long audienceId) {
        return "人群 " + audienceId;
    }

    /** 释放人群计算锁，释放失败仅记录日志不影响计算结果返回。 */
    private void releaseLock(String lockKey, Long audienceId) {
        try {
            redis.delete(lockKey);
        } catch (Exception e) {
            log.error("[AUDIENCE] failed to release compute lock audienceId={}: {}", audienceId, e.getMessage(), e);
        }
    }

    /** 查询可调度或可计算的人群定义。 */
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
