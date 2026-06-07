package org.chovy.canvas.domain.paidmedia;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.dataobject.PaidMediaAudienceDestinationDO;
import org.chovy.canvas.dal.dataobject.PaidMediaAudienceMemberDO;
import org.chovy.canvas.dal.dataobject.PaidMediaAudienceSyncRunDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.PaidMediaAudienceDestinationMapper;
import org.chovy.canvas.dal.mapper.PaidMediaAudienceMemberMapper;
import org.chovy.canvas.dal.mapper.PaidMediaAudienceSyncRunMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class PaidMediaAudienceSyncService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final PaidMediaAudienceDestinationMapper destinationMapper;
    private final PaidMediaAudienceMemberMapper memberMapper;
    private final PaidMediaAudienceSyncRunMapper runMapper;
    private final AudienceDefinitionMapper audienceMapper;
    private final CdpUserProfileMapper profileMapper;
    private final MarketingConsentMapper consentMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public PaidMediaAudienceSyncService(PaidMediaAudienceDestinationMapper destinationMapper,
                                        PaidMediaAudienceMemberMapper memberMapper,
                                        PaidMediaAudienceSyncRunMapper runMapper,
                                        AudienceDefinitionMapper audienceMapper,
                                        CdpUserProfileMapper profileMapper,
                                        MarketingConsentMapper consentMapper,
                                        ObjectMapper objectMapper) {
        this(destinationMapper, memberMapper, runMapper, audienceMapper, profileMapper, consentMapper,
                objectMapper, Clock.systemDefaultZone());
    }

    PaidMediaAudienceSyncService(PaidMediaAudienceDestinationMapper destinationMapper,
                                 PaidMediaAudienceMemberMapper memberMapper,
                                 PaidMediaAudienceSyncRunMapper runMapper,
                                 AudienceDefinitionMapper audienceMapper,
                                 CdpUserProfileMapper profileMapper,
                                 MarketingConsentMapper consentMapper,
                                 ObjectMapper objectMapper,
                                 Clock clock) {
        this.destinationMapper = destinationMapper;
        this.memberMapper = memberMapper;
        this.runMapper = runMapper;
        this.audienceMapper = audienceMapper;
        this.profileMapper = profileMapper;
        this.consentMapper = consentMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public PaidMediaAudienceDestinationView upsertDestination(Long tenantId,
                                                              PaidMediaDestinationCommand command,
                                                              String actor) {
        if (command == null) {
            throw new IllegalArgumentException("paid-media destination command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = normalizeUpper(command.provider(), "provider");
        String destinationKey = required(command.destinationKey(), "destinationKey");
        LocalDateTime changedAt = now();
        PaidMediaAudienceDestinationDO row = destinationMapper.selectOne(
                new LambdaQueryWrapper<PaidMediaAudienceDestinationDO>()
                        .eq(PaidMediaAudienceDestinationDO::getTenantId, scopedTenantId)
                        .eq(PaidMediaAudienceDestinationDO::getProvider, provider)
                        .eq(PaidMediaAudienceDestinationDO::getDestinationKey, destinationKey)
                        .last("LIMIT 1"));
        if (row == null) {
            row = new PaidMediaAudienceDestinationDO();
            row.setTenantId(scopedTenantId);
            row.setProvider(provider);
            row.setDestinationKey(destinationKey);
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(changedAt);
        }
        row.setDisplayName(defaultString(command.displayName(), destinationKey));
        row.setAccountId(trimToNull(command.accountId()));
        row.setExternalAudienceId(trimToNull(command.externalAudienceId()));
        row.setIdentifierTypesJson(json(normalizeIdentifierTypes(command.identifierTypes())));
        row.setConsentChannel(normalizeText(command.consentChannel(), "PAID_MEDIA").toUpperCase(Locale.ROOT));
        row.setEnforceConsent(Boolean.FALSE.equals(command.enforceConsent()) ? 0 : 1);
        row.setEnabled(Boolean.FALSE.equals(command.enabled()) ? 0 : 1);
        row.setMetadataJson(json(command.metadata()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            destinationMapper.insert(row);
        } else {
            destinationMapper.updateById(row);
        }
        return toDestinationView(row);
    }

    public PaidMediaAudienceSyncRunView syncAudience(Long tenantId,
                                                     PaidMediaAudienceSyncCommand command,
                                                     String actor) {
        if (command == null) {
            throw new IllegalArgumentException("paid-media audience sync command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        List<String> userIds = distinctUsers(command.userIds());
        PaidMediaAudienceDestinationDO destination = destinationMapper.selectById(requiredId(
                command.destinationId(), "destinationId"));
        String provider = destination == null ? "UNKNOWN" : defaultString(destination.getProvider(), "UNKNOWN");
        LocalDateTime startedAt = now();
        PaidMediaAudienceSyncRunDO run = insertRunningRun(scopedTenantId, provider, command, userIds.size(), actor,
                startedAt);
        try {
            validateDestination(scopedTenantId, destination);
            AudienceDefinitionDO audience = audienceMapper.selectById(requiredId(command.audienceId(), "audienceId"));
            validateAudience(scopedTenantId, audience);
            SyncCounters counters = processUsers(scopedTenantId, destination, command.audienceId(), run.getId(), userIds,
                    startedAt);
            completeRun(run, "SUCCESS", counters.eligibleUsers(), counters.skippedUsers(), 0,
                    command.externalOperationId(), null, now());
            return toRunView(run);
        } catch (RuntimeException ex) {
            completeRun(run, "FAILED", 0, 0, Math.max(1, userIds.size()), command.externalOperationId(),
                    ex.getMessage(), now());
            throw ex;
        }
    }

    public List<PaidMediaAudienceSyncRunView> runs(Long tenantId, PaidMediaAudienceRunQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("paid-media run query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        int limit = boundedLimit(query.limit());
        String status = normalizeOptionalUpper(query.status());
        return safeList(runMapper.selectList(
                new LambdaQueryWrapper<PaidMediaAudienceSyncRunDO>()
                        .eq(PaidMediaAudienceSyncRunDO::getTenantId, scopedTenantId)
                        .eq(query.destinationId() != null, PaidMediaAudienceSyncRunDO::getDestinationId,
                                query.destinationId())
                        .eq(query.audienceId() != null, PaidMediaAudienceSyncRunDO::getAudienceId, query.audienceId())
                        .eq(status != null, PaidMediaAudienceSyncRunDO::getStatus, status)
                        .orderByDesc(PaidMediaAudienceSyncRunDO::getCreatedAt)
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> query.destinationId() == null || Objects.equals(query.destinationId(), row.getDestinationId()))
                .filter(row -> query.audienceId() == null || Objects.equals(query.audienceId(), row.getAudienceId()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toRunView)
                .toList();
    }

    public List<PaidMediaAudienceMemberView> members(Long tenantId, PaidMediaAudienceMemberQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("paid-media member query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        int limit = boundedLimit(query.limit());
        String status = normalizeOptionalUpper(query.eligibilityStatus());
        return safeList(memberMapper.selectList(
                new LambdaQueryWrapper<PaidMediaAudienceMemberDO>()
                        .eq(PaidMediaAudienceMemberDO::getTenantId, scopedTenantId)
                        .eq(query.runId() != null, PaidMediaAudienceMemberDO::getRunId, query.runId())
                        .eq(status != null, PaidMediaAudienceMemberDO::getEligibilityStatus, status)
                        .orderByDesc(PaidMediaAudienceMemberDO::getCreatedAt)
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> query.runId() == null || Objects.equals(query.runId(), row.getRunId()))
                .filter(row -> status == null || status.equals(row.getEligibilityStatus()))
                .limit(limit)
                .map(this::toMemberView)
                .toList();
    }

    private PaidMediaAudienceSyncRunDO insertRunningRun(Long tenantId,
                                                       String provider,
                                                       PaidMediaAudienceSyncCommand command,
                                                       int requestedCount,
                                                       String actor,
                                                       LocalDateTime startedAt) {
        PaidMediaAudienceSyncRunDO row = new PaidMediaAudienceSyncRunDO();
        row.setTenantId(tenantId);
        row.setDestinationId(command.destinationId());
        row.setAudienceId(command.audienceId());
        row.setProvider(defaultString(provider, "UNKNOWN").toUpperCase(Locale.ROOT));
        row.setStatus("RUNNING");
        row.setRequestedCount(requestedCount);
        row.setEligibleCount(0);
        row.setSkippedCount(0);
        row.setFailedCount(0);
        row.setExternalOperationId(command.externalOperationId());
        row.setMetadataJson(json(command.metadata()));
        row.setCreatedBy(defaultString(actor, "system"));
        row.setStartedAt(startedAt);
        row.setCreatedAt(startedAt);
        row.setUpdatedAt(startedAt);
        runMapper.insert(row);
        return row;
    }

    private void completeRun(PaidMediaAudienceSyncRunDO run,
                             String status,
                             int eligibleCount,
                             int skippedCount,
                             int failedCount,
                             String externalOperationId,
                             String errorMessage,
                             LocalDateTime finishedAt) {
        run.setStatus(status);
        run.setEligibleCount(eligibleCount);
        run.setSkippedCount(skippedCount);
        run.setFailedCount(failedCount);
        run.setExternalOperationId(externalOperationId);
        run.setErrorMessage(errorMessage);
        run.setFinishedAt(finishedAt);
        run.setUpdatedAt(finishedAt);
        runMapper.updateById(run);
    }

    private SyncCounters processUsers(Long tenantId,
                                      PaidMediaAudienceDestinationDO destination,
                                      Long audienceId,
                                      Long runId,
                                      List<String> userIds,
                                      LocalDateTime syncedAt) {
        int eligibleUsers = 0;
        int skippedUsers = 0;
        List<String> identifierTypes = list(destination.getIdentifierTypesJson());
        for (String userId : userIds) {
            CdpUserProfileDO profile = profileMapper.selectOne(
                    new LambdaQueryWrapper<CdpUserProfileDO>()
                            .eq(CdpUserProfileDO::getTenantId, tenantId)
                            .eq(CdpUserProfileDO::getUserId, userId)
                            .last("LIMIT 1"));
            if (profile == null || !tenantId.equals(profile.getTenantId())) {
                insertSkippedMember(tenantId, destination, audienceId, runId, userId, "PROFILE_NOT_FOUND", syncedAt);
                skippedUsers++;
                continue;
            }
            if (enabled(destination.getEnforceConsent()) && !hasConsent(tenantId, userId,
                    destination.getConsentChannel())) {
                insertSkippedMember(tenantId, destination, audienceId, runId, userId, "CONSENT_DENIED", syncedAt);
                skippedUsers++;
                continue;
            }
            List<Identifier> identifiers = identifiers(profile, identifierTypes);
            if (identifiers.isEmpty()) {
                insertSkippedMember(tenantId, destination, audienceId, runId, userId, "MISSING_IDENTIFIER", syncedAt);
                skippedUsers++;
                continue;
            }
            for (Identifier identifier : identifiers) {
                insertMember(tenantId, destination, audienceId, runId, userId, identifier.type(),
                        sha256(identifier.value()), "ELIGIBLE", null, syncedAt);
            }
            eligibleUsers++;
        }
        return new SyncCounters(eligibleUsers, skippedUsers);
    }

    private boolean hasConsent(Long tenantId, String userId, String consentChannel) {
        MarketingConsentDO row = consentMapper.selectOne(
                new LambdaQueryWrapper<MarketingConsentDO>()
                        .eq(MarketingConsentDO::getTenantId, tenantId)
                        .eq(MarketingConsentDO::getUserId, userId)
                        .eq(MarketingConsentDO::getChannel, consentChannel)
                        .last("LIMIT 1"));
        return row != null
                && tenantId.equals(row.getTenantId())
                && MarketingConsentDO.OPT_IN.equals(row.getConsentStatus());
    }

    private void insertSkippedMember(Long tenantId,
                                     PaidMediaAudienceDestinationDO destination,
                                     Long audienceId,
                                     Long runId,
                                     String userId,
                                     String reason,
                                     LocalDateTime syncedAt) {
        insertMember(tenantId, destination, audienceId, runId, userId, "UNKNOWN", null, "SKIPPED", reason, syncedAt);
    }

    private void insertMember(Long tenantId,
                              PaidMediaAudienceDestinationDO destination,
                              Long audienceId,
                              Long runId,
                              String userId,
                              String identifierType,
                              String identifierHash,
                              String eligibilityStatus,
                              String reason,
                              LocalDateTime syncedAt) {
        PaidMediaAudienceMemberDO row = new PaidMediaAudienceMemberDO();
        row.setTenantId(tenantId);
        row.setRunId(runId);
        row.setDestinationId(destination.getId());
        row.setAudienceId(audienceId);
        row.setProvider(destination.getProvider());
        row.setUserId(userId);
        row.setIdentifierType(identifierType);
        row.setIdentifierHash(identifierHash);
        row.setEligibilityStatus(eligibilityStatus);
        row.setReason(reason);
        row.setSyncedAt(syncedAt);
        row.setCreatedAt(syncedAt);
        row.setUpdatedAt(syncedAt);
        memberMapper.insert(row);
    }

    private List<Identifier> identifiers(CdpUserProfileDO profile, List<String> identifierTypes) {
        List<Identifier> identifiers = new ArrayList<>();
        for (String type : identifierTypes) {
            if ("EMAIL".equals(type)) {
                String email = normalizeEmail(profile.getEmail());
                if (email != null) {
                    identifiers.add(new Identifier(type, email));
                }
            } else if ("PHONE".equals(type)) {
                String phone = normalizePhone(profile.getPhone());
                if (phone != null) {
                    identifiers.add(new Identifier(type, phone));
                }
            }
        }
        return identifiers;
    }

    private void validateDestination(Long tenantId, PaidMediaAudienceDestinationDO destination) {
        if (destination == null || !tenantId.equals(destination.getTenantId())) {
            throw new IllegalArgumentException("paid-media destination is not found");
        }
        if (!enabled(destination.getEnabled())) {
            throw new IllegalStateException("paid-media destination is disabled");
        }
    }

    private void validateAudience(Long tenantId, AudienceDefinitionDO audience) {
        if (audience == null || !tenantId.equals(audience.getTenantId())) {
            throw new IllegalArgumentException("audience is not found");
        }
        if (!enabled(audience.getEnabled())) {
            throw new IllegalStateException("audience is disabled");
        }
    }

    private PaidMediaAudienceDestinationView toDestinationView(PaidMediaAudienceDestinationDO row) {
        return new PaidMediaAudienceDestinationView(
                row.getId(),
                row.getTenantId(),
                row.getProvider(),
                row.getDestinationKey(),
                row.getDisplayName(),
                row.getAccountId(),
                row.getExternalAudienceId(),
                list(row.getIdentifierTypesJson()),
                row.getConsentChannel(),
                enabled(row.getEnforceConsent()),
                enabled(row.getEnabled()),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private PaidMediaAudienceSyncRunView toRunView(PaidMediaAudienceSyncRunDO row) {
        return new PaidMediaAudienceSyncRunView(
                row.getId(),
                row.getTenantId(),
                row.getDestinationId(),
                row.getAudienceId(),
                row.getProvider(),
                row.getStatus(),
                defaultInt(row.getRequestedCount()),
                defaultInt(row.getEligibleCount()),
                defaultInt(row.getSkippedCount()),
                defaultInt(row.getFailedCount()),
                row.getExternalOperationId(),
                row.getErrorMessage(),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getStartedAt(),
                row.getFinishedAt());
    }

    private PaidMediaAudienceMemberView toMemberView(PaidMediaAudienceMemberDO row) {
        return new PaidMediaAudienceMemberView(
                row.getId(),
                row.getTenantId(),
                row.getRunId(),
                row.getDestinationId(),
                row.getAudienceId(),
                row.getProvider(),
                row.getUserId(),
                row.getIdentifierType(),
                row.getIdentifierHash(),
                row.getEligibilityStatus(),
                row.getReason(),
                row.getSyncedAt());
    }

    private List<String> normalizeIdentifierTypes(List<String> values) {
        Set<String> types = new LinkedHashSet<>();
        for (String value : values == null ? List.<String>of() : values) {
            if (hasText(value)) {
                types.add(value.trim().toUpperCase(Locale.ROOT));
            }
        }
        return types.isEmpty() ? List.of("EMAIL", "PHONE") : List.copyOf(types);
    }

    private List<String> distinctUsers(List<String> userIds) {
        Set<String> users = new LinkedHashSet<>();
        for (String userId : userIds == null ? List.<String>of() : userIds) {
            if (hasText(userId)) {
                users.add(userId.trim());
            }
        }
        return List.copyOf(users);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("paid-media audience sync JSON serialization failed", ex);
        }
    }

    private List<String> list(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private Map<String, Object> map(String json) {
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String normalizeEmail(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String normalizePhone(String value) {
        if (!hasText(value)) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private boolean enabled(Integer value) {
        return value == null || value == 1;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private Long requiredId(Long value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String normalizeUpper(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private record Identifier(String type, String value) {
    }

    private record SyncCounters(int eligibleUsers, int skippedUsers) {
    }
}
