package org.chovy.canvas.marketing.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.chovy.canvas.marketing.api.PaidMediaFacade.DestinationCommand;
import org.chovy.canvas.marketing.api.PaidMediaFacade.DestinationView;
import org.chovy.canvas.marketing.api.PaidMediaFacade.MemberQuery;
import org.chovy.canvas.marketing.api.PaidMediaFacade.MemberView;
import org.chovy.canvas.marketing.api.PaidMediaFacade.RunQuery;
import org.chovy.canvas.marketing.api.PaidMediaFacade.SyncCommand;
import org.chovy.canvas.marketing.api.PaidMediaFacade.SyncRunView;

public class PaidMediaCatalog {

    private final Clock clock;
    private final Map<DestinationKey, DestinationRow> destinationsByKey = new LinkedHashMap<>();
    private final Map<Long, DestinationRow> destinationsById = new LinkedHashMap<>();
    private final Map<TenantAudienceKey, Boolean> audiences = new LinkedHashMap<>();
    private final Map<TenantUserKey, ProfileRow> profiles = new LinkedHashMap<>();
    private final Set<ConsentKey> consents = new LinkedHashSet<>();
    private final List<RunRow> runs = new ArrayList<>();
    private final List<MemberRow> members = new ArrayList<>();
    private long destinationIds;
    private long runIds;
    private long memberIds;

