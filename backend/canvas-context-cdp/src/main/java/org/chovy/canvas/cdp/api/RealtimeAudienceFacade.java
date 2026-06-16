package org.chovy.canvas.cdp.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 定义 RealtimeAudienceFacade 对外暴露的 CDP 业务能力。
 */
public interface RealtimeAudienceFacade {

    /**
     * 执行 processEvent 对应的 CDP 业务操作。
     */
    EventResult processEvent(Long tenantId, Long audienceId, CdpEvent event, boolean removeOnNoMatch);

    /**
     * 创建Snapshot。
     */
    SnapshotResult createSnapshot(Long tenantId, Long audienceId, String reason, String actor);

    /**
     * 查询Snapshots列表。
     */
    List<SnapshotRow> listSnapshots(Long tenantId, Long audienceId, int limit);

    /**
     * 执行 overlap 对应的 CDP 业务操作。
     */
    OverlapResult overlap(Long leftId, Long rightId);

    /**
     * 执行 merge 对应的 CDP 业务操作。
     */
    SetOperationResult merge(Long leftId, Long rightId);

    /**
     * 执行 exclude 对应的 CDP 业务操作。
     */
    SetOperationResult exclude(Long baseId, Long excludedId);

    /**
     * 表示 CdpEvent 的业务数据或处理组件。
     */
    final class CdpEvent {

        /**
         * source Event Id。
         */
        private final String sourceEventId;

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * 事件时间。
         */
        private final Instant eventTime;

        /**
         * 扩展属性。
         */
        private final Map<String, Object> properties;

        /**
         * 使用记录字段创建 CdpEvent。
         */
        public CdpEvent(
                String sourceEventId,
                String userId,
                Instant eventTime,
                Map<String, Object> properties) {
            this.sourceEventId = sourceEventId;
            this.userId = userId;
            this.eventTime = eventTime;
            this.properties = properties;
        }

        /**
         * 返回source Event Id。
         */
        public String sourceEventId() {
            return sourceEventId;
        }

        /**
         * 返回用户标识。
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回事件时间。
         */
        public Instant eventTime() {
            return eventTime;
        }

        /**
         * 返回扩展属性。
         */
        public Map<String, Object> properties() {
            return properties;
        }

        /**
         * 按所有字段比较 CdpEvent。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CdpEvent that = (CdpEvent) o;
            return java.util.Objects.equals(sourceEventId, that.sourceEventId)
                    && java.util.Objects.equals(userId, that.userId)
                    && java.util.Objects.equals(eventTime, that.eventTime)
                    && java.util.Objects.equals(properties, that.properties);
        }

        /**
         * 根据所有字段计算 CdpEvent 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(sourceEventId, userId, eventTime, properties);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "CdpEvent[" + "sourceEventId=" + sourceEventId + ", userId=" + userId + ", eventTime=" + eventTime + ", properties=" + properties + "]";
        }
    }

    /**
     * 表示 EventResult 的业务数据或处理组件。
     */
    final class EventResult {

        /**
         * 人群标识。
         */
        private final Long audienceId;

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * matched。
         */
        private final boolean matched;

        /**
         * removed。
         */
        private final boolean removed;

        /**
         * 成员数量。
         */
        private final int memberCount;

        /**
         * 使用记录字段创建 EventResult。
         */
        public EventResult(
                Long audienceId,
                String userId,
                boolean matched,
                boolean removed,
                int memberCount) {
            this.audienceId = audienceId;
            this.userId = userId;
            this.matched = matched;
            this.removed = removed;
            this.memberCount = memberCount;
        }

        /**
         * 返回人群标识。
         */
        public Long audienceId() {
            return audienceId;
        }

        /**
         * 返回用户标识。
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回matched。
         */
        public boolean matched() {
            return matched;
        }

        /**
         * 返回removed。
         */
        public boolean removed() {
            return removed;
        }

        /**
         * 返回成员数量。
         */
        public int memberCount() {
            return memberCount;
        }

        /**
         * 按所有字段比较 EventResult。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EventResult that = (EventResult) o;
            return java.util.Objects.equals(audienceId, that.audienceId)
                    && java.util.Objects.equals(userId, that.userId)
                    && java.util.Objects.equals(matched, that.matched)
                    && java.util.Objects.equals(removed, that.removed)
                    && java.util.Objects.equals(memberCount, that.memberCount);
        }

        /**
         * 根据所有字段计算 EventResult 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(audienceId, userId, matched, removed, memberCount);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "EventResult[" + "audienceId=" + audienceId + ", userId=" + userId + ", matched=" + matched + ", removed=" + removed + ", memberCount=" + memberCount + "]";
        }
    }

    /**
     * 表示 SnapshotResult 的业务数据或处理组件。
     */
    final class SnapshotResult {

