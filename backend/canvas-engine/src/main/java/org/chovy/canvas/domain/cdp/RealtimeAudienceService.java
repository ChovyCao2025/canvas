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
public class RealtimeAudienceService {
    private static final String BITMAP_KEY_PREFIX = "audience:bitmap:";

    private final EventLogRepository eventLogs;
    private final AudienceRuleRepository audienceRules;
    private final SnapshotRepository snapshots;
    private final AudienceBitmapStore bitmapStore;
    private final long safeSetOperationLimit;

    @Autowired
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

    public EventResult processEvent(Long tenantId,
                                    Long audienceId,
                                    CdpEvent event,
                                    boolean removeOnNoMatch) throws IOException {
        Long scopedTenantId = normalizeTenantId(tenantId);
        CdpEvent normalizedEvent = event.normalized();
        boolean matched = audienceRules.matches(scopedTenantId, audienceId, normalizedEvent.properties());
        String operation = matched ? CdpRealtimeAudienceEventLogDO.ADD
                : removeOnNoMatch ? CdpRealtimeAudienceEventLogDO.REMOVE : "NOOP";
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
        bitmapStore.save(audienceId, bitmap);
        return new EventResult("UPDATED", operation, audienceId,
                normalizedEvent.sourceEventId(), normalizedEvent.userId());
    }

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

    public SetOperationResult merge(Long leftAudienceId, Long rightAudienceId) throws IOException {
        return guard("MERGE", bitmapStore.merge(leftAudienceId, rightAudienceId));
    }

    public SetOperationResult exclude(Long baseAudienceId, Long excludedAudienceId) throws IOException {
        return guard("EXCLUDE", bitmapStore.exclude(baseAudienceId, excludedAudienceId));
    }

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

    public List<SnapshotRow> listSnapshots(Long tenantId, Long audienceId, int limit) {
        return snapshots.list(normalizeTenantId(tenantId), audienceId, normalizeLimit(limit));
    }

    private SetOperationResult guard(String operation, RoaringBitmap result) {
        long cardinality = result.getLongCardinality();
        if (cardinality > safeSetOperationLimit) {
            return new SetOperationResult("BLOCKED", "SAFE_SIZE_LIMIT_EXCEEDED", cardinality, safeSetOperationLimit);
        }
        return new SetOperationResult("READY", operation, cardinality, safeSetOperationLimit);
    }

    private double percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0d;
        }
        return numerator * 100.0d / denominator;
    }

    private int normalizeLimit(int limit) {
        return limit <= 0 ? 100 : Math.min(limit, 500);
    }

    private static Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    public record CdpEvent(String sourceEventId,
                           String userId,
                           Instant eventTime,
                           Map<String, Object> properties) {
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

    public record EventResult(String status,
                              String operation,
                              long audienceId,
                              String sourceEventId,
                              String userId) {
    }

    public record OverlapResult(long leftCount,
                                long rightCount,
                                long intersectionCount,
                                double leftPercentage,
                                double rightPercentage) {
    }

    public record SetOperationResult(String status,
                                     String reason,
                                     long resultSize,
                                     long safeLimit) {
    }

    public record SnapshotInput(Long tenantId,
                                Long audienceId,
                                long estimatedSize,
                                String bitmapKey,
                                String snapshotSource,
                                String createdBy) {
    }

    public record SnapshotResult(Long audienceId,
                                 long estimatedSize,
                                 String bitmapKey,
                                 String snapshotSource) {
    }

    public record SnapshotRow(Long id,
                              Long tenantId,
                              Long audienceId,
                              Long estimatedSize,
                              String bitmapKey,
                              String snapshotSource,
                              String createdBy,
                              LocalDateTime createdAt) {
    }

    public interface EventLogRepository {
        boolean reserve(Long tenantId, Long audienceId, String sourceEventId, String userId, String operation);
    }

    public interface AudienceRuleRepository {
        boolean matches(Long tenantId, Long audienceId, Map<String, Object> eventProperties);
    }

    public interface SnapshotRepository {
        void insert(SnapshotInput input);

        List<SnapshotRow> list(Long tenantId, Long audienceId, int limit);
    }

    private static class MapperEventLogRepository implements EventLogRepository {
        private final CdpRealtimeAudienceEventLogMapper mapper;

        private MapperEventLogRepository(CdpRealtimeAudienceEventLogMapper mapper) {
            this.mapper = mapper;
        }

        @Override
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

    private static class MapperAudienceRuleRepository implements AudienceRuleRepository {
        private final AudienceDefinitionMapper mapper;
        private final CdpRuleEvaluator evaluator;

        private MapperAudienceRuleRepository(AudienceDefinitionMapper mapper, CdpRuleEvaluator evaluator) {
            this.mapper = mapper;
            this.evaluator = evaluator;
        }

        @Override
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

    private static class MapperSnapshotRepository implements SnapshotRepository {
        private final CdpAudienceSnapshotMapper mapper;

        private MapperSnapshotRepository(CdpAudienceSnapshotMapper mapper) {
            this.mapper = mapper;
        }

        @Override
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
