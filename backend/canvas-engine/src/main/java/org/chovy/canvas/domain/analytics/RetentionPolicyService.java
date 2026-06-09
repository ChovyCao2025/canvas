package org.chovy.canvas.domain.analytics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.AnalyticsRetentionPolicyDO;
import org.chovy.canvas.dal.dataobject.AnalyticsRetentionRunDO;
import org.chovy.canvas.dal.mapper.AnalyticsEventMapper;
import org.chovy.canvas.dal.mapper.AnalyticsEventTraceMapper;
import org.chovy.canvas.dal.mapper.AnalyticsRetentionPolicyMapper;
import org.chovy.canvas.dal.mapper.AnalyticsRetentionRunMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class RetentionPolicyService {

    private static final Long PLATFORM_TENANT_ID = 0L;
    private static final String KIND_EVENT = "EVENT";
    private static final String KIND_TRACE = "TRACE";
    private static final String ACTION_ARCHIVE = "ARCHIVE";
    private static final String ACTION_DELETE = "DELETE";
    private static final Set<String> SUPPORTED_KINDS = Set.of(KIND_EVENT, KIND_TRACE);
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(ACTION_ARCHIVE, ACTION_DELETE);

    private final PolicyRepository policyRepository;
    private final RetentionTargetRepository targetRepository;
    private final RunRepository runRepository;
    private final Bounds bounds;

    public RetentionPolicyService(AnalyticsRetentionPolicyMapper policyMapper,
                                  AnalyticsRetentionRunMapper runMapper,
                                  AnalyticsEventMapper eventMapper,
                                  AnalyticsEventTraceMapper traceMapper,
                                  @Value("${canvas.analytics.retention.min-days:7}") int minDays,
                                  @Value("${canvas.analytics.retention.max-days:730}") int maxDays,
                                  @Value("${canvas.analytics.retention.default-days:90}") int defaultDays,
                                  @Value("${canvas.analytics.retention.max-batch-size:1000}") int maxBatchSize) {
        this(
                new MyBatisPolicyRepository(policyMapper),
                new MyBatisRetentionTargetRepository(eventMapper, traceMapper),
                new MyBatisRunRepository(runMapper),
                new Bounds(minDays, maxDays, defaultDays, maxBatchSize));
    }

    RetentionPolicyService(PolicyRepository policyRepository,
                           RetentionTargetRepository targetRepository,
                           RunRepository runRepository,
                           Bounds bounds) {
        this.policyRepository = policyRepository;
        this.targetRepository = targetRepository;
        this.runRepository = runRepository;
        this.bounds = bounds;
    }

    public PolicyInput resolvePolicy(Long tenantId, String recordKind, boolean dryRun) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String normalizedKind = normalizeRequired(recordKind, "recordKind");
        AnalyticsRetentionPolicyDO policy = policyRepository.findPolicy(scopedTenantId, normalizedKind)
                .or(() -> policyRepository.findPolicy(PLATFORM_TENANT_ID, normalizedKind))
                .orElseGet(() -> defaultPolicy(scopedTenantId, normalizedKind));
        PolicyInput input = new PolicyInput(
                scopedTenantId,
                normalizedKind,
                valueOrDefault(policy.getRetentionDays(), bounds.defaultRetentionDays()),
                normalizeRequired(valueOrDefault(policy.getAction(), ACTION_ARCHIVE), "action"),
                valueOrDefault(policy.getMaxBatchSize(), bounds.maxBatchSize()),
                dryRun);
        validate(input);
        return input;
    }

    public void validate(PolicyInput input) {
        if (input == null) {
            throw new IllegalArgumentException("policy input is required");
        }
        String recordKind = normalizeRequired(input.recordKind(), "recordKind");
        String action = normalizeRequired(input.action(), "action");
        if (!SUPPORTED_KINDS.contains(recordKind)) {
            throw new IllegalArgumentException("unsupported record kind: " + input.recordKind());
        }
        if (!SUPPORTED_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("unsupported retention action: " + input.action());
        }
        if (input.retentionDays() < bounds.minDays()) {
            throw new IllegalArgumentException("retentionDays below platform minimum: " + bounds.minDays());
        }
        if (input.retentionDays() > bounds.maxDays()) {
            throw new IllegalArgumentException("retentionDays above platform maximum: " + bounds.maxDays());
        }
        if (input.maxBatchSize() <= 0) {
            throw new IllegalArgumentException("maxBatchSize must be positive");
        }
        if (input.maxBatchSize() > bounds.maxBatchSize()) {
            throw new IllegalArgumentException("maxBatchSize above platform maximum: " + bounds.maxBatchSize());
        }
    }

    public RunResult run(PolicyInput input) {
        validate(input);
        Long tenantId = normalizeTenant(input.tenantId());
        String recordKind = normalizeRequired(input.recordKind(), "recordKind");
        String action = normalizeRequired(input.action(), "action");
        long eligible = targetRepository.countEligible(tenantId, recordKind, input.retentionDays(), true);
        long archived = 0;
        long deleted = 0;
        if (!input.dryRun()) {
            if (ACTION_ARCHIVE.equals(action)) {
                archived = targetRepository.archiveBatch(
                        tenantId,
                        recordKind,
                        input.retentionDays(),
                        input.maxBatchSize());
            } else if (ACTION_DELETE.equals(action)) {
                deleted = targetRepository.deleteBatch(
                        tenantId,
                        recordKind,
                        input.retentionDays(),
                        input.maxBatchSize());
            }
        }
        RunResult result = new RunResult(
                tenantId,
                recordKind,
                action,
                input.dryRun(),
                eligible,
                archived,
                deleted,
                input.dryRun() ? 0 : Math.max(0, eligible - archived - deleted),
                0,
                LocalDateTime.now());
        runRepository.record(result);
        return result;
    }

    private AnalyticsRetentionPolicyDO defaultPolicy(Long tenantId, String recordKind) {
        AnalyticsRetentionPolicyDO row = new AnalyticsRetentionPolicyDO();
        row.setTenantId(tenantId);
        row.setRecordKind(recordKind);
        row.setRetentionDays(bounds.defaultRetentionDays());
        row.setAction(ACTION_ARCHIVE);
        row.setMaxBatchSize(bounds.maxBatchSize());
        row.setLegalHoldBehavior("SKIP");
        row.setEnabled(true);
        return row;
    }

    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null ? PLATFORM_TENANT_ID : tenantId;
    }

    private static String normalizeRequired(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static int valueOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public interface PolicyRepository {
        Optional<AnalyticsRetentionPolicyDO> findPolicy(Long tenantId, String recordKind);
    }

    public interface RetentionTargetRepository {
        long countEligible(Long tenantId, String recordKind, int retentionDays, boolean skipLegalHold);

        int archiveBatch(Long tenantId, String recordKind, int retentionDays, int limit);

        int deleteBatch(Long tenantId, String recordKind, int retentionDays, int limit);
    }

    public interface RunRepository {
        void record(RunResult result);
    }

    public record Bounds(int minDays, int maxDays, int defaultRetentionDays, int maxBatchSize) {
        public Bounds {
            if (minDays <= 0) {
                throw new IllegalArgumentException("minDays must be positive");
            }
            if (maxDays < minDays) {
                throw new IllegalArgumentException("maxDays must be >= minDays");
            }
            if (defaultRetentionDays < minDays || defaultRetentionDays > maxDays) {
                throw new IllegalArgumentException("defaultRetentionDays must stay within bounds");
            }
            if (maxBatchSize <= 0) {
                throw new IllegalArgumentException("maxBatchSize must be positive");
            }
        }
    }

    public record PolicyInput(
            Long tenantId,
            String recordKind,
            int retentionDays,
            String action,
            int maxBatchSize,
            boolean dryRun) {
    }

    public record RunResult(
            Long tenantId,
            String recordKind,
            String action,
            boolean dryRun,
            long scannedCount,
            long archivedCount,
            long deletedCount,
            long skippedCount,
            long failedCount,
            LocalDateTime finishedAt) {
    }

    private static class MyBatisPolicyRepository implements PolicyRepository {
        private final AnalyticsRetentionPolicyMapper mapper;

        private MyBatisPolicyRepository(AnalyticsRetentionPolicyMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public Optional<AnalyticsRetentionPolicyDO> findPolicy(Long tenantId, String recordKind) {
            return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<AnalyticsRetentionPolicyDO>()
                    .eq(AnalyticsRetentionPolicyDO::getTenantId, tenantId)
                    .eq(AnalyticsRetentionPolicyDO::getRecordKind, recordKind)
                    .eq(AnalyticsRetentionPolicyDO::getEnabled, true)
                    .last("LIMIT 1")));
        }
    }

    private static class MyBatisRetentionTargetRepository implements RetentionTargetRepository {
        private final AnalyticsEventMapper eventMapper;
        private final AnalyticsEventTraceMapper traceMapper;

        private MyBatisRetentionTargetRepository(AnalyticsEventMapper eventMapper,
                                                 AnalyticsEventTraceMapper traceMapper) {
            this.eventMapper = eventMapper;
            this.traceMapper = traceMapper;
        }

        @Override
        public long countEligible(Long tenantId, String recordKind, int retentionDays, boolean skipLegalHold) {
            return switch (recordKind) {
                case KIND_EVENT -> eventMapper.countRetentionEligible(tenantId, retentionDays, skipLegalHold);
                case KIND_TRACE -> traceMapper.countRetentionEligible(tenantId, retentionDays, skipLegalHold);
                default -> throw new IllegalArgumentException("unsupported record kind: " + recordKind);
            };
        }

        @Override
        public int archiveBatch(Long tenantId, String recordKind, int retentionDays, int limit) {
            return switch (recordKind) {
                case KIND_EVENT -> eventMapper.archiveRetentionBatch(tenantId, retentionDays, limit);
                case KIND_TRACE -> traceMapper.archiveRetentionBatch(tenantId, retentionDays, limit);
                default -> throw new IllegalArgumentException("unsupported record kind: " + recordKind);
            };
        }

        @Override
        public int deleteBatch(Long tenantId, String recordKind, int retentionDays, int limit) {
            return switch (recordKind) {
                case KIND_EVENT -> eventMapper.deleteRetentionBatch(tenantId, retentionDays, limit);
                case KIND_TRACE -> traceMapper.deleteRetentionBatch(tenantId, retentionDays, limit);
                default -> throw new IllegalArgumentException("unsupported record kind: " + recordKind);
            };
        }
    }

    private static class MyBatisRunRepository implements RunRepository {
        private final AnalyticsRetentionRunMapper mapper;

        private MyBatisRunRepository(AnalyticsRetentionRunMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public void record(RunResult result) {
            AnalyticsRetentionRunDO row = new AnalyticsRetentionRunDO();
            row.setTenantId(result.tenantId());
            row.setRecordKind(result.recordKind());
            row.setAction(result.action());
            row.setDryRun(result.dryRun());
            row.setScannedCount(result.scannedCount());
            row.setArchivedCount(result.archivedCount());
            row.setDeletedCount(result.deletedCount());
            row.setSkippedCount(result.skippedCount());
            row.setFailedCount(result.failedCount());
            row.setStartedAt(result.finishedAt());
            row.setFinishedAt(result.finishedAt());
            mapper.insert(row);
        }
    }
}
