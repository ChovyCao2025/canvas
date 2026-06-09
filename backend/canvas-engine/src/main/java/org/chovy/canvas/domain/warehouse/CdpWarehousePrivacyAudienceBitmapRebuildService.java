package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.domain.analytics.AudienceMaterializationOperationsService;
import org.chovy.canvas.domain.analytics.AudienceMaterializationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
/**
 * CdpWarehousePrivacyAudienceBitmapRebuildService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehousePrivacyAudienceBitmapRebuildService {

    private static final String ASSET_KEY = "AUDIENCE_BITMAP_VERSION";
    private static final String PASS = "PASS";
    private static final String WARN = "WARN";
    private static final String FAIL = "FAIL";
    private static final String SKIPPED = "SKIPPED";
    private static final String SUCCESS = "SUCCESS";
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final String DEFAULT_ACTOR = "privacy-audience-rebuild";

    private final CdpWarehousePrivacyErasureService erasureService;
    private final AudienceDefinitionMapper definitionMapper;
    private final AudienceMaterializationOperationsService operationsService;

    /**
     * 初始化 CdpWarehousePrivacyAudienceBitmapRebuildService 实例。
     *
     * @param erasureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param definitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param operationsService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehousePrivacyAudienceBitmapRebuildService(
            CdpWarehousePrivacyErasureService erasureService,
            AudienceDefinitionMapper definitionMapper,
            AudienceMaterializationOperationsService operationsService) {
        this.erasureService = erasureService;
        this.definitionMapper = definitionMapper;
        this.operationsService = operationsService;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requestId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 rebuild 流程生成的业务结果。
     */
    public AudienceBitmapRebuildResult rebuild(Long tenantId,
                                               Long requestId,
                                               AudienceBitmapRebuildCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        AudienceBitmapRebuildCommand scopedCommand =
                command == null ? new AudienceBitmapRebuildCommand(null, null, null) : command;
        String actor = actor(scopedCommand.actor());
        CdpWarehousePrivacyErasureService.ErasureRequestView request =
                erasureService.get(scopedTenantId, requestId);

        List<CdpWarehousePrivacyErasureService.AssetProofView> blockers =
                upstreamBlockers(request.assetProofs());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!blockers.isEmpty()) {
            String message = limit("audience bitmap rebuild blocked until upstream erasure proofs pass: "
                    + blockerSummary(blockers));
            recordProof(scopedTenantId, requestId, WARN, 0, 0, message, null, actor);
            return new AudienceBitmapRebuildResult(
                    scopedTenantId, requestId, WARN, true, false, 0, 0, 0, 0, List.of());
        }

        int limit = boundLimit(scopedCommand.limit());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<AudienceDefinitionDO> selected = selectCandidates(scopedTenantId, scopedCommand.audienceIds(), limit);
        boolean truncated = selected.size() > limit;
        if (truncated) {
            selected = selected.subList(0, limit);
        }
        if (selected.isEmpty()) {
            String message = "no enabled offline or hybrid audience materializations require rebuild";
            recordProof(scopedTenantId, requestId, SKIPPED, 0, 0, message, null, actor);
            return new AudienceBitmapRebuildResult(
                    scopedTenantId, requestId, SKIPPED, false, false, 0, 0, 0, 0, List.of());
        }

        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<AudienceRebuildItem> results = selected.stream()
                .map(definition -> rebuildAudience(scopedTenantId, definition.getId(), actor))
                .toList();
        int rebuilt = (int) results.stream().filter(item -> SUCCESS.equalsIgnoreCase(item.status())).count();
        int failed = results.size() - rebuilt;
        long matchedUsers = results.stream().mapToLong(AudienceRebuildItem::matchedUsers).sum();
        String status = aggregateStatus(failed, truncated);
        String message = proofMessage(status, results.size(), rebuilt, failed, truncated, limit);
        String errorMessage = failed > 0 ? failedSummary(results) : null;
        recordProof(scopedTenantId, requestId, status, results.size(), rebuilt, message, errorMessage, actor);

        return new AudienceBitmapRebuildResult(
                scopedTenantId,
                requestId,
                status,
                false,
                truncated,
                results.size(),
                rebuilt,
                failed,
                matchedUsers,
                results);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param proofs proofs 参数，用于 upstreamBlockers 流程中的校验、计算或对象转换。
     * @return 返回 upstream blockers 汇总后的集合、分页或映射视图。
     */
    private List<CdpWarehousePrivacyErasureService.AssetProofView> upstreamBlockers(
            List<CdpWarehousePrivacyErasureService.AssetProofView> proofs) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (proofs == null || proofs.isEmpty()) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return proofs.stream()
                .filter(proof -> proof != null && !ASSET_KEY.equalsIgnoreCase(proof.assetKey()))
                .filter(proof -> !isTerminalPass(proof.status()))
                .toList();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回布尔判断结果。
     */
    private boolean isTerminalPass(String status) {
        String normalized = normalizeStatus(status);
        return PASS.equals(normalized) || SKIPPED.equals(normalized);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceIds audience ids 参数，用于 selectCandidates 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<AudienceDefinitionDO> selectCandidates(Long tenantId, List<Long> audienceIds, int limit) {
        boolean explicitAudienceScope = audienceIds != null && !audienceIds.isEmpty();
        List<Long> scopedIds = audienceIds(audienceIds);
        if (explicitAudienceScope && scopedIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<AudienceDefinitionDO> query = new LambdaQueryWrapper<AudienceDefinitionDO>()
                .eq(AudienceDefinitionDO::getTenantId, tenantId)
                .eq(AudienceDefinitionDO::getEnabled, 1)
                .in(AudienceDefinitionDO::getEvaluationStrategy, List.of("OFFLINE_BATCH", "HYBRID"))
                .orderByAsc(AudienceDefinitionDO::getUpdatedAt)
                .orderByAsc(AudienceDefinitionDO::getId)
                .last("LIMIT " + (limit + 1));
        if (!scopedIds.isEmpty()) {
            query.in(AudienceDefinitionDO::getId, scopedIds);
        }
        List<AudienceDefinitionDO> rows = definitionMapper.selectList(query);
        return rows == null ? List.of() : rows;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 rebuildAudience 流程生成的业务结果。
     */
    private AudienceRebuildItem rebuildAudience(Long tenantId, Long audienceId, String actor) {
        try {
            AudienceMaterializationService.MaterializationResult result =
                    operationsService.materialize(tenantId, audienceId, actor);
            String status = result == null ? FAIL : normalizeStatus(result.status());
            long matchedUsers = result == null ? 0 : Math.max(result.matchedUsers(), 0);
            String message = SUCCESS.equals(status)
                    ? "rebuilt audience " + audienceId
                    : "audience " + audienceId + " materialization returned " + status;
            return new AudienceRebuildItem(audienceId, status, matchedUsers, message);
        } catch (RuntimeException e) {
            return new AudienceRebuildItem(
                    audienceId, FAIL, 0, "audience " + audienceId + " rebuild failed: " + e.getMessage());
        }
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requestId 业务对象 ID，用于定位具体记录。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param matchedCount matched count 参数，用于 recordProof 流程中的校验、计算或对象转换。
     * @param affectedCount affected count 参数，用于 recordProof 流程中的校验、计算或对象转换。
     * @param proofMessage proof message 参数，用于 recordProof 流程中的校验、计算或对象转换。
     * @param errorMessage error message 参数，用于 recordProof 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void recordProof(Long tenantId,
                             Long requestId,
                             String status,
                             long matchedCount,
                             long affectedCount,
                             String proofMessage,
                             String errorMessage,
                             String actor) {
        erasureService.recordAssetProof(tenantId, requestId,
                new CdpWarehousePrivacyErasureService.AssetProofCommand(
                        ASSET_KEY,
                        "ADS",
                        "REBUILD",
                        status,
                        matchedCount,
                        affectedCount,
                        limit(proofMessage),
                        limit(errorMessage),
                        actor,
                        LocalDateTime.now()));
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param failed failed 参数，用于 aggregateStatus 流程中的校验、计算或对象转换。
     * @param truncated truncated 参数，用于 aggregateStatus 流程中的校验、计算或对象转换。
     * @return 返回 aggregate status 生成的文本或业务键。
     */
    private String aggregateStatus(int failed, boolean truncated) {
        if (failed > 0) {
            return FAIL;
        }
        return truncated ? WARN : PASS;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param selected selected 参数，用于 proofMessage 流程中的校验、计算或对象转换。
     * @param rebuilt rebuilt 参数，用于 proofMessage 流程中的校验、计算或对象转换。
     * @param failed failed 参数，用于 proofMessage 流程中的校验、计算或对象转换。
     * @param truncated truncated 参数，用于 proofMessage 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 proof message 生成的文本或业务键。
     */
    private String proofMessage(String status, int selected, int rebuilt, int failed, boolean truncated, int limit) {
        if (FAIL.equals(status)) {
            return "rebuilt " + rebuilt + " of " + selected
                    + " audience bitmap version(s); " + failed + " failed";
        }
        if (truncated) {
            return "rebuilt " + rebuilt
                    + " audience bitmap version(s); candidate selection truncated by limit " + limit;
        }
        return "rebuilt " + rebuilt + " audience bitmap version(s) after privacy erasure request";
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param results results 参数，用于 failedSummary 流程中的校验、计算或对象转换。
     * @return 返回 failed summary 生成的文本或业务键。
     */
    private String failedSummary(List<AudienceRebuildItem> results) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return limit(results.stream()
                .filter(item -> !SUCCESS.equalsIgnoreCase(item.status()))
                .map(AudienceRebuildItem::message)
                .reduce((left, right) -> left + "; " + right)
                .orElse(null));
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param blockers blockers 参数，用于 blockerSummary 流程中的校验、计算或对象转换。
     * @return 返回 blocker summary 生成的文本或业务键。
     */
    private String blockerSummary(List<CdpWarehousePrivacyErasureService.AssetProofView> blockers) {
        return blockers.stream()
                .map(proof -> proof.assetKey() + "=" + normalizeStatus(proof.status()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("unknown upstream asset");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回 audience ids 汇总后的集合、分页或映射视图。
     */
    private List<Long> audienceIds(List<Long> input) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Long id : input) {
            if (id != null && id > 0) {
                ids.add(id);
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(ids);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundLimit(Integer input) {
        int value = input == null || input <= 0 ? DEFAULT_LIMIT : input;
        return Math.min(value, MAX_LIMIT);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(String actor) {
        return actor != null && !actor.isBlank() ? actor.trim() : DEFAULT_ACTOR;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? FAIL : status.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limit(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_MESSAGE_LENGTH ? value : value.substring(0, MAX_MESSAGE_LENGTH);
    }

    /**
     * AudienceBitmapRebuildCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AudienceBitmapRebuildCommand(
            String actor,
            Integer limit,
            List<Long> audienceIds) {
        public AudienceBitmapRebuildCommand {
            audienceIds = audienceIds == null ? List.of() : List.copyOf(audienceIds);
        }
    }

    /**
     * AudienceBitmapRebuildResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AudienceBitmapRebuildResult(
            Long tenantId,
            Long requestId,
            String status,
            boolean blocked,
            boolean truncated,
            int selectedAudiences,
            int rebuiltAudiences,
            int failedAudiences,
            long matchedUsers,
            List<AudienceRebuildItem> audienceResults) {
        public AudienceBitmapRebuildResult {
            audienceResults = audienceResults == null ? List.of() : List.copyOf(audienceResults);
        }
    }

    /**
     * AudienceRebuildItem 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AudienceRebuildItem(
            Long audienceId,
            String status,
            long matchedUsers,
            String message) {
    }
}
