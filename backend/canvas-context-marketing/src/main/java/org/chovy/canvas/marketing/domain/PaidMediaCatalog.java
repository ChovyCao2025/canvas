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

/**
 * 维护PaidMedia相关的内存业务目录。
 */
public class PaidMediaCatalog {

    /**
     * 用于生成确定性业务时间的时钟。
     */
    private final Clock clock;
    private final Map<DestinationKey, DestinationRow> destinationsByKey = new LinkedHashMap<>();
    private final Map<Long, DestinationRow> destinationsById = new LinkedHashMap<>();
    private final Map<TenantAudienceKey, Boolean> audiences = new LinkedHashMap<>();
    private final Map<TenantUserKey, ProfileRow> profiles = new LinkedHashMap<>();
    private final Set<ConsentKey> consents = new LinkedHashSet<>();
    private final List<RunRow> runs = new ArrayList<>();
    private final List<MemberRow> members = new ArrayList<>();

    /**
     * 下一个投放目的地内存标识。
     */
    private long destinationIds;

    /**
     * 下一个同步运行内存标识。
     */
    private long runIds;

    /**
     * 下一个同步成员内存标识。
     */
    private long memberIds;

    /**
     * 创建PaidMediaCatalog实例。
     */
    public PaidMediaCatalog(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行upsertDestination业务操作。
     */
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

    /**
     * 执行syncAudience业务操作。
     */
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
        // 逐个用户固化本次同步的资格判定，确保运行汇总与成员明细使用同一批输入。
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

    /**
     * 执行runs业务操作。
     */
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

    /**
     * 执行members业务操作。
     */
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

    /**
     * 执行registerAudience业务操作。
     */
    public synchronized void registerAudience(Long tenantId, Long audienceId, boolean active) {
        if (audienceId != null) {
            audiences.put(new TenantAudienceKey(normalizeTenant(tenantId), audienceId), active);
        }
    }

    /**
     * 执行registerProfile业务操作。
     */
    public synchronized void registerProfile(Long tenantId, String userId, String email, String phone) {
        String scopedUserId = requireText(userId, "profile user id is required");
        profiles.put(new TenantUserKey(normalizeTenant(tenantId), scopedUserId), new ProfileRow(email, phone));
    }

    /**
     * 执行grantConsent业务操作。
     */
    public synchronized void grantConsent(Long tenantId, String userId, String channel) {
        consents.add(new ConsentKey(normalizeTenant(tenantId), requireText(userId, "consent user id is required"),
                defaultText(channel, "PAID_MEDIA").toUpperCase()));
    }

    /**
     * 执行skipReason业务操作。
     */
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

    /**
     * 执行identifierTypes业务操作。
     */
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

    /**
     * 执行distinctUsers业务操作。
     */
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

    /**
     * 执行firstIdentifierType业务操作。
     */
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

    /**
     * 执行identifierHash业务操作。
     */
    private static String identifierHash(ProfileRow profile) {
        if (profile == null) {
            return null;
        }
        String identifier = profile.email != null && !profile.email.isBlank() ? profile.email : profile.phone;
        return identifier == null ? null : Integer.toHexString(identifier.trim().toLowerCase().hashCode());
    }

    /**
     * 执行now业务操作。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 规范化tenant输入值。
     */
    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 执行boundedLimit业务操作。
     */
    private static int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    /**
     * 执行statusMatches业务操作。
     */
    private static boolean statusMatches(String actual, String expected) {
        return expected == null || expected.isBlank() || actual.equalsIgnoreCase(expected);
    }

    /**
     * 校验并返回text必填值。
     */
    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 执行defaultText业务操作。
     */
    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 执行actorOrSystem业务操作。
     */
    private static String actorOrSystem(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 执行view业务操作。
     */
    private static DestinationView view(DestinationRow row) {
        return new DestinationView(row.id, row.tenantId, row.provider, row.destinationKey, row.displayName,
                row.accountId, row.externalAudienceId, row.identifierTypes, row.consentChannel, row.enforceConsent,
                row.enabled, row.metadata, row.createdBy, row.createdAt, row.updatedAt);
    }

    /**
     * 执行view业务操作。
     */
    private static SyncRunView view(RunRow row) {
        return new SyncRunView(row.id, row.tenantId, row.destinationId, row.audienceId, row.provider, row.status,
                row.requestedCount, row.eligibleCount, row.skippedCount, row.failedCount, row.externalOperationId,
                row.failureReason, row.metadata, row.createdBy, row.createdAt, row.completedAt);
    }

    /**
     * 执行view业务操作。
     */
    private static MemberView view(MemberRow row) {
        return new MemberView(row.id, row.tenantId, row.runId, row.destinationId, row.audienceId, row.provider,
                row.userId, row.identifierType, row.identifierHash, row.status, row.reason, row.createdAt);
    }

    /**
     * 表示DestinationKey使用的稳定匹配键。
     */
    private static final class DestinationKey {

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * provider 字段值。
         */
        private final String provider;

        /**
         * destinationKey 字段值。
         */
        private final String destinationKey;

        /**
         * 创建DestinationKey实例。
         */
        public DestinationKey(Long tenantId, String provider, String destinationKey) {
            this.tenantId = tenantId;
            this.provider = provider;
            this.destinationKey = destinationKey;
        }

        /**
         * 返回所属租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回provider 字段值。
         */
        public String provider() {
            return provider;
        }

        /**
         * 返回destinationKey 字段值。
         */
        public String destinationKey() {
            return destinationKey;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DestinationKey that = (DestinationKey) o;
            return                     Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(provider, that.provider) &&
                    Objects.equals(destinationKey, that.destinationKey);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(tenantId, provider, destinationKey);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "DestinationKey[tenantId=" + tenantId + ", provider=" + provider + ", destinationKey=" + destinationKey + "]";
        }
    }

    /**
     * 表示TenantAudienceKey使用的稳定匹配键。
     */
    private static final class TenantAudienceKey {

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * audienceId 字段值。
         */
        private final Long audienceId;

        /**
         * 创建TenantAudienceKey实例。
         */
        public TenantAudienceKey(Long tenantId, Long audienceId) {
            this.tenantId = tenantId;
            this.audienceId = audienceId;
        }

        /**
         * 返回所属租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回audienceId 字段值。
         */
        public Long audienceId() {
            return audienceId;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TenantAudienceKey that = (TenantAudienceKey) o;
            return                     Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(audienceId, that.audienceId);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(tenantId, audienceId);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "TenantAudienceKey[tenantId=" + tenantId + ", audienceId=" + audienceId + "]";
        }
    }

    /**
     * 表示TenantUserKey使用的稳定匹配键。
     */
    private static final class TenantUserKey {

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * 创建TenantUserKey实例。
         */
        public TenantUserKey(Long tenantId, String userId) {
            this.tenantId = tenantId;
            this.userId = userId;
        }

        /**
         * 返回所属租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回用户标识。
         */
        public String userId() {
            return userId;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TenantUserKey that = (TenantUserKey) o;
            return                     Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(userId, that.userId);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(tenantId, userId);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "TenantUserKey[tenantId=" + tenantId + ", userId=" + userId + "]";
        }
    }

    /**
     * 表示ConsentKey使用的稳定匹配键。
     */
    private static final class ConsentKey {

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * 渠道标识。
         */
        private final String channel;

        /**
         * 创建ConsentKey实例。
         */
        public ConsentKey(Long tenantId, String userId, String channel) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.channel = channel;
        }

        /**
         * 返回所属租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回用户标识。
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回渠道标识。
         */
        public String channel() {
            return channel;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ConsentKey that = (ConsentKey) o;
            return                     Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(userId, that.userId) &&
                    Objects.equals(channel, that.channel);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(tenantId, userId, channel);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "ConsentKey[tenantId=" + tenantId + ", userId=" + userId + ", channel=" + channel + "]";
        }
    }

    /**
     * 保存ProfileRow的内存行数据。
     */
    private static final class ProfileRow {

        /**
         * email 字段值。
         */
        private final String email;

        /**
         * phone 字段值。
         */
        private final String phone;

        /**
         * 创建ProfileRow实例。
         */
        public ProfileRow(String email, String phone) {
            this.email = email;
            this.phone = phone;
        }

        /**
         * 返回email 字段值。
         */
        public String email() {
            return email;
        }

        /**
         * 返回phone 字段值。
         */
        public String phone() {
            return phone;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ProfileRow that = (ProfileRow) o;
            return                     Objects.equals(email, that.email) &&
                    Objects.equals(phone, that.phone);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(email, phone);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "ProfileRow[email=" + email + ", phone=" + phone + "]";
        }
    }

    /**
     * 提供DestinationRow的业务能力。
     */
    private static final class DestinationRow {
        /**
         * 保存id字段值。
         */
        private final Long id;

        /**
         * 保存tenantId字段值。
         */
        private final Long tenantId;

        /**
         * 保存provider字段值。
         */
        private final String provider;

        /**
         * 保存destinationKey字段值。
         */
        private final String destinationKey;

        /**
         * 保存displayName字段值。
         */
        private String displayName;

        /**
         * 保存accountId字段值。
         */
        private String accountId;

        /**
         * 保存externalAudienceId字段值。
         */
        private String externalAudienceId;

        /**
         * 保存identifierTypes字段值。
         */
        private List<String> identifierTypes;

        /**
         * 保存consentChannel字段值。
         */
        private String consentChannel;

        /**
         * 保存enforceConsent字段值。
         */
        private boolean enforceConsent;

        /**
         * 保存enabled字段值。
         */
        private boolean enabled;

        /**
         * 保存metadata字段值。
         */
        private Map<String, Object> metadata;

        /**
         * 保存createdBy字段值。
         */
        private final String createdBy;

        /**
         * 保存createdAt字段值。
         */
        private final LocalDateTime createdAt;

        /**
         * 保存updatedAt字段值。
         */
        private LocalDateTime updatedAt;

        /**
         * 创建DestinationRow实例。
         */
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

    /**
     * 保存RunRow的内存行数据。
     */
    private static final class RunRow {

        /**
         * 记录的唯一标识。
         */
        private final Long id;

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * destinationId 字段值。
         */
        private final Long destinationId;

        /**
         * audienceId 字段值。
         */
        private final Long audienceId;

        /**
         * provider 字段值。
         */
        private final String provider;

        /**
         * 当前业务状态。
         */
        private final String status;

        /**
         * requestedCount 字段值。
         */
        private final int requestedCount;

        /**
         * eligibleCount 字段值。
         */
        private final int eligibleCount;

        /**
         * skippedCount 字段值。
         */
        private final int skippedCount;

        /**
         * failedCount 字段值。
         */
        private final int failedCount;

        /**
         * externalOperationId 字段值。
         */
        private final String externalOperationId;

        /**
         * failureReason 字段值。
         */
        private final String failureReason;

        /**
         * 扩展元数据。
         */
        private final Map<String, Object> metadata;

        /**
         * 创建人标识。
         */
        private final String createdBy;

        /**
         * 创建时间。
         */
        private final LocalDateTime createdAt;

        /**
         * completedAt 字段值。
         */
        private final LocalDateTime completedAt;

        /**
         * 创建RunRow实例。
         */
        public RunRow(Long id, Long tenantId, Long destinationId, Long audienceId, String provider, String status, int requestedCount, int eligibleCount, int skippedCount, int failedCount, String externalOperationId, String failureReason, Map<String, Object> metadata, String createdBy, LocalDateTime createdAt, LocalDateTime completedAt) {
            this.id = id;
            this.tenantId = tenantId;
            this.destinationId = destinationId;
            this.audienceId = audienceId;
            this.provider = provider;
            this.status = status;
            this.requestedCount = requestedCount;
            this.eligibleCount = eligibleCount;
            this.skippedCount = skippedCount;
            this.failedCount = failedCount;
            this.externalOperationId = externalOperationId;
            this.failureReason = failureReason;
            this.metadata = metadata;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
            this.completedAt = completedAt;
        }

        /**
         * 返回记录的唯一标识。
         */
        public Long id() {
            return id;
        }

        /**
         * 返回所属租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回destinationId 字段值。
         */
        public Long destinationId() {
            return destinationId;
        }

        /**
         * 返回audienceId 字段值。
         */
        public Long audienceId() {
            return audienceId;
        }

        /**
         * 返回provider 字段值。
         */
        public String provider() {
            return provider;
        }

        /**
         * 返回当前业务状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回requestedCount 字段值。
         */
        public int requestedCount() {
            return requestedCount;
        }

        /**
         * 返回eligibleCount 字段值。
         */
        public int eligibleCount() {
            return eligibleCount;
        }

        /**
         * 返回skippedCount 字段值。
         */
        public int skippedCount() {
            return skippedCount;
        }

        /**
         * 返回failedCount 字段值。
         */
        public int failedCount() {
            return failedCount;
        }

        /**
         * 返回externalOperationId 字段值。
         */
        public String externalOperationId() {
            return externalOperationId;
        }

        /**
         * 返回failureReason 字段值。
         */
        public String failureReason() {
            return failureReason;
        }

        /**
         * 返回扩展元数据。
         */
        public Map<String, Object> metadata() {
            return metadata;
        }

        /**
         * 返回创建人标识。
         */
        public String createdBy() {
            return createdBy;
        }

        /**
         * 返回创建时间。
         */
        public LocalDateTime createdAt() {
            return createdAt;
        }

        /**
         * 返回completedAt 字段值。
         */
        public LocalDateTime completedAt() {
            return completedAt;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RunRow that = (RunRow) o;
            return                     Objects.equals(id, that.id) &&
                    Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(destinationId, that.destinationId) &&
                    Objects.equals(audienceId, that.audienceId) &&
                    Objects.equals(provider, that.provider) &&
                    Objects.equals(status, that.status) &&
                    requestedCount == that.requestedCount &&
                    eligibleCount == that.eligibleCount &&
                    skippedCount == that.skippedCount &&
                    failedCount == that.failedCount &&
                    Objects.equals(externalOperationId, that.externalOperationId) &&
                    Objects.equals(failureReason, that.failureReason) &&
                    Objects.equals(metadata, that.metadata) &&
                    Objects.equals(createdBy, that.createdBy) &&
                    Objects.equals(createdAt, that.createdAt) &&
                    Objects.equals(completedAt, that.completedAt);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(id, tenantId, destinationId, audienceId, provider, status, requestedCount, eligibleCount, skippedCount, failedCount, externalOperationId, failureReason, metadata, createdBy, createdAt, completedAt);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "RunRow[id=" + id + ", tenantId=" + tenantId + ", destinationId=" + destinationId + ", audienceId=" + audienceId + ", provider=" + provider + ", status=" + status + ", requestedCount=" + requestedCount + ", eligibleCount=" + eligibleCount + ", skippedCount=" + skippedCount + ", failedCount=" + failedCount + ", externalOperationId=" + externalOperationId + ", failureReason=" + failureReason + ", metadata=" + metadata + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", completedAt=" + completedAt + "]";
        }
    }

    /**
     * 保存MemberRow的内存行数据。
     */
    private static final class MemberRow {

        /**
         * 记录的唯一标识。
         */
        private final Long id;

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * runId 字段值。
         */
        private final Long runId;

        /**
         * destinationId 字段值。
         */
        private final Long destinationId;

        /**
         * audienceId 字段值。
         */
        private final Long audienceId;

        /**
         * provider 字段值。
         */
        private final String provider;

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * identifierType 字段值。
         */
        private final String identifierType;

        /**
         * identifierHash 字段值。
         */
        private final String identifierHash;

        /**
         * 当前业务状态。
         */
        private final String status;

        /**
         * 问题原因。
         */
        private final String reason;

        /**
         * 创建时间。
         */
        private final LocalDateTime createdAt;

        /**
         * 创建MemberRow实例。
         */
        public MemberRow(Long id, Long tenantId, Long runId, Long destinationId, Long audienceId, String provider, String userId, String identifierType, String identifierHash, String status, String reason, LocalDateTime createdAt) {
            this.id = id;
            this.tenantId = tenantId;
            this.runId = runId;
            this.destinationId = destinationId;
            this.audienceId = audienceId;
            this.provider = provider;
            this.userId = userId;
            this.identifierType = identifierType;
            this.identifierHash = identifierHash;
            this.status = status;
            this.reason = reason;
            this.createdAt = createdAt;
        }

        /**
         * 返回记录的唯一标识。
         */
        public Long id() {
            return id;
        }

        /**
         * 返回所属租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回runId 字段值。
         */
        public Long runId() {
            return runId;
        }

        /**
         * 返回destinationId 字段值。
         */
        public Long destinationId() {
            return destinationId;
        }

        /**
         * 返回audienceId 字段值。
         */
        public Long audienceId() {
            return audienceId;
        }

        /**
         * 返回provider 字段值。
         */
        public String provider() {
            return provider;
        }

        /**
         * 返回用户标识。
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回identifierType 字段值。
         */
        public String identifierType() {
            return identifierType;
        }

        /**
         * 返回identifierHash 字段值。
         */
        public String identifierHash() {
            return identifierHash;
        }

        /**
         * 返回当前业务状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回问题原因。
         */
        public String reason() {
            return reason;
        }

        /**
         * 返回创建时间。
         */
        public LocalDateTime createdAt() {
            return createdAt;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MemberRow that = (MemberRow) o;
            return                     Objects.equals(id, that.id) &&
                    Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(runId, that.runId) &&
                    Objects.equals(destinationId, that.destinationId) &&
                    Objects.equals(audienceId, that.audienceId) &&
                    Objects.equals(provider, that.provider) &&
                    Objects.equals(userId, that.userId) &&
                    Objects.equals(identifierType, that.identifierType) &&
                    Objects.equals(identifierHash, that.identifierHash) &&
                    Objects.equals(status, that.status) &&
                    Objects.equals(reason, that.reason) &&
                    Objects.equals(createdAt, that.createdAt);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(id, tenantId, runId, destinationId, audienceId, provider, userId, identifierType, identifierHash, status, reason, createdAt);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "MemberRow[id=" + id + ", tenantId=" + tenantId + ", runId=" + runId + ", destinationId=" + destinationId + ", audienceId=" + audienceId + ", provider=" + provider + ", userId=" + userId + ", identifierType=" + identifierType + ", identifierHash=" + identifierHash + ", status=" + status + ", reason=" + reason + ", createdAt=" + createdAt + "]";
        }
    }
}
