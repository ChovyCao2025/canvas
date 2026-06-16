package org.chovy.canvas.marketing.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 定义PaidMediaFacade的营销上下文访问契约。
 */
public interface PaidMediaFacade {

    /**
     * 执行upsertDestination业务操作。
     */
    DestinationView upsertDestination(Long tenantId, DestinationCommand command, String actor);

    /**
     * 执行syncAudience业务操作。
     */
    SyncRunView syncAudience(Long tenantId, SyncCommand command, String actor);

    /**
     * 执行runs业务操作。
     */
    List<SyncRunView> runs(Long tenantId, RunQuery query);

    /**
     * 执行members业务操作。
     */
    List<MemberView> members(Long tenantId, MemberQuery query);

    /**
     * 执行registerAudience业务操作。
     */
    default void registerAudience(Long tenantId, Long audienceId, boolean active) {
    }

    /**
     * 执行registerProfile业务操作。
     */
    default void registerProfile(Long tenantId, String userId, String email, String phone) {
    }

    /**
     * 执行grantConsent业务操作。
     */
    default void grantConsent(Long tenantId, String userId, String channel) {
    }

    /**
     * 承载DestinationCommand调用所需的输入参数。
     */
    static final class DestinationCommand {

        /**
         * provider 字段值。
         */
        private final String provider;

        /**
         * destinationKey 字段值。
         */
        private final String destinationKey;

        /**
         * displayName 字段值。
         */
        private final String displayName;

        /**
         * accountId 字段值。
         */
        private final String accountId;

        /**
         * externalAudienceId 字段值。
         */
        private final String externalAudienceId;

        /**
         * identifierTypes 字段值。
         */
        private final List<String> identifierTypes;

        /**
         * consentChannel 字段值。
         */
        private final String consentChannel;

        /**
         * enforceConsent 字段值。
         */
        private final Boolean enforceConsent;

        /**
         * 是否启用。
         */
        private final Boolean enabled;

        /**
         * 扩展元数据。
         */
        private final Map<String, Object> metadata;

        /**
         * 创建DestinationCommand实例。
         */
        public DestinationCommand(String provider, String destinationKey, String displayName, String accountId, String externalAudienceId, List<String> identifierTypes, String consentChannel, Boolean enforceConsent, Boolean enabled, Map<String, Object> metadata) {
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
         * 返回displayName 字段值。
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回accountId 字段值。
         */
        public String accountId() {
            return accountId;
        }

        /**
         * 返回externalAudienceId 字段值。
         */
        public String externalAudienceId() {
            return externalAudienceId;
        }

        /**
         * 返回identifierTypes 字段值。
         */
        public List<String> identifierTypes() {
            return identifierTypes;
        }

        /**
         * 返回consentChannel 字段值。
         */
        public String consentChannel() {
            return consentChannel;
        }

        /**
         * 返回enforceConsent 字段值。
         */
        public Boolean enforceConsent() {
            return enforceConsent;
        }

        /**
         * 返回是否启用。
         */
        public Boolean enabled() {
            return enabled;
        }

        /**
         * 返回扩展元数据。
         */
        public Map<String, Object> metadata() {
            return metadata;
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
            DestinationCommand that = (DestinationCommand) o;
            return                     Objects.equals(provider, that.provider) &&
                    Objects.equals(destinationKey, that.destinationKey) &&
                    Objects.equals(displayName, that.displayName) &&
                    Objects.equals(accountId, that.accountId) &&
                    Objects.equals(externalAudienceId, that.externalAudienceId) &&
                    Objects.equals(identifierTypes, that.identifierTypes) &&
                    Objects.equals(consentChannel, that.consentChannel) &&
                    Objects.equals(enforceConsent, that.enforceConsent) &&
                    Objects.equals(enabled, that.enabled) &&
                    Objects.equals(metadata, that.metadata);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(provider, destinationKey, displayName, accountId, externalAudienceId, identifierTypes, consentChannel, enforceConsent, enabled, metadata);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "DestinationCommand[provider=" + provider + ", destinationKey=" + destinationKey + ", displayName=" + displayName + ", accountId=" + accountId + ", externalAudienceId=" + externalAudienceId + ", identifierTypes=" + identifierTypes + ", consentChannel=" + consentChannel + ", enforceConsent=" + enforceConsent + ", enabled=" + enabled + ", metadata=" + metadata + "]";
        }
    }

    /**
     * 承载SyncCommand调用所需的输入参数。
     */
    static final class SyncCommand {

        /**
         * destinationId 字段值。
         */
        private final Long destinationId;

