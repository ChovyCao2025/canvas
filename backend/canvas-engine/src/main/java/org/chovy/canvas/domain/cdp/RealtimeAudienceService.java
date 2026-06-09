package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.CdpAudienceSnapshotDO;
import org.chovy.canvas.dal.dataobject.CdpRealtimeAudienceEventLogDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.CdpAudienceSnapshotMapper;
import org.chovy.canvas.dal.mapper.CdpRealtimeAudienceEventLogMapper;
import org.chovy.canvas.engine.audience.AudienceBitmapStore;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
/**
 * RealtimeAudienceService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class RealtimeAudienceService {
    private static final String BITMAP_KEY_PREFIX = "audience:bitmap:";

    private final EventLogRepository eventLogs;
    private final AudienceRuleRepository audienceRules;
    private final SnapshotRepository snapshots;
    private final AudienceBitmapStore bitmapStore;
    private final long safeSetOperationLimit;

    @Autowired
    /**
     * 初始化 RealtimeAudienceService 实例。
     *
     * @param eventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param audienceDefinitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param snapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param bitmapStore bitmap store 参数，用于 RealtimeAudienceService 流程中的校验、计算或对象转换。
     * @param ruleEvaluator rule evaluator 参数，用于 RealtimeAudienceService 流程中的校验、计算或对象转换。
     * @param safeSetOperationLimit safe set operation limit 参数，用于 RealtimeAudienceService 流程中的校验、计算或对象转换。
     */
    public RealtimeAudienceService(CdpRealtimeAudienceEventLogMapper eventLogMapper,
                                   AudienceDefinitionMapper audienceDefinitionMapper,
                                   CdpAudienceSnapshotMapper snapshotMapper,
                                   AudienceBitmapStore bitmapStore,
                                   CdpRuleEvaluator ruleEvaluator,
                                   @Value("${canvas.cdp.realtime-audience.safe-set-operation-limit:50000}")
                                   long safeSetOperationLimit) {
        this(
                new MapperEventLogRepository(eventLogMapper),
                new MapperAudienceRuleRepository(audienceDefinitionMapper, ruleEvaluator),
                new MapperSnapshotRepository(snapshotMapper),
                bitmapStore,
                safeSetOperationLimit);
    }

    /**
     * 初始化 RealtimeAudienceService 实例。
     *
     * @param eventLogs event logs 参数，用于 RealtimeAudienceService 流程中的校验、计算或对象转换。
     * @param audienceRules audience rules 参数，用于 RealtimeAudienceService 流程中的校验、计算或对象转换。
     * @param snapshots snapshots 参数，用于 RealtimeAudienceService 流程中的校验、计算或对象转换。
     * @param bitmapStore bitmap store 参数，用于 RealtimeAudienceService 流程中的校验、计算或对象转换。
     * @param safeSetOperationLimit safe set operation limit 参数，用于 RealtimeAudienceService 流程中的校验、计算或对象转换。
     */
    public RealtimeAudienceService(EventLogRepository eventLogs,
                                   AudienceRuleRepository audienceRules,
                                   SnapshotRepository snapshots,
                                   AudienceBitmapStore bitmapStore,
                                   long safeSetOperationLimit) {
        this.eventLogs = eventLogs;
        this.audienceRules = audienceRules;
        this.snapshots = snapshots;
        this.bitmapStore = bitmapStore;
        this.safeSetOperationLimit = safeSetOperationLimit;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param event event 参数，用于 processEvent 流程中的校验、计算或对象转换。
     * @param removeOnNoMatch remove on no match 参数，用于 processEvent 流程中的校验、计算或对象转换。
     * @return 返回 processEvent 流程生成的业务结果。
     */
    public EventResult processEvent(Long tenantId,
                                    Long audienceId,
                                    CdpEvent event,
                                    boolean removeOnNoMatch) throws IOException {
        Long scopedTenantId = normalizeTenantId(tenantId);
        CdpEvent normalizedEvent = event.normalized();
        boolean matched = audienceRules.matches(scopedTenantId, audienceId, normalizedEvent.properties());
        String operation = matched ? CdpRealtimeAudienceEventLogDO.ADD
                : removeOnNoMatch ? CdpRealtimeAudienceEventLogDO.REMOVE : "NOOP";
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if ("NOOP".equals(operation)) {
            return new EventResult("SKIPPED", operation, audienceId,
                    normalizedEvent.sourceEventId(), normalizedEvent.userId());
        }
        boolean reserved = eventLogs.reserve(scopedTenantId, audienceId,
                normalizedEvent.sourceEventId(), normalizedEvent.userId(), operation);
        if (!reserved) {
            return new EventResult("DUPLICATED", operation, audienceId,
                    normalizedEvent.sourceEventId(), normalizedEvent.userId());
        }

        RoaringBitmap bitmap = bitmapStore.load(audienceId);
        int uid = AudienceBitmapStore.toUid(normalizedEvent.userId());
        if (CdpRealtimeAudienceEventLogDO.ADD.equals(operation)) {
            bitmap.add(uid);
        } else {
            bitmap.remove(uid);
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        bitmapStore.save(audienceId, bitmap);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new EventResult("UPDATED", operation, audienceId,
                normalizedEvent.sourceEventId(), normalizedEvent.userId());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param leftAudienceId 业务对象 ID，用于定位具体记录。
     * @param rightAudienceId 业务对象 ID，用于定位具体记录。
     * @return 返回 overlap 流程生成的业务结果。
     */
    public OverlapResult overlap(Long leftAudienceId, Long rightAudienceId) throws IOException {
        RoaringBitmap left = bitmapStore.load(leftAudienceId);
        RoaringBitmap right = bitmapStore.load(rightAudienceId);
        RoaringBitmap intersection = left.clone();
        intersection.and(right);
        long leftCount = left.getLongCardinality();
        long rightCount = right.getLongCardinality();
        long intersectionCount = intersection.getLongCardinality();
        return new OverlapResult(
                leftCount,
                rightCount,
                intersectionCount,
                percentage(intersectionCount, leftCount),
                percentage(intersectionCount, rightCount));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param leftAudienceId 业务对象 ID，用于定位具体记录。
     * @param rightAudienceId 业务对象 ID，用于定位具体记录。
     * @return 返回 merge 汇总后的集合、分页或映射视图。
     */
    public SetOperationResult merge(Long leftAudienceId, Long rightAudienceId) throws IOException {
        return guard("MERGE", bitmapStore.merge(leftAudienceId, rightAudienceId));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param baseAudienceId 业务对象 ID，用于定位具体记录。
     * @param excludedAudienceId 业务对象 ID，用于定位具体记录。
     * @return 返回 exclude 汇总后的集合、分页或映射视图。
     */
    public SetOperationResult exclude(Long baseAudienceId, Long excludedAudienceId) throws IOException {
        return guard("EXCLUDE", bitmapStore.exclude(baseAudienceId, excludedAudienceId));
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param source source 参数，用于 createSnapshot 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    public SnapshotResult createSnapshot(Long tenantId,
                                         Long audienceId,
                                         String source,
                                         String operator) throws IOException {
        RoaringBitmap bitmap = bitmapStore.load(audienceId);
        String bitmapKey = BITMAP_KEY_PREFIX + audienceId;
        SnapshotInput input = new SnapshotInput(
                normalizeTenantId(tenantId),
                audienceId,
                bitmap.getLongCardinality(),
                bitmapKey,
                source == null || source.isBlank() ? "MANUAL" : source.trim(),
                operator);
        snapshots.insert(input);
        return new SnapshotResult(audienceId, input.estimatedSize(), bitmapKey, input.snapshotSource());
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<SnapshotRow> listSnapshots(Long tenantId, Long audienceId, int limit) {
        return snapshots.list(normalizeTenantId(tenantId), audienceId, normalizeLimit(limit));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @param result result 参数，用于 guard 流程中的校验、计算或对象转换。
     * @return 返回 guard 汇总后的集合、分页或映射视图。
     */
    private SetOperationResult guard(String operation, RoaringBitmap result) {
        long cardinality = result.getLongCardinality();
        if (cardinality > safeSetOperationLimit) {
            return new SetOperationResult("BLOCKED", "SAFE_SIZE_LIMIT_EXCEEDED", cardinality, safeSetOperationLimit);
        }
        return new SetOperationResult("READY", operation, cardinality, safeSetOperationLimit);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param numerator numerator 参数，用于 percentage 流程中的校验、计算或对象转换。
     * @param denominator denominator 参数，用于 percentage 流程中的校验、计算或对象转换。
     * @return 返回 percentage 计算得到的数量、金额或指标值。
     */
    private double percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0d;
        }
        return numerator * 100.0d / denominator;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int normalizeLimit(int limit) {
        return limit <= 0 ? 100 : Math.min(limit, 500);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * CdpEvent 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record CdpEvent(String sourceEventId,
                           String userId,
                           Instant eventTime,
                           Map<String, Object> properties) {
        /**
         * 解析、归一化或保护输入值，生成安全可用的中间结果。
         *
         * @return 返回解析、归一化或安全处理后的值。
         */
        CdpEvent normalized() {
            if (sourceEventId == null || sourceEventId.isBlank()) {
                throw new IllegalArgumentException("sourceEventId cannot be blank");
            }
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("userId cannot be blank");
            }
            return new CdpEvent(
                    sourceEventId.trim(),
                    userId.trim(),
                    eventTime,
                    properties == null ? Map.of() : properties);
        }
    }

    /**
     * EventResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record EventResult(String status,
                              String operation,
                              long audienceId,
                              String sourceEventId,
                              String userId) {
    }

    /**
     * OverlapResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record OverlapResult(long leftCount,
                                long rightCount,
                                long intersectionCount,
                                double leftPercentage,
                                double rightPercentage) {
    }

    /**
     * SetOperationResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record SetOperationResult(String status,
                                     String reason,
                                     long resultSize,
                                     long safeLimit) {
    }

    /**
     * SnapshotInput 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record SnapshotInput(Long tenantId,
                                Long audienceId,
                                long estimatedSize,
                                String bitmapKey,
                                String snapshotSource,
                                String createdBy) {
    }

    /**
     * SnapshotResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record SnapshotResult(Long audienceId,
                                 long estimatedSize,
                                 String bitmapKey,
                                 String snapshotSource) {
    }

    /**
     * SnapshotRow 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record SnapshotRow(Long id,
                              Long tenantId,
                              Long audienceId,
                              Long estimatedSize,
                              String bitmapKey,
                              String snapshotSource,
                              String createdBy,
                              LocalDateTime createdAt) {
    }

    /**
     * EventLogRepository 承载对应领域的业务规则、流程编排和结果转换。
     */
    public interface EventLogRepository {
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param audienceId 业务对象 ID，用于定位具体记录。
         * @param sourceEventId 业务对象 ID，用于定位具体记录。
         * @param userId 业务对象 ID，用于定位具体记录。
         * @param operation 待调度任务或操作名称，用于封装阻塞工作。
         * @return 返回 reserve 的布尔判断结果。
         */
        boolean reserve(Long tenantId, Long audienceId, String sourceEventId, String userId, String operation);
    }

    /**
     * AudienceRuleRepository 承载对应领域的业务规则、流程编排和结果转换。
     */
    public interface AudienceRuleRepository {
        /**
         * 根据输入和依赖数据计算业务判断结果。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param audienceId 业务对象 ID，用于定位具体记录。
         * @param eventProperties 配置对象，用于控制运行参数和策略开关。
         * @return 返回布尔判断结果。
         */
        boolean matches(Long tenantId, Long audienceId, Map<String, Object> eventProperties);
    }

    /**
     * SnapshotRepository 承载对应领域的业务规则、流程编排和结果转换。
     */
    public interface SnapshotRepository {
        /**
         * 写入或更新业务数据，并保持关联状态一致。
         *
         * @param input 输入数据，用于驱动规则判断或对象转换。
         */
        void insert(SnapshotInput input);

        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param audienceId 业务对象 ID，用于定位具体记录。
         * @param limit 分页或数量限制，避免一次处理过多数据。
         * @return 返回符合条件的数据列表或视图。
         */
        List<SnapshotRow> list(Long tenantId, Long audienceId, int limit);
    }

    /**
     * MapperEventLogRepository 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static class MapperEventLogRepository implements EventLogRepository {
        private final CdpRealtimeAudienceEventLogMapper mapper;

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
         * @return 返回 MapperEventLogRepository 流程生成的业务结果。
         */
        private MapperEventLogRepository(CdpRealtimeAudienceEventLogMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param audienceId 业务对象 ID，用于定位具体记录。
         * @param sourceEventId 业务对象 ID，用于定位具体记录。
         * @param userId 业务对象 ID，用于定位具体记录。
         * @param operation 待调度任务或操作名称，用于封装阻塞工作。
         * @return 返回 reserve 的布尔判断结果。
         */
        public boolean reserve(Long tenantId,
                               Long audienceId,
                               String sourceEventId,
                               String userId,
                               String operation) {
            CdpRealtimeAudienceEventLogDO row = new CdpRealtimeAudienceEventLogDO();
            row.setTenantId(normalizeTenantId(tenantId));
            row.setAudienceId(audienceId);
            row.setSourceEventId(sourceEventId);
            row.setUserId(userId);
            row.setOperation(operation);
            row.setProcessedAt(LocalDateTime.now());
            try {
                mapper.insert(row);
                return true;
            } catch (DuplicateKeyException duplicate) {
                return false;
            }
        }
    }

    /**
     * MapperAudienceRuleRepository 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static class MapperAudienceRuleRepository implements AudienceRuleRepository {
        private final AudienceDefinitionMapper mapper;
        private final CdpRuleEvaluator evaluator;

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
         * @param evaluator evaluator 参数，用于 MapperAudienceRuleRepository 流程中的校验、计算或对象转换。
         * @return 返回 MapperAudienceRuleRepository 流程生成的业务结果。
         */
        private MapperAudienceRuleRepository(AudienceDefinitionMapper mapper, CdpRuleEvaluator evaluator) {
            this.mapper = mapper;
            this.evaluator = evaluator;
        }

        @Override
        /**
         * 根据输入和依赖数据计算业务判断结果。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param audienceId 业务对象 ID，用于定位具体记录。
         * @param MapString map string 参数，用于 matches 流程中的校验、计算或对象转换。
         * @param eventProperties 配置对象，用于控制运行参数和策略开关。
         * @return 返回布尔判断结果。
         */
        public boolean matches(Long tenantId, Long audienceId, Map<String, Object> eventProperties) {
            AudienceDefinitionDO definition = mapper.selectOne(new LambdaQueryWrapper<AudienceDefinitionDO>()
                    .eq(AudienceDefinitionDO::getTenantId, normalizeTenantId(tenantId))
                    .eq(AudienceDefinitionDO::getId, audienceId)
                    .last("LIMIT 1"));
            if (definition == null || definition.getEnabled() == null || definition.getEnabled() == 0) {
                return false;
            }
            return evaluator.evaluate(definition.getRuleJson(), eventProperties).matched();
        }
    }

    /**
     * MapperSnapshotRepository 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static class MapperSnapshotRepository implements SnapshotRepository {
        private final CdpAudienceSnapshotMapper mapper;

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
         * @return 返回 MapperSnapshotRepository 流程生成的业务结果。
         */
        private MapperSnapshotRepository(CdpAudienceSnapshotMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        /**
         * 写入或更新业务数据，并保持关联状态一致。
         *
         * @param input 输入数据，用于驱动规则判断或对象转换。
         */
        public void insert(SnapshotInput input) {
            CdpAudienceSnapshotDO row = new CdpAudienceSnapshotDO();
            row.setTenantId(input.tenantId());
            row.setAudienceId(input.audienceId());
            row.setEstimatedSize(input.estimatedSize());
            row.setBitmapKey(input.bitmapKey());
            row.setSnapshotSource(input.snapshotSource());
            row.setCreatedBy(input.createdBy());
            row.setCreatedAt(LocalDateTime.now());
            mapper.insert(row);
        }

        @Override
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param audienceId 业务对象 ID，用于定位具体记录。
         * @param limit 分页或数量限制，避免一次处理过多数据。
         * @return 返回符合条件的数据列表或视图。
         */
        public List<SnapshotRow> list(Long tenantId, Long audienceId, int limit) {
            return mapper.selectList(new LambdaQueryWrapper<CdpAudienceSnapshotDO>()
                            .eq(CdpAudienceSnapshotDO::getTenantId, normalizeTenantId(tenantId))
                            .eq(CdpAudienceSnapshotDO::getAudienceId, audienceId)
                            .orderByDesc(CdpAudienceSnapshotDO::getCreatedAt)
                            .last("LIMIT " + Math.max(1, Math.min(limit, 500))))
                    .stream()
                    .map(row -> new SnapshotRow(
                            row.getId(),
                            row.getTenantId(),
                            row.getAudienceId(),
                            row.getEstimatedSize(),
                            row.getBitmapKey(),
                            row.getSnapshotSource(),
                            row.getCreatedBy(),
                            row.getCreatedAt()))
                    .toList();
        }
    }
}