    public PaidMediaCatalog(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public synchronized DestinationView upsertDestination(Long tenantId, DestinationCommand command, String actor) {
        DestinationCommand safe = command == null
                ? new DestinationCommand(null, null, null, null, null, List.of(), null, null, null, Map.of())
                : command;
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = requireText(safe.provider(), "provider is required").toUpperCase();
        String destinationKey = requireText(safe.destinationKey(), "destination key is required");
        String displayName = defaultText(safe.displayName(), destinationKey);
        String consentChannel = defaultText(safe.consentChannel(), "PAID_MEDIA").toUpperCase();
        List<String> identifierTypes = identifierTypes(safe.identifierTypes());
        boolean enforceConsent = safe.enforceConsent() == null || safe.enforceConsent();
        boolean enabled = safe.enabled() == null || safe.enabled();
        Map<String, Object> metadata = safe.metadata() == null ? Map.of() : Map.copyOf(safe.metadata());
        LocalDateTime now = now();
        DestinationKey key = new DestinationKey(scopedTenantId, provider, destinationKey);
        DestinationRow row = destinationsByKey.get(key);
        if (row == null) {
            row = new DestinationRow(++destinationIds, scopedTenantId, provider, destinationKey,
                    displayName, safe.accountId(), safe.externalAudienceId(), identifierTypes, consentChannel,
                    enforceConsent, enabled, metadata, actorOrSystem(actor), now, now);
            destinationsByKey.put(key, row);
            destinationsById.put(row.id, row);
            return view(row);
        }
        row.displayName = displayName;
        row.accountId = safe.accountId();
        row.externalAudienceId = safe.externalAudienceId();
        row.identifierTypes = identifierTypes;
        row.consentChannel = consentChannel;
        row.enforceConsent = enforceConsent;
        row.enabled = enabled;
        row.metadata = metadata;
        row.updatedAt = now;
        return view(row);
    }

    public synchronized SyncRunView syncAudience(Long tenantId, SyncCommand command, String actor) {
        SyncCommand safe = command == null ? new SyncCommand(null, null, List.of(), null, Map.of()) : command;
        Long scopedTenantId = normalizeTenant(tenantId);
        DestinationRow destination = destinationsById.get(safe.destinationId());
        if (destination == null || !Objects.equals(destination.tenantId, scopedTenantId)) {
            throw new IllegalArgumentException("paid-media destination is not found");
        }
        if (safe.audienceId() == null) {
            throw new IllegalArgumentException("audience id is required");
        }
        Map<String, Object> metadata = safe.metadata() == null ? Map.of() : Map.copyOf(safe.metadata());
        List<String> requestedUsers = distinctUsers(safe.userIds());
        LocalDateTime now = now();
        List<MemberRow> runMembers = new ArrayList<>();
        int eligibleCount = 0;
        int skippedCount = 0;
        for (String userId : requestedUsers) {
            ProfileRow profile = profiles.get(new TenantUserKey(scopedTenantId, userId));
            String reason = skipReason(destination, scopedTenantId, safe.audienceId(), userId, profile);
            String status = reason == null ? "ELIGIBLE" : "SKIPPED";
            if ("ELIGIBLE".equals(status)) {
                eligibleCount++;
            } else {
                skippedCount++;
            }
            runMembers.add(new MemberRow(++memberIds, scopedTenantId, runIds + 1, destination.id,
                    safe.audienceId(), destination.provider, userId, firstIdentifierType(destination, profile),
                    identifierHash(profile), status, reason, now));
        }
        RunRow run = new RunRow(++runIds, scopedTenantId, destination.id, safe.audienceId(), destination.provider,
                "SUCCESS", requestedUsers.size(), eligibleCount, skippedCount, 0, safe.externalOperationId(), null,
                metadata, actorOrSystem(actor), now, now);
        runs.add(run);
        members.addAll(runMembers);
        return view(run);
    }

    public synchronized List<SyncRunView> runs(Long tenantId, RunQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        RunQuery safe = query == null ? new RunQuery(null, null, null, 50) : query;
        return runs.stream()
                .filter(row -> Objects.equals(row.tenantId, scopedTenantId))
                .filter(row -> safe.destinationId() == null || Objects.equals(row.destinationId, safe.destinationId()))
                .filter(row -> safe.audienceId() == null || Objects.equals(row.audienceId, safe.audienceId()))
                .filter(row -> statusMatches(row.status, safe.status()))
                .sorted(Comparator.comparing((RunRow row) -> row.id).reversed())
                .limit(boundedLimit(safe.limit()))
                .map(PaidMediaCatalog::view)
                .toList();
    }

    public synchronized List<MemberView> members(Long tenantId, MemberQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        MemberQuery safe = query == null ? new MemberQuery(null, null, 50) : query;
        return members.stream()
                .filter(row -> Objects.equals(row.tenantId, scopedTenantId))
                .filter(row -> safe.runId() == null || Objects.equals(row.runId, safe.runId()))
                .filter(row -> statusMatches(row.status, safe.status()))
                .sorted(Comparator.comparing(row -> row.id))
                .limit(boundedLimit(safe.limit()))
                .map(PaidMediaCatalog::view)
                .toList();
    }

    public synchronized void registerAudience(Long tenantId, Long audienceId, boolean active) {
        if (audienceId != null) {
            audiences.put(new TenantAudienceKey(normalizeTenant(tenantId), audienceId), active);
        }
    }

    public synchronized void registerProfile(Long tenantId, String userId, String email, String phone) {
        String scopedUserId = requireText(userId, "profile user id is required");
        profiles.put(new TenantUserKey(normalizeTenant(tenantId), scopedUserId), new ProfileRow(email, phone));
    }

    public synchronized void grantConsent(Long tenantId, String userId, String channel) {
        consents.add(new ConsentKey(normalizeTenant(tenantId), requireText(userId, "consent user id is required"),
                defaultText(channel, "PAID_MEDIA").toUpperCase()));
    }

    private String skipReason(DestinationRow destination, Long tenantId, Long audienceId, String userId,
            ProfileRow profile) {
        if (profile == null) {
            return "PROFILE_NOT_FOUND";
        }
        if (Boolean.FALSE.equals(audiences.getOrDefault(new TenantAudienceKey(tenantId, audienceId), true))) {
            return "AUDIENCE_INACTIVE";
        }
        if (destination.enforceConsent
                && !consents.contains(new ConsentKey(tenantId, userId, destination.consentChannel))) {
            return "CONSENT_DENIED";
        }
        return null;
    }

    private static List<String> identifierTypes(List<String> values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    normalized.add(value.trim().toUpperCase());
                }
            }
        }
        if (normalized.isEmpty()) {
            normalized.add("EMAIL");
            normalized.add("PHONE");
        }
        return List.copyOf(normalized);
    }

    private static List<String> distinctUsers(List<String> userIds) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (userIds != null) {
            for (String userId : userIds) {
                if (userId != null && !userId.isBlank()) {
                    normalized.add(userId.trim());
                }
            }
        }
        return List.copyOf(normalized);
    }

    private static String firstIdentifierType(DestinationRow destination, ProfileRow profile) {
        if (destination.identifierTypes.isEmpty()) {
            return "EMAIL";
        }
        if (profile == null) {
            return destination.identifierTypes.get(0);
        }
        for (String type : destination.identifierTypes) {
            if ("EMAIL".equals(type) && profile.email != null && !profile.email.isBlank()) {
                return "EMAIL";
            }
            if ("PHONE".equals(type) && profile.phone != null && !profile.phone.isBlank()) {
                return "PHONE";
            }
        }
        return destination.identifierTypes.get(0);
    }

    private static String identifierHash(ProfileRow profile) {
        if (profile == null) {
            return null;
        }
        String identifier = profile.email != null && !profile.email.isBlank() ? profile.email : profile.phone;
        return identifier == null ? null : Integer.toHexString(identifier.trim().toLowerCase().hashCode());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private static boolean statusMatches(String actual, String expected) {
        return expected == null || expected.isBlank() || actual.equalsIgnoreCase(expected);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String actorOrSystem(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static DestinationView view(DestinationRow row) {
        return new DestinationView(row.id, row.tenantId, row.provider, row.destinationKey, row.displayName,
                row.accountId, row.externalAudienceId, row.identifierTypes, row.consentChannel, row.enforceConsent,
                row.enabled, row.metadata, row.createdBy, row.createdAt, row.updatedAt);
    }

    private static SyncRunView view(RunRow row) {
        return new SyncRunView(row.id, row.tenantId, row.destinationId, row.audienceId, row.provider, row.status,
                row.requestedCount, row.eligibleCount, row.skippedCount, row.failedCount, row.externalOperationId,
                row.failureReason, row.metadata, row.createdBy, row.createdAt, row.completedAt);
    }

    private static MemberView view(MemberRow row) {
        return new MemberView(row.id, row.tenantId, row.runId, row.destinationId, row.audienceId, row.provider,
                row.userId, row.identifierType, row.identifierHash, row.status, row.reason, row.createdAt);
    }

    private record DestinationKey(Long tenantId, String provider, String destinationKey) {
    }

    private record TenantAudienceKey(Long tenantId, Long audienceId) {
    }

    private record TenantUserKey(Long tenantId, String userId) {
    }

    private record ConsentKey(Long tenantId, String userId, String channel) {
    }

    private record ProfileRow(String email, String phone) {
    }

    private static final class DestinationRow {
        private final Long id;
        private final Long tenantId;
        private final String provider;
        private final String destinationKey;
        private String displayName;
        private String accountId;
        private String externalAudienceId;
        private List<String> identifierTypes;
        private String consentChannel;
        private boolean enforceConsent;
        private boolean enabled;
        private Map<String, Object> metadata;
        private final String createdBy;
        private final LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private DestinationRow(Long id, Long tenantId, String provider, String destinationKey, String displayName,
                String accountId, String externalAudienceId, List<String> identifierTypes, String consentChannel,
                boolean enforceConsent, boolean enabled, Map<String, Object> metadata, String createdBy,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.tenantId = tenantId;
            this.provider = provider;
            this.destinationKey = destinationKey;
            this.displayName = displayName;
            this.accountId = accountId;
            this.externalAudienceId = externalAudienceId;
            this.identifierTypes = identifierTypes;
            this.consentChannel = consentChannel;
            this.enforceConsent = enforceConsent;
            this.enabled = enabled;
            this.metadata = metadata;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }

    private record RunRow(
            Long id,
            Long tenantId,
            Long destinationId,
            Long audienceId,
            String provider,
            String status,
            int requestedCount,
            int eligibleCount,
            int skippedCount,
            int failedCount,
            String externalOperationId,
            String failureReason,
            Map<String, Object> metadata,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime completedAt) {
    }

    private record MemberRow(
            Long id,
            Long tenantId,
            Long runId,
            Long destinationId,
            Long audienceId,
            String provider,
            String userId,
            String identifierType,
            String identifierHash,
            String status,
            String reason,
            LocalDateTime createdAt) {
    }
}