        /**
         * audienceId 字段值。
         */
        private final Long audienceId;

        /**
         * userIds 字段值。
         */
        private final List<String> userIds;

        /**
         * externalOperationId 字段值。
         */
        private final String externalOperationId;

        /**
         * 扩展元数据。
         */
        private final Map<String, Object> metadata;

        /**
         * 创建SyncCommand实例。
         */
        public SyncCommand(Long destinationId, Long audienceId, List<String> userIds, String externalOperationId, Map<String, Object> metadata) {
            this.destinationId = destinationId;
            this.audienceId = audienceId;
            this.userIds = userIds;
            this.externalOperationId = externalOperationId;
            this.metadata = metadata;
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
         * 返回userIds 字段值。
         */
        public List<String> userIds() {
            return userIds;
        }

        /**
         * 返回externalOperationId 字段值。
         */
        public String externalOperationId() {
            return externalOperationId;
        }

        /**
         * 返回扩展元数据。
         */
        public Map<String, Object> metadata() {
            return metadata;
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
            SyncCommand that = (SyncCommand) o;
            return                     Objects.equals(destinationId, that.destinationId) &&
                    Objects.equals(audienceId, that.audienceId) &&
                    Objects.equals(userIds, that.userIds) &&
                    Objects.equals(externalOperationId, that.externalOperationId) &&
                    Objects.equals(metadata, that.metadata);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(destinationId, audienceId, userIds, externalOperationId, metadata);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "SyncCommand[destinationId=" + destinationId + ", audienceId=" + audienceId + ", userIds=" + userIds + ", externalOperationId=" + externalOperationId + ", metadata=" + metadata + "]";
        }
    }

    /**
     * 承载RunQuery查询条件。
     */
    static final class RunQuery {

        /**
         * destinationId 字段值。
         */
        private final Long destinationId;

        /**
         * audienceId 字段值。
         */
        private final Long audienceId;

        /**
         * 当前业务状态。
         */
        private final String status;

        /**
         * limit 字段值。
         */
        private final int limit;

        /**
         * 创建RunQuery实例。
         */
        public RunQuery(Long destinationId, Long audienceId, String status, int limit) {
            this.destinationId = destinationId;
            this.audienceId = audienceId;
            this.status = status;
            this.limit = limit;
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
         * 返回当前业务状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回limit 字段值。
         */
        public int limit() {
            return limit;
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
            RunQuery that = (RunQuery) o;
            return                     Objects.equals(destinationId, that.destinationId) &&
                    Objects.equals(audienceId, that.audienceId) &&
                    Objects.equals(status, that.status) &&
                    limit == that.limit;
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(destinationId, audienceId, status, limit);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "RunQuery[destinationId=" + destinationId + ", audienceId=" + audienceId + ", status=" + status + ", limit=" + limit + "]";
        }
    }

    /**
     * 承载MemberQuery查询条件。
     */
    static final class MemberQuery {

        /**
         * runId 字段值。
         */
        private final Long runId;

        /**
         * 当前业务状态。
         */
        private final String status;

        /**
         * limit 字段值。
         */
        private final int limit;

        /**
         * 创建MemberQuery实例。
         */
        public MemberQuery(Long runId, String status, int limit) {
            this.runId = runId;
            this.status = status;
            this.limit = limit;
        }

        /**
         * 返回runId 字段值。
         */
        public Long runId() {
            return runId;
        }

        /**
         * 返回当前业务状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回limit 字段值。
         */
        public int limit() {
            return limit;
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
            MemberQuery that = (MemberQuery) o;
            return                     Objects.equals(runId, that.runId) &&
                    Objects.equals(status, that.status) &&
                    limit == that.limit;
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(runId, status, limit);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "MemberQuery[runId=" + runId + ", status=" + status + ", limit=" + limit + "]";
        }
    }

    /**
     * 承载DestinationView返回给调用方的只读视图。
     */
    static final class DestinationView {

        /**
         * 记录的唯一标识。
         */
        private final Long id;

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
         * displayName 字段值。
         */
        private final String displayName;

        /**
         * accountId 字段值。
         */
        private final String accountId;

        /**
         * externalAudienceId 字段值。
         */
        private final String externalAudienceId;

        /**
         * identifierTypes 字段值。
         */
        private final List<String> identifierTypes;

        /**
         * consentChannel 字段值。
         */
        private final String consentChannel;

        /**
         * enforceConsent 字段值。
         */
        private final boolean enforceConsent;

        /**
         * 是否启用。
         */
        private final boolean enabled;

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
         * 最后更新时间。
         */
        private final LocalDateTime updatedAt;

