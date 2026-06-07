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
    public CdpWarehousePrivacyErasureService(CdpWarehousePrivacyErasureRequestMapper requestMapper,
                                             CdpWarehousePrivacyErasureAssetProofMapper proofMapper) {
        this(requestMapper, proofMapper, new ObjectMapper().findAndRegisterModules(), Clock.systemDefaultZone());
    }

    CdpWarehousePrivacyErasureService(CdpWarehousePrivacyErasureRequestMapper requestMapper,
                                      CdpWarehousePrivacyErasureAssetProofMapper proofMapper,
                                      ObjectMapper objectMapper,
                                      Clock clock) {
        this.requestMapper = requestMapper;
        this.proofMapper = proofMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper().findAndRegisterModules() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public ErasureRequestView create(Long tenantId, ErasureRequestCommand command) {
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
        requestMapper.insert(row);

        for (String asset : assets) {
            proofMapper.insert(plan(row, asset));
        }
        return toView(row, proofs(row.getId()));
    }

    public ErasureRequestView recordAssetProof(Long tenantId, Long requestId, AssetProofCommand command) {
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
        proof.setExecutedBy(defaultString(command.executedBy(), "system"));
        proof.setExecutedAt(command.executedAt() == null ? now() : command.executedAt());
        if (proof.getId() == null) {
            proofMapper.insert(proof);
        } else {
            proofMapper.updateById(proof);
        }
        recomputeRequest(request);
        return get(tenantId, requestId);
    }

    public ErasureRequestView get(Long tenantId, Long requestId) {
        CdpWarehousePrivacyErasureRequestDO row = requireRequest(tenantId, requestId);
        return toView(row, proofs(row.getId()));
    }

    public List<ErasureRequestView> recent(Long tenantId, String status, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehousePrivacyErasureRequestDO> query =
                new LambdaQueryWrapper<CdpWarehousePrivacyErasureRequestDO>()
                        .eq(CdpWarehousePrivacyErasureRequestDO::getTenantId, scopedTenantId)
                        .orderByDesc(CdpWarehousePrivacyErasureRequestDO::getId)
                        .last("LIMIT " + boundLimit(limit));
        if (hasText(status)) {
            query.eq(CdpWarehousePrivacyErasureRequestDO::getStatus,
                    status.trim().toUpperCase(Locale.ROOT));
        }
        return safeRequests(requestMapper.selectList(query)).stream()
                .map(row -> toView(row, proofs(row.getId())))
                .toList();
    }

    public BacklogSummary summary(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime generatedAt = now();
        List<CdpWarehousePrivacyErasureRequestDO> rows = safeRequests(requestMapper.selectList(
                new LambdaQueryWrapper<CdpWarehousePrivacyErasureRequestDO>()
                        .eq(CdpWarehousePrivacyErasureRequestDO::getTenantId, scopedTenantId)
                        .orderByDesc(CdpWarehousePrivacyErasureRequestDO::getId)
                        .last("LIMIT " + MAX_LIMIT)));
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
        return new BacklogSummary(scopedTenantId, status, active, overdue, failed, pending, generatedAt, reason);
    }

    private void recomputeRequest(CdpWarehousePrivacyErasureRequestDO request) {
        List<CdpWarehousePrivacyErasureAssetProofDO> proofs = proofs(request.getId());
        String status = rollupStatus(proofs);
        request.setStatus(status);
        request.setEvidenceJson(toJson(proofs.stream()
                .map(proof -> new RequestEvidence(proof.getAssetKey(), normalizeStatus(proof.getStatus()),
                        defaultString(proof.getProofMessage(), proof.getPlannedAction())))
                .toList()));
        if (STATUS_PASS.equals(status) || STATUS_WARN.equals(status) || STATUS_FAIL.equals(status)) {
            request.setFinishedAt(now());
        } else {
            request.setFinishedAt(null);
        }
        requestMapper.updateById(request);
    }

    private String rollupStatus(List<CdpWarehousePrivacyErasureAssetProofDO> proofs) {
        if (proofs == null || proofs.isEmpty()) {
            return STATUS_PENDING;
        }
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
        return STATUS_RUNNING;
    }

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

    private CdpWarehousePrivacyErasureAssetProofDO findProof(Long tenantId, Long requestId, String assetKey) {
        List<CdpWarehousePrivacyErasureAssetProofDO> rows = proofMapper.selectList(
                new LambdaQueryWrapper<CdpWarehousePrivacyErasureAssetProofDO>()
                        .eq(CdpWarehousePrivacyErasureAssetProofDO::getTenantId, tenantId)
                        .eq(CdpWarehousePrivacyErasureAssetProofDO::getRequestId, requestId)
                        .eq(CdpWarehousePrivacyErasureAssetProofDO::getAssetKey, assetKey)
                        .last("LIMIT 1"));
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    private List<CdpWarehousePrivacyErasureAssetProofDO> proofs(Long requestId) {
        return safeProofs(proofMapper.selectList(
                new LambdaQueryWrapper<CdpWarehousePrivacyErasureAssetProofDO>()
                        .eq(CdpWarehousePrivacyErasureAssetProofDO::getRequestId, requestId)
                        .orderByAsc(CdpWarehousePrivacyErasureAssetProofDO::getId)));
    }

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
                safeProofs(proofs).stream().map(this::toProofView).toList(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private AssetProofView toProofView(CdpWarehousePrivacyErasureAssetProofDO row) {
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
                row.getExecutedBy(),
                row.getExecutedAt(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private List<String> targetAssets(List<String> assets) {
        Set<String> normalized = new LinkedHashSet<>();
        List<String> input = assets == null || assets.isEmpty() ? DEFAULT_ASSETS : assets;
        for (String asset : input) {
            if (hasText(asset)) {
                normalized.add(asset.trim().toUpperCase(Locale.ROOT));
            }
        }
        if (normalized.isEmpty()) {
            normalized.addAll(DEFAULT_ASSETS);
        }
        return List.copyOf(normalized);
    }

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

    private String mask(String subjectValue) {
        String value = required(subjectValue, "subjectValue");
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    private String assetLayer(String assetKey) {
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
        return "CDP";
    }

    private String plannedAction(String assetKey) {
        return "prove erasure propagation for " + assetKey;
    }

    private String proofStatus(String status) {
        String normalized = normalizeStatus(status);
        if (STATUS_PASS.equals(normalized) || STATUS_WARN.equals(normalized)
                || STATUS_FAIL.equals(normalized) || STATUS_SKIPPED.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("asset proof status must be PASS, WARN, FAIL, or SKIPPED");
    }

    private String normalizeStatus(String status) {
        if (!hasText(status)) {
            return STATUS_FAIL;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isActiveStatus(String status) {
        String normalized = normalizeStatus(status);
        return STATUS_PENDING.equals(normalized) || STATUS_RUNNING.equals(normalized) || STATUS_WARN.equals(normalized);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String upperRequired(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    private String upperDefault(String value, String fallback) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String limit(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_TEXT_LENGTH ? value : value.substring(0, MAX_TEXT_LENGTH);
    }

    private String sanitizeProofText(String value, CdpWarehousePrivacyErasureRequestDO request) {
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
        return rawSubjectToken.matcher(sanitized).replaceAll("[REDACTED_SUBJECT]");
    }

    private long nonNegative(Long value) {
        return value == null || value < 0 ? 0L : value;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private int boundLimit(int value) {
        int limit = value <= 0 ? 20 : value;
        return Math.min(limit, MAX_LIMIT);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize privacy erasure proof", e);
        }
    }

    private List<CdpWarehousePrivacyErasureRequestDO> safeRequests(
            List<CdpWarehousePrivacyErasureRequestDO> rows) {
        return rows == null ? List.of() : rows;
    }

    private List<CdpWarehousePrivacyErasureAssetProofDO> safeProofs(
            List<CdpWarehousePrivacyErasureAssetProofDO> rows) {
        return rows == null ? List.of() : rows;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public record ErasureRequestCommand(
            String requestKey,
            String subjectType,
            String subjectValue,
            String reason,
            String requestedBy,
            LocalDateTime dueAt,
            List<String> targetAssets) {
    }

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

    public record RequestEvidence(
            String key,
            String status,
            String reason) {
    }
}