        /**
         * 快照标识。
         */
        private final Long snapshotId;

        /**
         * 人群标识。
         */
        private final Long audienceId;

        /**
         * 原因。
         */
        private final String reason;

        /**
         * 创建人。
         */
        private final String createdBy;

        /**
         * 成员数量。
         */
        private final int memberCount;

        /**
         * 创建时间。
         */
        private final String createdAt;

        /**
         * 使用记录字段创建 SnapshotResult。
         */
        public SnapshotResult(
                Long snapshotId,
                Long audienceId,
                String reason,
                String createdBy,
                int memberCount,
                String createdAt) {
            this.snapshotId = snapshotId;
            this.audienceId = audienceId;
            this.reason = reason;
            this.createdBy = createdBy;
            this.memberCount = memberCount;
            this.createdAt = createdAt;
        }

        /**
         * 返回快照标识。
         */
        public Long snapshotId() {
            return snapshotId;
        }

        /**
         * 返回人群标识。
         */
        public Long audienceId() {
            return audienceId;
        }

        /**
         * 返回原因。
         */
        public String reason() {
            return reason;
        }

        /**
         * 返回创建人。
         */
        public String createdBy() {
            return createdBy;
        }

        /**
         * 返回成员数量。
         */
        public int memberCount() {
            return memberCount;
        }

        /**
         * 返回创建时间。
         */
        public String createdAt() {
            return createdAt;
        }

        /**
         * 按所有字段比较 SnapshotResult。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SnapshotResult that = (SnapshotResult) o;
            return java.util.Objects.equals(snapshotId, that.snapshotId)
                    && java.util.Objects.equals(audienceId, that.audienceId)
                    && java.util.Objects.equals(reason, that.reason)
                    && java.util.Objects.equals(createdBy, that.createdBy)
                    && java.util.Objects.equals(memberCount, that.memberCount)
                    && java.util.Objects.equals(createdAt, that.createdAt);
        }

        /**
         * 根据所有字段计算 SnapshotResult 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(snapshotId, audienceId, reason, createdBy, memberCount, createdAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "SnapshotResult[" + "snapshotId=" + snapshotId + ", audienceId=" + audienceId + ", reason=" + reason + ", createdBy=" + createdBy + ", memberCount=" + memberCount + ", createdAt=" + createdAt + "]";
        }
    }

    /**
     * 表示 SnapshotRow 的业务数据或处理组件。
     */
    final class SnapshotRow {

        /**
         * 快照标识。
         */
        private final Long snapshotId;

        /**
         * 人群标识。
         */
        private final Long audienceId;

        /**
         * 成员数量。
         */
        private final int memberCount;

        /**
         * 原因。
         */
        private final String reason;

        /**
         * 创建人。
         */
        private final String createdBy;

        /**
         * 创建时间。
         */
        private final String createdAt;

        /**
         * 使用记录字段创建 SnapshotRow。
         */
        public SnapshotRow(
                Long snapshotId,
                Long audienceId,
                int memberCount,
                String reason,
                String createdBy,
                String createdAt) {
            this.snapshotId = snapshotId;
            this.audienceId = audienceId;
            this.memberCount = memberCount;
            this.reason = reason;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
        }

        /**
         * 返回快照标识。
         */
        public Long snapshotId() {
            return snapshotId;
        }

        /**
         * 返回人群标识。
         */
        public Long audienceId() {
            return audienceId;
        }

        /**
         * 返回成员数量。
         */
        public int memberCount() {
            return memberCount;
        }

        /**
         * 返回原因。
         */
        public String reason() {
            return reason;
        }

        /**
         * 返回创建人。
         */
        public String createdBy() {
            return createdBy;
        }

        /**
         * 返回创建时间。
         */
        public String createdAt() {
            return createdAt;
        }