        /**
         * 创建DestinationView实例。
         */
        public DestinationView(Long id, Long tenantId, String provider, String destinationKey, String displayName, String accountId, String externalAudienceId, List<String> identifierTypes, String consentChannel, boolean enforceConsent, boolean enabled, Map<String, Object> metadata, String createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
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
         * 返回displayName 字段值。
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回accountId 字段值。
         */
        public String accountId() {
            return accountId;
        }

        /**
         * 返回externalAudienceId 字段值。
         */
        public String externalAudienceId() {
            return externalAudienceId;
        }

        /**
         * 返回identifierTypes 字段值。
         */
        public List<String> identifierTypes() {
            return identifierTypes;
        }

        /**
         * 返回consentChannel 字段值。
         */
        public String consentChannel() {
            return consentChannel;
        }

        /**
         * 返回enforceConsent 字段值。
         */
        public boolean enforceConsent() {
            return enforceConsent;
        }

        /**
         * 返回是否启用。
         */
        public boolean enabled() {
            return enabled;
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
         * 返回最后更新时间。
         */
        public LocalDateTime updatedAt() {
            return updatedAt;
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
            DestinationView that = (DestinationView) o;
            return                     Objects.equals(id, that.id) &&
                    Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(provider, that.provider) &&
                    Objects.equals(destinationKey, that.destinationKey) &&
                    Objects.equals(displayName, that.displayName) &&
                    Objects.equals(accountId, that.accountId) &&
                    Objects.equals(externalAudienceId, that.externalAudienceId) &&
                    Objects.equals(identifierTypes, that.identifierTypes) &&
                    Objects.equals(consentChannel, that.consentChannel) &&
                    enforceConsent == that.enforceConsent &&
                    enabled == that.enabled &&
                    Objects.equals(metadata, that.metadata) &&
                    Objects.equals(createdBy, that.createdBy) &&
                    Objects.equals(createdAt, that.createdAt) &&
                    Objects.equals(updatedAt, that.updatedAt);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(id, tenantId, provider, destinationKey, displayName, accountId, externalAudienceId, identifierTypes, consentChannel, enforceConsent, enabled, metadata, createdBy, createdAt, updatedAt);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "DestinationView[id=" + id + ", tenantId=" + tenantId + ", provider=" + provider + ", destinationKey=" + destinationKey + ", displayName=" + displayName + ", accountId=" + accountId + ", externalAudienceId=" + externalAudienceId + ", identifierTypes=" + identifierTypes + ", consentChannel=" + consentChannel + ", enforceConsent=" + enforceConsent + ", enabled=" + enabled + ", metadata=" + metadata + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
        }
    }

    /**
     * 承载SyncRunView返回给调用方的只读视图。
     */
    static final class SyncRunView {

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
         * 创建SyncRunView实例。
         */
        public SyncRunView(Long id, Long tenantId, Long destinationId, Long audienceId, String provider, String status, int requestedCount, int eligibleCount, int skippedCount, int failedCount, String externalOperationId, String failureReason, Map<String, Object> metadata, String createdBy, LocalDateTime createdAt, LocalDateTime completedAt) {
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
            SyncRunView that = (SyncRunView) o;
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
            return "SyncRunView[id=" + id + ", tenantId=" + tenantId + ", destinationId=" + destinationId + ", audienceId=" + audienceId + ", provider=" + provider + ", status=" + status + ", requestedCount=" + requestedCount + ", eligibleCount=" + eligibleCount + ", skippedCount=" + skippedCount + ", failedCount=" + failedCount + ", externalOperationId=" + externalOperationId + ", failureReason=" + failureReason + ", metadata=" + metadata + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", completedAt=" + completedAt + "]";
        }
    }

    /**
     * 承载MemberView返回给调用方的只读视图。
     */
    static final class MemberView {

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
         * 创建MemberView实例。
         */
        public MemberView(Long id, Long tenantId, Long runId, Long destinationId, Long audienceId, String provider, String userId, String identifierType, String identifierHash, String status, String reason, LocalDateTime createdAt) {
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
            MemberView that = (MemberView) o;
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
            return "MemberView[id=" + id + ", tenantId=" + tenantId + ", runId=" + runId + ", destinationId=" + destinationId + ", audienceId=" + audienceId + ", provider=" + provider + ", userId=" + userId + ", identifierType=" + identifierType + ", identifierHash=" + identifierHash + ", status=" + status + ", reason=" + reason + ", createdAt=" + createdAt + "]";
        }
    }
}
