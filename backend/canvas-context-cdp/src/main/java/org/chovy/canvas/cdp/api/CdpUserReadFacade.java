package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定义 CdpUserReadFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpUserReadFacade {

    /**
     * 查询Users列表。
     */
    List<CdpUserRowView> listUsers(Long tenantId, String keyword);

    /**
     * 返回user。
     */
    CdpUserProfileView getUser(Long tenantId, String userId);

    /**
     * 返回insight。
     */
    CdpUserInsightView getInsight(Long tenantId, String userId);

    /**
     * 表示 CdpUserRowView 的业务数据或处理组件。
     */
    final class CdpUserRowView {

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * 展示名称。
         */
        private final String displayName;

        /**
         * execution Count。
         */
        private final long executionCount;

        /**
         * success Count。
         */
        private final long successCount;

        /**
         * failed Count。
         */
        private final long failedCount;

        /**
         * latest Status。
         */
        private final String latestStatus;

        /**
         * first Entered At。
         */
        private final LocalDateTime firstEnteredAt;

        /**
         * last Entered At。
         */
        private final LocalDateTime lastEnteredAt;

        /**
         * tags。
         */
        private final List<CdpUserTagSummaryView> tags;

        /**
         * 使用记录字段创建 CdpUserRowView。
         */
        public CdpUserRowView(
                String userId,
                String displayName,
                long executionCount,
                long successCount,
                long failedCount,
                String latestStatus,
                LocalDateTime firstEnteredAt,
                LocalDateTime lastEnteredAt,
                List<CdpUserTagSummaryView> tags) {
            this.userId = userId;
            this.displayName = displayName;
            this.executionCount = executionCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.latestStatus = latestStatus;
            this.firstEnteredAt = firstEnteredAt;
            this.lastEnteredAt = lastEnteredAt;
            this.tags = tags;
        }

        /**
         * 返回用户标识。
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回展示名称。
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回execution Count。
         */
        public long executionCount() {
            return executionCount;
        }

        /**
         * 返回success Count。
         */
        public long successCount() {
            return successCount;
        }

        /**
         * 返回failed Count。
         */
        public long failedCount() {
            return failedCount;
        }

        /**
         * 返回latest Status。
         */
        public String latestStatus() {
            return latestStatus;
        }

        /**
         * 返回first Entered At。
         */
        public LocalDateTime firstEnteredAt() {
            return firstEnteredAt;
        }

        /**
         * 返回last Entered At。
         */
        public LocalDateTime lastEnteredAt() {
            return lastEnteredAt;
        }

        /**
         * 返回tags。
         */
        public List<CdpUserTagSummaryView> tags() {
            return tags;
        }

        /**
         * 按所有字段比较 CdpUserRowView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CdpUserRowView that = (CdpUserRowView) o;
            return java.util.Objects.equals(userId, that.userId)
                    && java.util.Objects.equals(displayName, that.displayName)
                    && java.util.Objects.equals(executionCount, that.executionCount)
                    && java.util.Objects.equals(successCount, that.successCount)
                    && java.util.Objects.equals(failedCount, that.failedCount)
                    && java.util.Objects.equals(latestStatus, that.latestStatus)
                    && java.util.Objects.equals(firstEnteredAt, that.firstEnteredAt)
                    && java.util.Objects.equals(lastEnteredAt, that.lastEnteredAt)
                    && java.util.Objects.equals(tags, that.tags);
        }

        /**
         * 根据所有字段计算 CdpUserRowView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, displayName, executionCount, successCount, failedCount, latestStatus, firstEnteredAt, lastEnteredAt, tags);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "CdpUserRowView[" + "userId=" + userId + ", displayName=" + displayName + ", executionCount=" + executionCount + ", successCount=" + successCount + ", failedCount=" + failedCount + ", latestStatus=" + latestStatus + ", firstEnteredAt=" + firstEnteredAt + ", lastEnteredAt=" + lastEnteredAt + ", tags=" + tags + "]";
        }
    }

    /**
     * 表示 CdpUserProfileView 的业务数据或处理组件。
     */
    final class CdpUserProfileView {

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * 展示名称。
         */
        private final String displayName;

        /**
         * 手机号。
         */
        private final String phone;

        /**
         * 邮箱。
         */
        private final String email;

        /**
         * 状态。
         */
        private final String status;

        /**
         * properties Json。
         */
        private final String propertiesJson;

        /**
         * 首次出现时间。
         */
        private final LocalDateTime firstSeenAt;

        /**
         * 最近出现时间。
         */
        private final LocalDateTime lastSeenAt;

        /**
         * 使用记录字段创建 CdpUserProfileView。
         */
        public CdpUserProfileView(
                String userId,
                String displayName,
                String phone,
                String email,
                String status,
                String propertiesJson,
                LocalDateTime firstSeenAt,
                LocalDateTime lastSeenAt) {
            this.userId = userId;
            this.displayName = displayName;
            this.phone = phone;
            this.email = email;
            this.status = status;
            this.propertiesJson = propertiesJson;
            this.firstSeenAt = firstSeenAt;
            this.lastSeenAt = lastSeenAt;
        }

        /**
         * 返回用户标识。
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回展示名称。
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回手机号。
         */
        public String phone() {
            return phone;
        }

        /**
         * 返回邮箱。
         */
        public String email() {
            return email;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回properties Json。
         */
        public String propertiesJson() {
            return propertiesJson;
        }

        /**
         * 返回首次出现时间。
         */
        public LocalDateTime firstSeenAt() {
            return firstSeenAt;
        }

        /**
         * 返回最近出现时间。
         */
        public LocalDateTime lastSeenAt() {
            return lastSeenAt;
        }

        /**
         * 按所有字段比较 CdpUserProfileView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CdpUserProfileView that = (CdpUserProfileView) o;
            return java.util.Objects.equals(userId, that.userId)
                    && java.util.Objects.equals(displayName, that.displayName)
                    && java.util.Objects.equals(phone, that.phone)
                    && java.util.Objects.equals(email, that.email)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(propertiesJson, that.propertiesJson)
                    && java.util.Objects.equals(firstSeenAt, that.firstSeenAt)
                    && java.util.Objects.equals(lastSeenAt, that.lastSeenAt);
        }

        /**
         * 根据所有字段计算 CdpUserProfileView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, displayName, phone, email, status, propertiesJson, firstSeenAt, lastSeenAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "CdpUserProfileView[" + "userId=" + userId + ", displayName=" + displayName + ", phone=" + phone + ", email=" + email + ", status=" + status + ", propertiesJson=" + propertiesJson + ", firstSeenAt=" + firstSeenAt + ", lastSeenAt=" + lastSeenAt + "]";
        }
    }

    /**
     * 表示 CdpUserInsightView 的业务数据或处理组件。
     */
    final class CdpUserInsightView {

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * profile。
         */
        private final CdpUserProfileView profile;

        /**
         * tags。
         */
        private final List<CdpUserTagSummaryView> tags;

        /**
         * canvas Rows。
         */
        private final List<CdpUserCanvasSummaryView> canvasRows;

        /**
         * 使用记录字段创建 CdpUserInsightView。
         */
        public CdpUserInsightView(
                String userId,
                CdpUserProfileView profile,
                List<CdpUserTagSummaryView> tags,
                List<CdpUserCanvasSummaryView> canvasRows) {
            this.userId = userId;
            this.profile = profile;
            this.tags = tags;
            this.canvasRows = canvasRows;
        }

        /**
         * 返回用户标识。
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回profile。
         */
        public CdpUserProfileView profile() {
            return profile;
        }

        /**
         * 返回tags。
         */
        public List<CdpUserTagSummaryView> tags() {
            return tags;
        }

        /**
         * 返回canvas Rows。
         */
        public List<CdpUserCanvasSummaryView> canvasRows() {
            return canvasRows;
        }

        /**
         * 按所有字段比较 CdpUserInsightView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CdpUserInsightView that = (CdpUserInsightView) o;
            return java.util.Objects.equals(userId, that.userId)
                    && java.util.Objects.equals(profile, that.profile)
                    && java.util.Objects.equals(tags, that.tags)
                    && java.util.Objects.equals(canvasRows, that.canvasRows);
        }

        /**
         * 根据所有字段计算 CdpUserInsightView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, profile, tags, canvasRows);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "CdpUserInsightView[" + "userId=" + userId + ", profile=" + profile + ", tags=" + tags + ", canvasRows=" + canvasRows + "]";
        }
    }

    /**
     * 表示 CdpUserTagSummaryView 的业务数据或处理组件。
     */
    final class CdpUserTagSummaryView {

        /**
         * 标签编码。
         */
        private final String tagCode;

        /**
         * 标签名称。
         */
        private final String tagName;

        /**
         * 标签值。
         */
        private final String tagValue;

        /**
         * 值类型。
         */
        private final String valueType;

        /**
         * 来源类型。
         */
        private final String sourceType;

        /**
         * 状态。
         */
        private final String status;

        /**
         * effective At。
         */
        private final LocalDateTime effectiveAt;

        /**
         * expires At。
         */
        private final LocalDateTime expiresAt;

        /**
         * 更新时间。
         */
        private final LocalDateTime updatedAt;

        /**
         * 使用记录字段创建 CdpUserTagSummaryView。
         */
        public CdpUserTagSummaryView(
                String tagCode,
                String tagName,
                String tagValue,
                String valueType,
                String sourceType,
                String status,
                LocalDateTime effectiveAt,
                LocalDateTime expiresAt,
                LocalDateTime updatedAt) {
            this.tagCode = tagCode;
            this.tagName = tagName;
            this.tagValue = tagValue;
            this.valueType = valueType;
            this.sourceType = sourceType;
            this.status = status;
            this.effectiveAt = effectiveAt;
            this.expiresAt = expiresAt;
            this.updatedAt = updatedAt;
        }

        /**
         * 返回标签编码。
         */
        public String tagCode() {
            return tagCode;
        }

        /**
         * 返回标签名称。
         */
        public String tagName() {
            return tagName;
        }

        /**
         * 返回标签值。
         */
        public String tagValue() {
            return tagValue;
        }

        /**
         * 返回值类型。
         */
        public String valueType() {
            return valueType;
        }

        /**
         * 返回来源类型。
         */
        public String sourceType() {
            return sourceType;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回effective At。
         */
        public LocalDateTime effectiveAt() {
            return effectiveAt;
        }

        /**
         * 返回expires At。
         */
        public LocalDateTime expiresAt() {
            return expiresAt;
        }

        /**
         * 返回更新时间。
         */
        public LocalDateTime updatedAt() {
            return updatedAt;
        }

        /**
         * 按所有字段比较 CdpUserTagSummaryView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CdpUserTagSummaryView that = (CdpUserTagSummaryView) o;
            return java.util.Objects.equals(tagCode, that.tagCode)
                    && java.util.Objects.equals(tagName, that.tagName)
                    && java.util.Objects.equals(tagValue, that.tagValue)
                    && java.util.Objects.equals(valueType, that.valueType)
                    && java.util.Objects.equals(sourceType, that.sourceType)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(effectiveAt, that.effectiveAt)
                    && java.util.Objects.equals(expiresAt, that.expiresAt)
                    && java.util.Objects.equals(updatedAt, that.updatedAt);
        }

        /**
         * 根据所有字段计算 CdpUserTagSummaryView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(tagCode, tagName, tagValue, valueType, sourceType, status, effectiveAt, expiresAt, updatedAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "CdpUserTagSummaryView[" + "tagCode=" + tagCode + ", tagName=" + tagName + ", tagValue=" + tagValue + ", valueType=" + valueType + ", sourceType=" + sourceType + ", status=" + status + ", effectiveAt=" + effectiveAt + ", expiresAt=" + expiresAt + ", updatedAt=" + updatedAt + "]";
        }
    }

    /**
     * 表示 CdpUserCanvasSummaryView 的业务数据或处理组件。
     */
    final class CdpUserCanvasSummaryView {

        /**
         * 画布标识。
         */
        private final Long canvasId;

        /**
         * canvas Name。
         */
        private final String canvasName;

        /**
         * execution Count。
         */
        private final long executionCount;

        /**
         * success Count。
         */
        private final long successCount;

        /**
         * failed Count。
         */
        private final long failedCount;

        /**
         * latest Status。
         */
        private final String latestStatus;

        /**
         * first Entered At。
         */
        private final LocalDateTime firstEnteredAt;

        /**
         * last Entered At。
         */
        private final LocalDateTime lastEnteredAt;

        /**
         * 使用记录字段创建 CdpUserCanvasSummaryView。
         */
        public CdpUserCanvasSummaryView(
                Long canvasId,
                String canvasName,
                long executionCount,
                long successCount,
                long failedCount,
                String latestStatus,
                LocalDateTime firstEnteredAt,
                LocalDateTime lastEnteredAt) {
            this.canvasId = canvasId;
            this.canvasName = canvasName;
            this.executionCount = executionCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.latestStatus = latestStatus;
            this.firstEnteredAt = firstEnteredAt;
            this.lastEnteredAt = lastEnteredAt;
        }

        /**
         * 返回画布标识。
         */
        public Long canvasId() {
            return canvasId;
        }

        /**
         * 返回canvas Name。
         */
        public String canvasName() {
            return canvasName;
        }

        /**
         * 返回execution Count。
         */
        public long executionCount() {
            return executionCount;
        }

        /**
         * 返回success Count。
         */
        public long successCount() {
            return successCount;
        }

        /**
         * 返回failed Count。
         */
        public long failedCount() {
            return failedCount;
        }

        /**
         * 返回latest Status。
         */
        public String latestStatus() {
            return latestStatus;
        }

        /**
         * 返回first Entered At。
         */
        public LocalDateTime firstEnteredAt() {
            return firstEnteredAt;
        }

        /**
         * 返回last Entered At。
         */
        public LocalDateTime lastEnteredAt() {
            return lastEnteredAt;
        }

        /**
         * 按所有字段比较 CdpUserCanvasSummaryView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CdpUserCanvasSummaryView that = (CdpUserCanvasSummaryView) o;
            return java.util.Objects.equals(canvasId, that.canvasId)
                    && java.util.Objects.equals(canvasName, that.canvasName)
                    && java.util.Objects.equals(executionCount, that.executionCount)
                    && java.util.Objects.equals(successCount, that.successCount)
                    && java.util.Objects.equals(failedCount, that.failedCount)
                    && java.util.Objects.equals(latestStatus, that.latestStatus)
                    && java.util.Objects.equals(firstEnteredAt, that.firstEnteredAt)
                    && java.util.Objects.equals(lastEnteredAt, that.lastEnteredAt);
        }

        /**
         * 根据所有字段计算 CdpUserCanvasSummaryView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(canvasId, canvasName, executionCount, successCount, failedCount, latestStatus, firstEnteredAt, lastEnteredAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "CdpUserCanvasSummaryView[" + "canvasId=" + canvasId + ", canvasName=" + canvasName + ", executionCount=" + executionCount + ", successCount=" + successCount + ", failedCount=" + failedCount + ", latestStatus=" + latestStatus + ", firstEnteredAt=" + firstEnteredAt + ", lastEnteredAt=" + lastEnteredAt + "]";
        }
    }
}
