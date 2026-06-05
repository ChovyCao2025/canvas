package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacyErasureRequestDO;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacySubjectTombstoneDO;
import org.chovy.canvas.dal.mapper.CdpWarehousePrivacyErasureRequestMapper;
import org.chovy.canvas.dal.mapper.CdpWarehousePrivacySubjectTombstoneMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class CdpWarehousePrivacyTombstoneService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String ERASURE_STATUS_PASS = "PASS";
    private static final int MAX_LIMIT = 100;
    private static final int MAX_TEXT_LENGTH = 1000;

    private final CdpWarehousePrivacySubjectTombstoneMapper tombstoneMapper;
    private final CdpWarehousePrivacyErasureRequestMapper erasureRequestMapper;
    private final Clock clock;

    public CdpWarehousePrivacyTombstoneService(CdpWarehousePrivacySubjectTombstoneMapper tombstoneMapper) {
        this(tombstoneMapper, null, Clock.systemDefaultZone());
    }

    @Autowired
    public CdpWarehousePrivacyTombstoneService(
            CdpWarehousePrivacySubjectTombstoneMapper tombstoneMapper,
            CdpWarehousePrivacyErasureRequestMapper erasureRequestMapper) {
        this(tombstoneMapper, erasureRequestMapper, Clock.systemDefaultZone());
    }

    CdpWarehousePrivacyTombstoneService(CdpWarehousePrivacySubjectTombstoneMapper tombstoneMapper,
                                        Clock clock) {
        this(tombstoneMapper, null, clock);
    }

    CdpWarehousePrivacyTombstoneService(CdpWarehousePrivacySubjectTombstoneMapper tombstoneMapper,
                                        CdpWarehousePrivacyErasureRequestMapper erasureRequestMapper,
                                        Clock clock) {
        this.tombstoneMapper = tombstoneMapper;
        this.erasureRequestMapper = erasureRequestMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public TombstoneView create(Long tenantId, TombstoneCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("tombstone command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String subjectType = upperDefault(command.subjectType(), "USER_ID");
        String subjectValue = required(command.subjectValue(), "subjectValue");
        String hash = subjectHash(scopedTenantId, subjectType, subjectValue);
        CdpWarehousePrivacySubjectTombstoneDO row = findByHash(scopedTenantId, subjectType, hash);
        if (row == null) {
            row = new CdpWarehousePrivacySubjectTombstoneDO();
            row.setTenantId(scopedTenantId);
            row.setSubjectType(subjectType);
            row.setSubjectHash(hash);
            row.setSubjectRefMasked(mask(subjectValue));
            row.setBlockedEventCount(0L);
        }
        row.setStatus(STATUS_ACTIVE);
        row.setSourceRequestId(command.sourceRequestId());
        row.setSourceRequestKey(blankToNull(command.sourceRequestKey()));
        row.setReason(limit(required(command.reason(), "reason")));
        row.setCreatedBy(defaultString(command.createdBy(), "system"));
        row.setRevokedBy(null);
        row.setRevokedAt(null);
        if (row.getId() == null) {
            tombstoneMapper.insert(row);
        } else {
            tombstoneMapper.updateById(row);
        }
        return toView(row);
    }

    public TombstoneView createFromErasureRequest(Long tenantId, ErasureRequestTombstoneCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("erasure request tombstone command is required");
        }
        if (command.requestId() == null || command.requestId() <= 0) {
            throw new IllegalArgumentException("requestId must be positive");
        }
        if (erasureRequestMapper == null) {
            throw new IllegalStateException("privacy erasure request mapper is not configured");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehousePrivacyErasureRequestDO request = erasureRequestMapper.selectById(command.requestId());
        if (request == null || !scopedTenantId.equals(request.getTenantId())) {
            throw new IllegalArgumentException("privacy erasure request not found: " + command.requestId());
        }
        if (!ERASURE_STATUS_PASS.equals(normalizeStatus(request.getStatus()))) {
            throw new IllegalArgumentException("privacy erasure request must be PASS before tombstone creation");
        }
        String subjectType = upperDefault(request.getSubjectType(), "USER_ID");
        String subjectHash = required(request.getSubjectHash(), "subjectHash");
        String subjectRefMasked = required(request.getSubjectRefMasked(), "subjectRefMasked");

        CdpWarehousePrivacySubjectTombstoneDO row = findByHash(scopedTenantId, subjectType, subjectHash);
        if (row == null) {
            row = new CdpWarehousePrivacySubjectTombstoneDO();
            row.setTenantId(scopedTenantId);
            row.setSubjectType(subjectType);
            row.setSubjectHash(subjectHash);
            row.setSubjectRefMasked(subjectRefMasked);
            row.setBlockedEventCount(0L);
        }
        row.setStatus(STATUS_ACTIVE);
        row.setSourceRequestId(request.getId());
        row.setSourceRequestKey(request.getRequestKey());
        row.setReason(limit(defaultString(command.reason(), request.getReason())));
        row.setCreatedBy(defaultString(command.createdBy(), defaultString(request.getRequestedBy(), "system")));
        row.setRevokedBy(null);
        row.setRevokedAt(null);
        if (row.getId() == null) {
            tombstoneMapper.insert(row);
        } else {
            tombstoneMapper.updateById(row);
        }
        return toView(row);
    }

    public TombstoneView revoke(Long tenantId, Long id, RevokeCommand command) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        CdpWarehousePrivacySubjectTombstoneDO row = tombstoneMapper.selectById(id);
        if (row == null || !normalizeTenant(tenantId).equals(row.getTenantId())) {
            throw new IllegalArgumentException("privacy tombstone not found: " + id);
        }
        row.setStatus(STATUS_REVOKED);
        row.setRevokedBy(defaultString(command == null ? null : command.revokedBy(), "system"));
        row.setRevokedAt(now());
        tombstoneMapper.updateById(row);
        return toView(row);
    }

    public TombstoneDecision decide(Long tenantId, String subjectType, String subjectValue) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String normalizedType = upperDefault(subjectType, "USER_ID");
        String value = required(subjectValue, "subjectValue");
        String hash = subjectHash(scopedTenantId, normalizedType, value);
        CdpWarehousePrivacySubjectTombstoneDO row = findByHash(scopedTenantId, normalizedType, hash);
        boolean blocked = row != null && STATUS_ACTIVE.equals(normalizeStatus(row.getStatus()));
        return new TombstoneDecision(
                scopedTenantId,
                normalizedType,
                hash,
                mask(value),
                blocked,
                row == null ? null : row.getId(),
                row == null ? null : row.getSourceRequestKey(),
                blocked ? "subject is blocked by active privacy tombstone" : "subject is not tombstoned");
    }

    public TombstoneDecision enforceNotBlocked(Long tenantId,
                                               String subjectType,
                                               String subjectValue,
                                               String source) {
        TombstoneDecision decision = decide(tenantId, subjectType, subjectValue);
        if (!decision.blocked()) {
            return decision;
        }
        tombstoneMapper.recordBlocked(decision.tenantId(), decision.subjectType(),
                decision.subjectHash(), now());
        throw new PrivacyTombstoneViolationException(
                "privacy tombstone blocks " + defaultString(source, "subject") + ": " + decision.subjectRefMasked());
    }

    public List<TombstoneView> list(Long tenantId, String status, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehousePrivacySubjectTombstoneDO> query =
                new LambdaQueryWrapper<CdpWarehousePrivacySubjectTombstoneDO>()
                        .eq(CdpWarehousePrivacySubjectTombstoneDO::getTenantId, scopedTenantId)
                        .orderByDesc(CdpWarehousePrivacySubjectTombstoneDO::getId)
                        .last("LIMIT " + boundLimit(limit));
        if (hasText(status)) {
            query.eq(CdpWarehousePrivacySubjectTombstoneDO::getStatus,
                    status.trim().toUpperCase(Locale.ROOT));
        }
        List<CdpWarehousePrivacySubjectTombstoneDO> rows = tombstoneMapper.selectList(query);
        return rows == null ? List.of() : rows.stream().map(this::toView).toList();
    }

    private CdpWarehousePrivacySubjectTombstoneDO findByHash(Long tenantId,
                                                             String subjectType,
                                                             String subjectHash) {
        List<CdpWarehousePrivacySubjectTombstoneDO> rows = tombstoneMapper.selectList(
                new LambdaQueryWrapper<CdpWarehousePrivacySubjectTombstoneDO>()
                        .eq(CdpWarehousePrivacySubjectTombstoneDO::getTenantId, tenantId)
                        .eq(CdpWarehousePrivacySubjectTombstoneDO::getSubjectType, subjectType)
                        .eq(CdpWarehousePrivacySubjectTombstoneDO::getSubjectHash, subjectHash)
                        .last("LIMIT 1"));
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    private TombstoneView toView(CdpWarehousePrivacySubjectTombstoneDO row) {
        return new TombstoneView(
                row.getId(),
                row.getTenantId(),
                row.getSubjectType(),
                row.getSubjectHash(),
                row.getSubjectRefMasked(),
                row.getStatus(),
                row.getSourceRequestId(),
                row.getSourceRequestKey(),
                row.getReason(),
                row.getBlockedEventCount() == null ? 0L : row.getBlockedEventCount(),
                row.getLastBlockedAt(),
                row.getCreatedBy(),
                row.getRevokedBy(),
                row.getRevokedAt(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private String subjectHash(Long tenantId, String subjectType, String subjectValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((tenantId + ":" + subjectType + ":" + subjectValue)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("failed to hash privacy tombstone subject", e);
        }
    }

    private String mask(String subjectValue) {
        String value = required(subjectValue, "subjectValue");
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeStatus(String status) {
        return hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : STATUS_REVOKED;
    }

    private String upperDefault(String value, String fallback) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    private String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
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

    private int boundLimit(int value) {
        int limit = value <= 0 ? 20 : value;
        return Math.min(limit, MAX_LIMIT);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public record TombstoneCommand(
            String subjectType,
            String subjectValue,
            Long sourceRequestId,
            String sourceRequestKey,
            String reason,
            String createdBy) {
    }

    public record RevokeCommand(String revokedBy) {
    }

    public record ErasureRequestTombstoneCommand(Long requestId, String reason, String createdBy) {
    }

    public record TombstoneDecision(
            Long tenantId,
            String subjectType,
            String subjectHash,
            String subjectRefMasked,
            boolean blocked,
            Long tombstoneId,
            String sourceRequestKey,
            String reason) {
    }

    public record TombstoneView(
            Long id,
            Long tenantId,
            String subjectType,
            String subjectHash,
            String subjectRefMasked,
            String status,
            Long sourceRequestId,
            String sourceRequestKey,
            String reason,
            long blockedEventCount,
            LocalDateTime lastBlockedAt,
            String createdBy,
            String revokedBy,
            LocalDateTime revokedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public static class PrivacyTombstoneViolationException extends IllegalArgumentException {
        public PrivacyTombstoneViolationException(String message) {
            super(message);
        }
    }
}
