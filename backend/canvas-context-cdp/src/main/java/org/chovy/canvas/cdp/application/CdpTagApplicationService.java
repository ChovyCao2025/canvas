package org.chovy.canvas.cdp.application;

import org.chovy.canvas.cdp.api.CdpTagFacade;
import org.chovy.canvas.cdp.api.CdpTagWriteCommand;
import org.chovy.canvas.cdp.api.CdpUserTagHistoryView;
import org.chovy.canvas.cdp.api.CdpUserTagView;
import org.chovy.canvas.cdp.domain.CdpTagDefinition;
import org.chovy.canvas.cdp.domain.CdpTagRepository;
import org.chovy.canvas.cdp.domain.CdpTagValueType;
import org.chovy.canvas.cdp.domain.CdpUserTag;
import org.chovy.canvas.cdp.domain.CdpUserTagHistory;
import org.chovy.canvas.cdp.domain.CustomerProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CdpTagApplicationService implements CdpTagFacade {

    private final CdpTagRepository tagRepository;
    private final CustomerProfileLookupApplicationService profileLookup;
    private final Clock clock;

    @Autowired
    public CdpTagApplicationService(CdpTagRepository tagRepository, CustomerProfileRepository profileRepository) {
        this(tagRepository, profileRepository, Clock.systemDefaultZone());
    }

    CdpTagApplicationService(CdpTagRepository tagRepository,
                             CustomerProfileRepository profileRepository,
                             Clock clock) {
        this.tagRepository = tagRepository;
        this.profileLookup = new CustomerProfileLookupApplicationService(profileRepository, clock);
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdpUserTagView setTag(Long tenantId, String userId, CdpTagWriteCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("tag command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedUserId = requireText(userId, "userId");
        String tagCode = requireText(command.tagCode(), "tagCode");
        CdpTagDefinition definition = tagRepository.findEnabledDefinition(tagCode);
        if (definition == null || !definition.enabled()) {
            throw new IllegalArgumentException("tag not found or disabled: " + tagCode);
        }
        String sourceType = normalizeSourceType(command.sourceType());
        if ("MANUAL".equals(sourceType) && !definition.manualEnabled()) {
            throw new IllegalArgumentException("manual tagging is disabled for tag: " + tagCode);
        }
        String normalizedValue = CdpTagValueType.from(definition.valueType()).normalize(command.tagValue());
        CdpUserTag existing = tagRepository.findCurrentTag(scopedTenantId, normalizedUserId, tagCode);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = command.expiresAt();
        if (expiresAt == null && definition.defaultTtlDays() != null) {
            expiresAt = now.plusDays(definition.defaultTtlDays());
        }
        boolean reserved = tagRepository.saveHistory(new CdpUserTagHistory(
                scopedTenantId,
                normalizedUserId,
                tagCode,
                existing == null ? null : existing.tagValue(),
                normalizedValue,
                "SET",
                sourceType,
                command.sourceRefId(),
                command.idempotencyKey(),
                command.reason(),
                command.operator(),
                now));
        if (!reserved) {
            return toTagView(existing == null
                    ? tagRepository.findCurrentTag(scopedTenantId, normalizedUserId, tagCode)
                    : existing);
        }
        profileLookup.ensureUser(scopedTenantId, normalizedUserId, sourceType, command.sourceRefId());
        CdpUserTag saved = tagRepository.saveCurrentTag(new CdpUserTag(
                existing == null ? null : existing.id(),
                scopedTenantId,
                normalizedUserId,
                tagCode,
                normalizedValue,
                CdpTagValueType.from(definition.valueType()).name(),
                sourceType,
                command.sourceRefId(),
                "ACTIVE",
                now,
                expiresAt,
                command.operator(),
                existing == null ? null : existing.createdAt(),
                now));
        return toTagView(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTag(Long tenantId, String userId, String tagCode, String reason, String operator) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedUserId = requireText(userId, "userId");
        String normalizedTagCode = requireText(tagCode, "tagCode");
        CdpUserTag existing = tagRepository.findCurrentTag(scopedTenantId, normalizedUserId, normalizedTagCode);
        if (existing == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        tagRepository.saveCurrentTag(existing.withStatus("REMOVED"));
        tagRepository.saveHistory(new CdpUserTagHistory(
                scopedTenantId,
                normalizedUserId,
                normalizedTagCode,
                existing.tagValue(),
                null,
                "REMOVE",
                "MANUAL",
                null,
                null,
                reason,
                operator,
                now));
    }

    @Override
    public List<CdpUserTagView> listCurrentTags(Long tenantId, String userId) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedUserId = requireText(userId, "userId");
        return tagRepository.listCurrentTags(scopedTenantId, normalizedUserId).stream()
                .map(this::toTagView)
                .toList();
    }

    @Override
    public List<CdpUserTagHistoryView> listHistory(Long tenantId, String userId) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedUserId = requireText(userId, "userId");
        return tagRepository.listHistory(scopedTenantId, normalizedUserId).stream()
                .map(this::toHistoryView)
                .toList();
    }

    private CdpUserTagView toTagView(CdpUserTag tag) {
        if (tag == null) {
            return null;
        }
        return new CdpUserTagView(
                tag.id(),
                tag.tenantId(),
                tag.userId(),
                tag.tagCode(),
                tag.tagValue(),
                tag.valueType(),
                tag.sourceType(),
                tag.status(),
                tag.effectiveAt(),
                tag.expiresAt(),
                tag.updatedAt());
    }

    private CdpUserTagHistoryView toHistoryView(CdpUserTagHistory history) {
        return new CdpUserTagHistoryView(
                history.tenantId(),
                history.userId(),
                history.tagCode(),
                history.oldValue(),
                history.newValue(),
                history.operation(),
                history.sourceType(),
                history.sourceRefId(),
                history.reason(),
                history.operator(),
                history.operatedAt());
    }

    private static String normalizeSourceType(String sourceType) {
        return sourceType == null || sourceType.isBlank()
                ? "MANUAL"
                : sourceType.trim().toUpperCase(Locale.ROOT);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
