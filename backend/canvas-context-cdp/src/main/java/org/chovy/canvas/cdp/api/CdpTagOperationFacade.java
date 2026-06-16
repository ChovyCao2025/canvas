package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 定义 CdpTagOperationFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpTagOperationFacade {

    /**
     * 创建create。
     */
    TagOperationView create(Long tenantId, BatchTagCommand command, String actor);

    /**
     * 查询Recent列表。
     */
    List<TagOperationView> listRecent(Long tenantId, int limit);

    /**
     * 返回get。
     */
    TagOperationView get(Long tenantId, Long id);

    /**
     * 执行 retryFailed 对应的 CDP 业务操作。
     */
    TagOperationView retryFailed(Long tenantId, Long id, String actor);

    /**
     * 表示 BatchTagCommand 的业务数据或处理组件。
     */
    final class BatchTagCommand {

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * 标签编码。
         */
        private final String tagCode;

        /**
         * 标签值。
         */
        private final String tagValue;

        /**
         * 成员标识列表。
         */
        private final List<String> memberIds;

        /**
         * metadata。
         */
        private final Map<String, Object> metadata;

        /**
         * 使用记录字段创建 BatchTagCommand。
         */
        public BatchTagCommand(
                String userId,
                String tagCode,
                String tagValue,
                List<String> memberIds,
                Map<String, Object> metadata) {
            this.userId = userId;
            this.tagCode = tagCode;
            this.tagValue = tagValue;
            this.memberIds = memberIds;
            this.metadata = metadata;
        }

        /**
         * 返回用户标识。
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回标签编码。
         */
        public String tagCode() {
            return tagCode;
        }

        /**
         * 返回标签值。
         */
        public String tagValue() {
            return tagValue;
        }

        /**
         * 返回成员标识列表。
         */
        public List<String> memberIds() {
            return memberIds;
        }

        /**
         * 返回metadata。
         */
        public Map<String, Object> metadata() {
            return metadata;
        }

        /**
         * 按所有字段比较 BatchTagCommand。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BatchTagCommand that = (BatchTagCommand) o;
            return java.util.Objects.equals(userId, that.userId)
                    && java.util.Objects.equals(tagCode, that.tagCode)
                    && java.util.Objects.equals(tagValue, that.tagValue)
                    && java.util.Objects.equals(memberIds, that.memberIds)
                    && java.util.Objects.equals(metadata, that.metadata);
        }

        /**
         * 根据所有字段计算 BatchTagCommand 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, tagCode, tagValue, memberIds, metadata);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "BatchTagCommand[" + "userId=" + userId + ", tagCode=" + tagCode + ", tagValue=" + tagValue + ", memberIds=" + memberIds + ", metadata=" + metadata + "]";
        }
    }

    /**
     * 表示 TagOperationView 的业务数据或处理组件。
     */
    final class TagOperationView {

        /**
         * 唯一标识。
         */
        private final Long id;

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * 标签编码。
         */
        private final String tagCode;

        /**
         * 标签值。
         */
        private final String tagValue;

        /**
         * 成员标识列表。
         */
        private final List<String> memberIds;

        /**
         * metadata。
         */
        private final Map<String, Object> metadata;

        /**
         * 状态。
         */
        private final String status;

        /**
         * affected Count。
         */
        private final int affectedCount;

        /**
         * 创建人。
         */
        private final String createdBy;

        /**
         * updated By。
         */
        private final String updatedBy;

        /**
         * 创建时间。
         */
        private final LocalDateTime createdAt;

        /**
         * 更新时间。
         */
        private final LocalDateTime updatedAt;

        /**
         * 使用记录字段创建 TagOperationView。
         */
        public TagOperationView(
                Long id,
                Long tenantId,
                String userId,
                String tagCode,
                String tagValue,
                List<String> memberIds,
                Map<String, Object> metadata,
                String status,
                int affectedCount,
                String createdBy,
                String updatedBy,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
            this.id = id;
            this.tenantId = tenantId;
            this.userId = userId;
            this.tagCode = tagCode;
            this.tagValue = tagValue;
            this.memberIds = memberIds;
            this.metadata = metadata;
            this.status = status;
            this.affectedCount = affectedCount;
            this.createdBy = createdBy;
            this.updatedBy = updatedBy;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        /**
         * 返回唯一标识。
         */
        public Long id() {
            return id;
        }

        /**
         * 返回租户标识。
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
         * 返回标签编码。
         */
        public String tagCode() {
            return tagCode;
        }

        /**
         * 返回标签值。
         */
        public String tagValue() {
            return tagValue;
        }

        /**
         * 返回成员标识列表。
         */
        public List<String> memberIds() {
            return memberIds;
        }

        /**
         * 返回metadata。
         */
        public Map<String, Object> metadata() {
            return metadata;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回affected Count。
         */
        public int affectedCount() {
            return affectedCount;
        }

        /**
         * 返回创建人。
         */
        public String createdBy() {
            return createdBy;
        }

        /**
         * 返回updated By。
         */
        public String updatedBy() {
            return updatedBy;
        }

        /**
         * 返回创建时间。
         */
        public LocalDateTime createdAt() {
            return createdAt;
        }

        /**
         * 返回更新时间。
         */
        public LocalDateTime updatedAt() {
            return updatedAt;
        }

        /**
         * 按所有字段比较 TagOperationView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TagOperationView that = (TagOperationView) o;
            return java.util.Objects.equals(id, that.id)
                    && java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(userId, that.userId)
                    && java.util.Objects.equals(tagCode, that.tagCode)
                    && java.util.Objects.equals(tagValue, that.tagValue)
                    && java.util.Objects.equals(memberIds, that.memberIds)
                    && java.util.Objects.equals(metadata, that.metadata)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(affectedCount, that.affectedCount)
                    && java.util.Objects.equals(createdBy, that.createdBy)
                    && java.util.Objects.equals(updatedBy, that.updatedBy)
                    && java.util.Objects.equals(createdAt, that.createdAt)
                    && java.util.Objects.equals(updatedAt, that.updatedAt);
        }

        /**
         * 根据所有字段计算 TagOperationView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, tenantId, userId, tagCode, tagValue, memberIds, metadata, status, affectedCount, createdBy, updatedBy, createdAt, updatedAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "TagOperationView[" + "id=" + id + ", tenantId=" + tenantId + ", userId=" + userId + ", tagCode=" + tagCode + ", tagValue=" + tagValue + ", memberIds=" + memberIds + ", metadata=" + metadata + ", status=" + status + ", affectedCount=" + affectedCount + ", createdBy=" + createdBy + ", updatedBy=" + updatedBy + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
        }
    }
}
