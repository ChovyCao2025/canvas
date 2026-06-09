package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
/**
 * CdpWarehouseEnterpriseOlapEvidenceCollectionService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseEnterpriseOlapEvidenceCollectionService {

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";
    private static final long PROOF_MAX_AGE_MINUTES = 15;

    private final CdpWarehouseEnterpriseOlapEvidenceService evidenceService;
    private final CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper mapper;
    private final Clock clock;

    @Autowired
    /**
     * 初始化 CdpWarehouseEnterpriseOlapEvidenceCollectionService 实例。
     *
     * @param evidenceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseEnterpriseOlapEvidenceCollectionService(
            CdpWarehouseEnterpriseOlapEvidenceService evidenceService,
            CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper mapper) {
        this(evidenceService, mapper, Clock.systemDefaultZone());
    }

    /**
     * 初始化 CdpWarehouseEnterpriseOlapEvidenceCollectionService 实例。
     *
     * @param evidenceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    CdpWarehouseEnterpriseOlapEvidenceCollectionService(
            CdpWarehouseEnterpriseOlapEvidenceService evidenceService,
            CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper mapper,
            Clock clock) {
        this.evidenceService = evidenceService;
        this.mapper = mapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param triggerType 类型标识，用于选择对应处理分支。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    public CollectionRunView run(Long tenantId, String triggerType, String actor) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        String trigger = defaultString(triggerType, "MANUAL").toUpperCase(Locale.ROOT);
        String createdBy = defaultString(actor, "system");
        LocalDateTime startedAt = now();
        CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO row = new CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO();
        row.setTenantId(scopedTenantId);
        row.setTriggerType(trigger);
        row.setStatus(STATUS_RUNNING);
        row.setStartedAt(startedAt);
        row.setEvidenceCount(0);
        row.setPassCount(0);
        row.setWarnCount(0);
        row.setFailCount(0);
        row.setReason("enterprise OLAP evidence collection running");
        row.setCreatedBy(createdBy);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        mapper.insert(row);
        try {
            CdpWarehouseEnterpriseOlapEvidenceService.EvidenceBundle bundle =
                    evidenceService.collectAutomatedEvidence(scopedTenantId, createdBy);
            Counts counts = counts(bundle.evidence());
            String status = normalizeStatus(bundle.status());
            String reason = "recorded " + counts.evidenceCount() + " enterprise OLAP evidence rows";
            LocalDateTime finishedAt = now();
            mapper.updateFinished(row.getId(), status, finishedAt, counts.evidenceCount(), counts.passCount(),
                    counts.warnCount(), counts.failCount(), reason);
            return new CollectionRunView(row.getId(), scopedTenantId, trigger, status, startedAt, finishedAt,
                    counts.evidenceCount(), counts.passCount(), counts.warnCount(), counts.failCount(), reason,
                    createdBy);
        } catch (RuntimeException e) {
            String reason = message(e);
            LocalDateTime finishedAt = now();
            mapper.updateFinished(row.getId(), STATUS_FAIL, finishedAt, 0, 0, 0, 0, reason);
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new CollectionRunView(row.getId(), scopedTenantId, trigger, STATUS_FAIL, startedAt, finishedAt,
                    0, 0, 0, 0, reason, createdBy);
        }
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<CollectionRunView> recentRuns(Long tenantId, int limit) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return safeList(mapper.listRecent(scopedTenantId, boundedLimit)).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .map(this::toView)
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 proofEvidence 流程生成的业务结果。
     */
    public CdpWarehouseProductionReadinessProofService.ProofEvidence proofEvidence(Long tenantId) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO> rows = safeList(mapper.listRecent(scopedTenantId, 1))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .toList();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (rows.isEmpty()) {
            return new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                    "enterprise_olap:evidence_collection",
                    STATUS_FAIL,
                    "enterprise OLAP automated evidence collection run is missing");
        }
        CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO latest = rows.getFirst();
        if (latest.getFinishedAt() == null || isExpired(latest.getFinishedAt())) {
            return new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                    "enterprise_olap:evidence_collection",
                    STATUS_FAIL,
                    "enterprise OLAP automated evidence collection run expired");
        }
        String status = normalizeStatus(latest.getStatus());
        return new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                "enterprise_olap:evidence_collection",
                status,
                defaultString(latest.getReason(), "enterprise OLAP automated evidence collection " + status));
    }

    /**
     * 统计符合条件的数据规模或状态数量。
     *
     * @param evidence evidence 参数，用于 counts 流程中的校验、计算或对象转换。
     * @return 返回统计数量。
     */
    private Counts counts(List<CdpWarehouseEnterpriseOlapEvidenceService.EvidenceView> evidence) {
        int pass = 0;
        int warn = 0;
        int fail = 0;
        for (CdpWarehouseEnterpriseOlapEvidenceService.EvidenceView row : safeList(evidence)) {
            String status = normalizeStatus(row.status());
            if (STATUS_PASS.equals(status)) {
                pass++;
            } else if (STATUS_WARN.equals(status)) {
                warn++;
            } else {
                fail++;
            }
        }
        return new Counts(safeList(evidence).size(), pass, warn, fail);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private CollectionRunView toView(CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO row) {
        return new CollectionRunView(row.getId(), row.getTenantId(), row.getTriggerType(), row.getStatus(),
                row.getStartedAt(), row.getFinishedAt(), value(row.getEvidenceCount()), value(row.getPassCount()),
                value(row.getWarnCount()), value(row.getFailCount()), row.getReason(), row.getCreatedBy());
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        String value = status == null ? STATUS_FAIL : status.trim().toUpperCase(Locale.ROOT);
        if (STATUS_PASS.equals(value) || STATUS_WARN.equals(value) || STATUS_FAIL.equals(value)) {
            return value;
        }
        return STATUS_FAIL;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 value 计算得到的数量、金额或指标值。
     */
    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param e e 参数，用于 message 流程中的校验、计算或对象转换。
     * @return 返回 message 生成的文本或业务键。
     */
    private String message(RuntimeException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param finishedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回布尔判断结果。
     */
    private boolean isExpired(LocalDateTime finishedAt) {
        return Duration.between(finishedAt, now()).toMinutes() > PROOF_MAX_AGE_MINUTES;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * Counts 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record Counts(int evidenceCount, int passCount, int warnCount, int failCount) {
    }

    /**
     * CollectionRunView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record CollectionRunView(
            Long id,
            Long tenantId,
            String triggerType,
            String status,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            int evidenceCount,
            int passCount,
            int warnCount,
            int failCount,
            String reason,
            String createdBy) {
    }
}
