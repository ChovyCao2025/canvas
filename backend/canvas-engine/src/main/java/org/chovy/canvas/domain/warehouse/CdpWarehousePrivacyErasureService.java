package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacyErasureAssetProofDO;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacyErasureRequestDO;
import org.chovy.canvas.dal.mapper.CdpWarehousePrivacyErasureAssetProofMapper;
import org.chovy.canvas.dal.mapper.CdpWarehousePrivacyErasureRequestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
/**
 * CdpWarehousePrivacyErasureService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehousePrivacyErasureService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_PLANNED = "PLANNED";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final int MAX_LIMIT = 100;
    private static final int MAX_TEXT_LENGTH = 1000;

    private static final List<String> DEFAULT_ASSETS = List.of(
            "CDP_USER_PROFILE",
            "CDP_USER_IDENTITY",
            "CDP_USER_TAG",
            "CDP_EVENT_LOG",
            "DORIS_ODS_CDP_EVENT_LOG",
            "DORIS_DWD_CDP_USER_EVENT_FACT",
            "REALTIME_RETRY_BUFFER",
            "AUDIENCE_BITMAP_VERSION");

    private final CdpWarehousePrivacyErasureRequestMapper requestMapper;
    private final CdpWarehousePrivacyErasureAssetProofMapper proofMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    /**
     * 初始化 CdpWarehousePrivacyErasureService 实例。
     *
     * @param requestMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param proofMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehousePrivacyErasureService(CdpWarehousePrivacyErasureRequestMapper requestMapper,
                                             CdpWarehousePrivacyErasureAssetProofMapper proofMapper) {
        this(requestMapper, proofMapper, new ObjectMapper().findAndRegisterModules(), Clock.systemDefaultZone());
    }

    /**
     * 初始化 CdpWarehousePrivacyErasureService 实例。
     *
     * @param requestMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param proofMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    CdpWarehousePrivacyErasureService(CdpWarehousePrivacyErasureRequestMapper requestMapper,
                                      CdpWarehousePrivacyErasureAssetProofMapper proofMapper,
                                      ObjectMapper objectMapper,
                                      Clock clock) {
        this.requestMapper = requestMapper;
        this.proofMapper = proofMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper().findAndRegisterModules() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public ErasureRequestView create(Long tenantId, ErasureRequestCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("erasure request command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String subjectType = upperDefault(command.subjectType(), "USER_ID");
        String subjectValue = required(command.subjectValue(), "subjectValue");
        List<String> assets = targetAssets(command.targetAssets());
        LocalDateTime now = now();

        CdpWarehousePrivacyErasureRequestDO row = new CdpWarehousePrivacyErasureRequestDO();
        row.setTenantId(scopedTenantId);
        row.setRequestKey(required(command.requestKey(), "requestKey"));
        row.setSubjectType(subjectType);
        row.setSubjectHash(subjectHash(scopedTenantId, subjectType, subjectValue));
        row.setSubjectRefMasked(mask(subjectValue));
        row.setReason(limit(required(command.reason(), "reason")));
        row.setRequestedBy(defaultString(command.requestedBy(), "system"));
        row.setStatus(STATUS_PENDING);
        row.setDueAt(command.dueAt() == null ? now.plusDays(7) : command.dueAt());
        row.setStartedAt(now);
        row.setTargetAssetsJson(toJson(assets));
        row.setEvidenceJson(toJson(List.of(new RequestEvidence("request_created", STATUS_PENDING,
                "erasure request accepted and asset proof plans created"))));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        requestMapper.insert(row);

        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String asset : assets) {
            proofMapper.insert(plan(row, asset));
        }
        return toView(row, proofs(row.getId()));
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requestId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public ErasureRequestView recordAssetProof(Long tenantId, Long requestId, AssetProofCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("asset proof command is required");
        }
        CdpWarehousePrivacyErasureRequestDO request = requireRequest(tenantId, requestId);
        String assetKey = upperRequired(command.assetKey(), "assetKey");
        CdpWarehousePrivacyErasureAssetProofDO proof = findProof(request.getTenantId(), request.getId(), assetKey);
        if (proof == null) {
            proof = new CdpWarehousePrivacyErasureAssetProofDO();
            proof.setTenantId(request.getTenantId());
            proof.setRequestId(request.getId());
            proof.setRequestKey(request.getRequestKey());
            proof.setAssetKey(assetKey);
            proof.setPlannedAction(plannedAction(assetKey));
        }
        proof.setAssetLayer(upperDefault(command.assetLayer(), assetLayer(assetKey)));
        proof.setActionType(upperDefault(command.actionType(), "ERASURE_PROOF"));
        proof.setStatus(proofStatus(command.status()));
        proof.setMatchedCount(nonNegative(command.matchedCount()));
        proof.setAffectedCount(nonNegative(command.affectedCount()));
        proof.setProofMessage(limit(sanitizeProofText(command.proofMessage(), request)));
        proof.setErrorMessage(limit(sanitizeProofText(command.errorMessage(), request)));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        proof.setExecutedBy(defaultString(command.executedBy(), "system"));
        proof.setExecutedAt(command.executedAt() == null ? now() : command.executedAt());
        if (proof.getId() == null) {
            proofMapper.insert(proof);
        } else {
            proofMapper.updateById(proof);
        }
        recomputeRequest(request);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return get(tenantId, requestId);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requestId 业务对象 ID，用于定位具体记录。
     * @return 返回 get 流程生成的业务结果。
     */
    public ErasureRequestView get(Long tenantId, Long requestId) {
        CdpWarehousePrivacyErasureRequestDO row = requireRequest(tenantId, requestId);
        return toView(row, proofs(row.getId()));
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<ErasureRequestView> recent(Long tenantId, String status, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehousePrivacyErasureRequestDO> query =
                new LambdaQueryWrapper<CdpWarehousePrivacyErasureRequestDO>()
                        .eq(CdpWarehousePrivacyErasureRequestDO::getTenantId, scopedTenantId)
                        .orderByDesc(CdpWarehousePrivacyErasureRequestDO::getId)
                        .last("LIMIT " + boundLimit(limit));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(status)) {
            query.eq(CdpWarehousePrivacyErasureRequestDO::getStatus,
                    status.trim().toUpperCase(Locale.ROOT));
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return safeRequests(requestMapper.selectList(query)).stream()
                .map(row -> toView(row, proofs(row.getId())))
                .toList();
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 summary 流程生成的业务结果。
     */
    public BacklogSummary summary(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime generatedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<CdpWarehousePrivacyErasureRequestDO> rows = safeRequests(requestMapper.selectList(
                new LambdaQueryWrapper<CdpWarehousePrivacyErasureRequestDO>()
                        .eq(CdpWarehousePrivacyErasureRequestDO::getTenantId, scopedTenantId)
                        .orderByDesc(CdpWarehousePrivacyErasureRequestDO::getId)
                        .last("LIMIT " + MAX_LIMIT)));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        long failed = rows.stream().filter(row -> STATUS_FAIL.equals(normalizeStatus(row.getStatus()))).count();
        long active = rows.stream().filter(row -> !STATUS_PASS.equals(normalizeStatus(row.getStatus()))).count();
        long pending = rows.stream().filter(row -> isActiveStatus(row.getStatus())).count();
        long overdue = rows.stream()
                .filter(row -> !STATUS_PASS.equals(normalizeStatus(row.getStatus())))
                .filter(row -> row.getDueAt() != null && row.getDueAt().isBefore(generatedAt))
                .count();
        String status = failed > 0 || overdue > 0 ? STATUS_FAIL : (active > 0 ? STATUS_WARN : STATUS_PASS);
        String reason = switch (status) {
            case STATUS_FAIL -> "privacy erasure backlog has failed or overdue requests";
            case STATUS_WARN -> "privacy erasure backlog has active requests";
            default -> "privacy erasure backlog is clear";
        };
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BacklogSummary(scopedTenantId, status, active, overdue, failed, pending, generatedAt, reason);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     */
    private void recomputeRequest(CdpWarehousePrivacyErasureRequestDO request) {
        List<CdpWarehousePrivacyErasureAssetProofDO> proofs = proofs(request.getId());
        String status = rollupStatus(proofs);
        request.setStatus(status);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        request.setEvidenceJson(toJson(proofs.stream()
                .map(proof -> new RequestEvidence(proof.getAssetKey(), normalizeStatus(proof.getStatus()),
                        defaultString(proof.getProofMessage(), proof.getPlannedAction())))
                .toList()));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (STATUS_PASS.equals(status) || STATUS_WARN.equals(status) || STATUS_FAIL.equals(status)) {
            request.setFinishedAt(now());
        } else {
            request.setFinishedAt(null);
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        requestMapper.updateById(request);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param proofs proofs 参数，用于 rollupStatus 流程中的校验、计算或对象转换。
     * @return 返回 rollup status 生成的文本或业务键。
     */
    private String rollupStatus(List<CdpWarehousePrivacyErasureAssetProofDO> proofs) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (proofs == null || proofs.isEmpty()) {
            return STATUS_PENDING;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<String> statuses = proofs.stream().map(row -> normalizeStatus(row.getStatus())).toList();
        if (statuses.contains(STATUS_FAIL)) {
            return STATUS_FAIL;
        }
        if (statuses.contains(STATUS_WARN)) {
            return STATUS_WARN;
        }
        if (statuses.stream().allMatch(status -> STATUS_PASS.equals(status) || STATUS_SKIPPED.equals(status))) {
            return STATUS_PASS;
        }
        if (statuses.stream().allMatch(STATUS_PLANNED::equals)) {
            return STATUS_PENDING;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return STATUS_RUNNING;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requestId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireRequest 流程生成的业务结果。
     */
    private CdpWarehousePrivacyErasureRequestDO requireRequest(Long tenantId, Long requestId) {
        if (requestId == null || requestId <= 0) {
            throw new IllegalArgumentException("requestId must be positive");
        }
        CdpWarehousePrivacyErasureRequestDO row = requestMapper.selectById(requestId);
        if (row == null || !normalizeTenant(tenantId).equals(row.getTenantId())) {
            throw new IllegalArgumentException("privacy erasure request not found: " + requestId);
        }
        return row;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 plan 流程生成的业务结果。
     */
    private CdpWarehousePrivacyErasureAssetProofDO plan(CdpWarehousePrivacyErasureRequestDO request,
                                                        String assetKey) {
        CdpWarehousePrivacyErasureAssetProofDO row = new CdpWarehousePrivacyErasureAssetProofDO();
        row.setTenantId(request.getTenantId());
        row.setRequestId(request.getId());
        row.setRequestKey(request.getRequestKey());
        row.setAssetKey(assetKey);
        row.setAssetLayer(assetLayer(assetKey));
        row.setActionType("ERASURE_PROOF");
        row.setStatus(STATUS_PLANNED);
        row.setPlannedAction(plannedAction(assetKey));
        row.setMatchedCount(0L);
        row.setAffectedCount(0L);
        return row;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requestId 业务对象 ID，用于定位具体记录。
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private CdpWarehousePrivacyErasureAssetProofDO findProof(Long tenantId, Long requestId, String assetKey) {
        List<CdpWarehousePrivacyErasureAssetProofDO> rows = proofMapper.selectList(
                new LambdaQueryWrapper<CdpWarehousePrivacyErasureAssetProofDO>()
                        .eq(CdpWarehousePrivacyErasureAssetProofDO::getTenantId, tenantId)
                        .eq(CdpWarehousePrivacyErasureAssetProofDO::getRequestId, requestId)
                        .eq(CdpWarehousePrivacyErasureAssetProofDO::getAssetKey, assetKey)
                        .last("LIMIT 1"));
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param requestId 业务对象 ID，用于定位具体记录。
     * @return 返回 proofs 汇总后的集合、分页或映射视图。
     */
    private List<CdpWarehousePrivacyErasureAssetProofDO> proofs(Long requestId) {
        return safeProofs(proofMapper.selectList(
                new LambdaQueryWrapper<CdpWarehousePrivacyErasureAssetProofDO>()
                        .eq(CdpWarehousePrivacyErasureAssetProofDO::getRequestId, requestId)
                        .orderByAsc(CdpWarehousePrivacyErasureAssetProofDO::getId)));
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param proofs proofs 参数，用于 toView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private ErasureRequestView toView(CdpWarehousePrivacyErasureRequestDO row,
                                      List<CdpWarehousePrivacyErasureAssetProofDO> proofs) {
        return new ErasureRequestView(
                row.getId(),
                row.getTenantId(),
                row.getRequestKey(),
                row.getSubjectType(),
                row.getSubjectHash(),
                row.getSubjectRefMasked(),
                row.getReason(),
                row.getRequestedBy(),
                row.getStatus(),
                row.getDueAt(),
                row.getStartedAt(),
                row.getFinishedAt(),
                row.getTargetAssetsJson(),
                row.getEvidenceJson(),
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                safeProofs(proofs).stream().map(this::toProofView).toList(),
                row.getCreatedAt(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private AssetProofView toProofView(CdpWarehousePrivacyErasureAssetProofDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new AssetProofView(
                row.getId(),
                row.getTenantId(),
                row.getRequestId(),
                row.getRequestKey(),
                row.getAssetKey(),
                row.getAssetLayer(),
                row.getActionType(),
                row.getStatus(),
                row.getPlannedAction(),
                nullToZero(row.getMatchedCount()),
                nullToZero(row.getAffectedCount()),
                row.getProofMessage(),
                row.getErrorMessage(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getExecutedBy(),
                row.getExecutedAt(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param assets assets 参数，用于 targetAssets 流程中的校验、计算或对象转换。
     * @return 返回 target assets 汇总后的集合、分页或映射视图。
     */
    private List<String> targetAssets(List<String> assets) {
        Set<String> normalized = new LinkedHashSet<>();
        List<String> input = assets == null || assets.isEmpty() ? DEFAULT_ASSETS : assets;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String asset : input) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (hasText(asset)) {
                normalized.add(asset.trim().toUpperCase(Locale.ROOT));
            }
        }
        if (normalized.isEmpty()) {
            normalized.addAll(DEFAULT_ASSETS);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(normalized);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @return 返回 subject hash 生成的文本或业务键。
     */
    private String subjectHash(Long tenantId, String subjectType, String subjectValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((tenantId + ":" + subjectType + ":" + subjectValue)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("failed to hash erasure subject", e);
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String mask(String subjectValue) {
        String value = required(subjectValue, "subjectValue");
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 asset layer 生成的文本或业务键。
     */
    private String assetLayer(String assetKey) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (assetKey.startsWith("DORIS_ODS")) {
            return "ODS";
        }
        if (assetKey.startsWith("DORIS_DWD")) {
            return "DWD";
        }
        if (assetKey.startsWith("DORIS_DWS")) {
            return "DWS";
        }
        if (assetKey.startsWith("AUDIENCE")) {
            return "ADS";
        }
        if (assetKey.startsWith("REALTIME")) {
            return "REALTIME";
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "CDP";
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 planned action 生成的文本或业务键。
     */
    private String plannedAction(String assetKey) {
        return "prove erasure propagation for " + assetKey;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 proof status 生成的文本或业务键。
     */
    private String proofStatus(String status) {
        String normalized = normalizeStatus(status);
        if (STATUS_PASS.equals(normalized) || STATUS_WARN.equals(normalized)
                || STATUS_FAIL.equals(normalized) || STATUS_SKIPPED.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("asset proof status must be PASS, WARN, FAIL, or SKIPPED");
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        if (!hasText(status)) {
            return STATUS_FAIL;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回布尔判断结果。
     */
    private boolean isActiveStatus(String status) {
        String normalized = normalizeStatus(status);
        return STATUS_PENDING.equals(normalized) || STATUS_RUNNING.equals(normalized) || STATUS_WARN.equals(normalized);
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
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 upper required 生成的文本或业务键。
     */
    private String upperRequired(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 upperDefault 流程中的校验、计算或对象转换。
     * @return 返回 upper default 生成的文本或业务键。
     */
    private String upperDefault(String value, String fallback) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
        return value.length() <= MAX_TEXT_LENGTH ? value : value.substring(0, MAX_TEXT_LENGTH);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 sanitize proof text 生成的文本或业务键。
     */
    private String sanitizeProofText(String value, CdpWarehousePrivacyErasureRequestDO request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null || request == null || !hasText(request.getSubjectRefMasked())) {
            return value;
        }
        String masked = request.getSubjectRefMasked().trim();
        String sanitized = value.replace(masked, "[REDACTED_SUBJECT]");
        int marker = masked.indexOf("***");
        if (marker <= 0 || marker + 3 >= masked.length()) {
            return sanitized;
        }
        String prefix = masked.substring(0, marker);
        String suffix = masked.substring(marker + 3);
        if (!hasText(prefix) || !hasText(suffix)) {
            return sanitized;
        }
        Pattern rawSubjectToken = Pattern.compile(
                "(?<![A-Za-z0-9])"
                        + Pattern.quote(prefix)
                        + "[A-Za-z0-9._@+\\-:]{1,200}"
                        + Pattern.quote(suffix)
                        + "(?![A-Za-z0-9])");
        // 汇总前面计算出的状态和明细，返回给调用方。
        return rawSubjectToken.matcher(sanitized).replaceAll("[REDACTED_SUBJECT]");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 non negative 计算得到的数量、金额或指标值。
     */
    private long nonNegative(Long value) {
        return value == null || value < 0 ? 0L : value;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to zero 计算得到的数量、金额或指标值。
     */
    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundLimit(int value) {
        int limit = value <= 0 ? 20 : value;
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize privacy erasure proof", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safeRequests 流程中的校验、计算或对象转换。
     * @return 返回 safe requests 汇总后的集合、分页或映射视图。
     */
    private List<CdpWarehousePrivacyErasureRequestDO> safeRequests(
            List<CdpWarehousePrivacyErasureRequestDO> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safeProofs 流程中的校验、计算或对象转换。
     * @return 返回 safe proofs 汇总后的集合、分页或映射视图。
     */
    private List<CdpWarehousePrivacyErasureAssetProofDO> safeProofs(
            List<CdpWarehousePrivacyErasureAssetProofDO> rows) {
        return rows == null ? List.of() : rows;
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
     * ErasureRequestCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ErasureRequestCommand(
            String requestKey,
            String subjectType,
            String subjectValue,
            String reason,
            String requestedBy,
            LocalDateTime dueAt,
            List<String> targetAssets) {
    }

    /**
     * AssetProofCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AssetProofCommand(
            String assetKey,
            String assetLayer,
            String actionType,
            String status,
            Long matchedCount,
            Long affectedCount,
            String proofMessage,
            String errorMessage,
            String executedBy,
            LocalDateTime executedAt) {
    }

    /**
     * ErasureRequestView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ErasureRequestView(
            Long id,
            Long tenantId,
            String requestKey,
            String subjectType,
            String subjectHash,
            String subjectRefMasked,
            String reason,
            String requestedBy,
            String status,
            LocalDateTime dueAt,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            String targetAssetsJson,
            String evidenceJson,
            List<AssetProofView> assetProofs,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        public ErasureRequestView {
            assetProofs = assetProofs == null ? List.of() : List.copyOf(assetProofs);
        }
    }

    /**
     * AssetProofView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AssetProofView(
            Long id,
            Long tenantId,
            Long requestId,
            String requestKey,
            String assetKey,
            String assetLayer,
            String actionType,
            String status,
            String plannedAction,
            long matchedCount,
            long affectedCount,
            String proofMessage,
            String errorMessage,
            String executedBy,
            LocalDateTime executedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    /**
     * BacklogSummary 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record BacklogSummary(
            Long tenantId,
            String status,
            long activeCount,
            long overdueCount,
            long failedCount,
            long pendingCount,
            LocalDateTime generatedAt,
            String reason) {
    }

    /**
     * RequestEvidence 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RequestEvidence(
            String key,
            String status,
            String reason) {
    }
}