        /**
         * 按所有字段比较 SnapshotRow。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SnapshotRow that = (SnapshotRow) o;
            return java.util.Objects.equals(snapshotId, that.snapshotId)
                    && java.util.Objects.equals(audienceId, that.audienceId)
                    && java.util.Objects.equals(memberCount, that.memberCount)
                    && java.util.Objects.equals(reason, that.reason)
                    && java.util.Objects.equals(createdBy, that.createdBy)
                    && java.util.Objects.equals(createdAt, that.createdAt);
        }

        /**
         * 根据所有字段计算 SnapshotRow 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(snapshotId, audienceId, memberCount, reason, createdBy, createdAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "SnapshotRow[" + "snapshotId=" + snapshotId + ", audienceId=" + audienceId + ", memberCount=" + memberCount + ", reason=" + reason + ", createdBy=" + createdBy + ", createdAt=" + createdAt + "]";
        }
    }

    /**
     * 表示 OverlapResult 的业务数据或处理组件。
     */
    final class OverlapResult {

        /**
         * 左侧标识。
         */
        private final Long leftId;

        /**
         * 右侧标识。
         */
        private final Long rightId;

        /**
         * 交集数量。
         */
        private final int overlapCount;

        /**
         * 成员标识列表。
         */
        private final List<String> memberIds;

        /**
         * 使用记录字段创建 OverlapResult。
         */
        public OverlapResult(
                Long leftId,
                Long rightId,
                int overlapCount,
                List<String> memberIds) {
            this.leftId = leftId;
            this.rightId = rightId;
            this.overlapCount = overlapCount;
            this.memberIds = memberIds;
        }

        /**
         * 返回左侧标识。
         */
        public Long leftId() {
            return leftId;
        }

        /**
         * 返回右侧标识。
         */
        public Long rightId() {
            return rightId;
        }

        /**
         * 返回交集数量。
         */
        public int overlapCount() {
            return overlapCount;
        }

        /**
         * 返回成员标识列表。
         */
        public List<String> memberIds() {
            return memberIds;
        }

        /**
         * 按所有字段比较 OverlapResult。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OverlapResult that = (OverlapResult) o;
            return java.util.Objects.equals(leftId, that.leftId)
                    && java.util.Objects.equals(rightId, that.rightId)
                    && java.util.Objects.equals(overlapCount, that.overlapCount)
                    && java.util.Objects.equals(memberIds, that.memberIds);
        }

        /**
         * 根据所有字段计算 OverlapResult 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(leftId, rightId, overlapCount, memberIds);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "OverlapResult[" + "leftId=" + leftId + ", rightId=" + rightId + ", overlapCount=" + overlapCount + ", memberIds=" + memberIds + "]";
        }
    }

    /**
     * 表示 SetOperationResult 的业务数据或处理组件。
     */
    final class SetOperationResult {

        /**
         * 操作类型。
         */
        private final String operation;

        /**
         * 左侧标识。
         */
        private final Long leftId;

        /**
         * 右侧标识。
         */
        private final Long rightId;

        /**
         * 成员数量。
         */
        private final int memberCount;

        /**
         * 成员标识列表。
         */
        private final List<String> memberIds;

        /**
         * 使用记录字段创建 SetOperationResult。
         */
        public SetOperationResult(
                String operation,
                Long leftId,
                Long rightId,
                int memberCount,
                List<String> memberIds) {
            this.operation = operation;
            this.leftId = leftId;
            this.rightId = rightId;
            this.memberCount = memberCount;
            this.memberIds = memberIds;
        }

        /**
         * 返回操作类型。
         */
        public String operation() {
            return operation;
        }

        /**
         * 返回左侧标识。
         */
        public Long leftId() {
            return leftId;
        }

        /**
         * 返回右侧标识。
         */
        public Long rightId() {
            return rightId;
        }

        /**
         * 返回成员数量。
         */
        public int memberCount() {
            return memberCount;
        }

        /**
         * 返回成员标识列表。
         */
        public List<String> memberIds() {
            return memberIds;
        }

        /**
         * 按所有字段比较 SetOperationResult。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SetOperationResult that = (SetOperationResult) o;
            return java.util.Objects.equals(operation, that.operation)
                    && java.util.Objects.equals(leftId, that.leftId)
                    && java.util.Objects.equals(rightId, that.rightId)
                    && java.util.Objects.equals(memberCount, that.memberCount)
                    && java.util.Objects.equals(memberIds, that.memberIds);
        }

        /**
         * 根据所有字段计算 SetOperationResult 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(operation, leftId, rightId, memberCount, memberIds);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "SetOperationResult[" + "operation=" + operation + ", leftId=" + leftId + ", rightId=" + rightId + ", memberCount=" + memberCount + ", memberIds=" + memberIds + "]";
        }
    }
}
