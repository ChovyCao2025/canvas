package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定义 CdpWriteKeyFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWriteKeyFacade {

    /**
     * 查询list列表。
     */
    List<KeyRow> list(Long tenantId);

    /**
     * 创建create。
     */
    CreateResult create(Long tenantId, CreateCommand command, String actor);

    /**
     * 执行 disable 对应的 CDP 业务操作。
     */
    void disable(Long tenantId, Long id);

    /**
     * 表示 CreateCommand 的业务数据或处理组件。
     */
    final class CreateCommand {

        /**
         * 名称。
         */
        private final String name;

        /**
         * 平台。
         */
        private final String platform;

        /**
         * rate Limit Qps。
         */
        private final Integer rateLimitQps;

        /**
         * daily Quota。
         */
        private final Long dailyQuota;

        /**
         * 描述。
         */
        private final String description;

        /**
         * 使用记录字段创建 CreateCommand。
         */
        public CreateCommand(
                String name,
                String platform,
                Integer rateLimitQps,
                Long dailyQuota,
                String description) {
            this.name = name;
            this.platform = platform;
            this.rateLimitQps = rateLimitQps;
            this.dailyQuota = dailyQuota;
            this.description = description;
        }

        /**
         * 返回名称。
         */
        public String name() {
            return name;
        }

        /**
         * 返回平台。
         */
        public String platform() {
            return platform;
        }

        /**
         * 返回rate Limit Qps。
         */
        public Integer rateLimitQps() {
            return rateLimitQps;
        }

        /**
         * 返回daily Quota。
         */
        public Long dailyQuota() {
            return dailyQuota;
        }

        /**
         * 返回描述。
         */
        public String description() {
            return description;
        }

        /**
         * 按所有字段比较 CreateCommand。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CreateCommand that = (CreateCommand) o;
            return java.util.Objects.equals(name, that.name)
                    && java.util.Objects.equals(platform, that.platform)
                    && java.util.Objects.equals(rateLimitQps, that.rateLimitQps)
                    && java.util.Objects.equals(dailyQuota, that.dailyQuota)
                    && java.util.Objects.equals(description, that.description);
        }

        /**
         * 根据所有字段计算 CreateCommand 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, platform, rateLimitQps, dailyQuota, description);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "CreateCommand[" + "name=" + name + ", platform=" + platform + ", rateLimitQps=" + rateLimitQps + ", dailyQuota=" + dailyQuota + ", description=" + description + "]";
        }
    }

    /**
     * 表示 CreateResult 的业务数据或处理组件。
     */
    final class CreateResult {

        /**
         * 唯一标识。
         */
        private final Long id;

        /**
         * 名称。
         */
        private final String name;

        /**
         * raw Key。
         */
        private final String rawKey;

        /**
         * key Prefix。
         */
        private final String keyPrefix;

        /**
         * 平台。
         */
        private final String platform;

        /**
         * rate Limit Qps。
         */
        private final Integer rateLimitQps;

        /**
         * daily Quota。
         */
        private final Long dailyQuota;

        /**
         * 使用记录字段创建 CreateResult。
         */
        public CreateResult(
                Long id,
                String name,
                String rawKey,
                String keyPrefix,
                String platform,
                Integer rateLimitQps,
                Long dailyQuota) {
            this.id = id;
            this.name = name;
            this.rawKey = rawKey;
            this.keyPrefix = keyPrefix;
            this.platform = platform;
            this.rateLimitQps = rateLimitQps;
            this.dailyQuota = dailyQuota;
        }

        /**
         * 返回唯一标识。
         */
        public Long id() {
            return id;
        }

        /**
         * 返回名称。
         */
        public String name() {
            return name;
        }

        /**
         * 返回raw Key。
         */
        public String rawKey() {
            return rawKey;
        }

        /**
         * 返回key Prefix。
         */
        public String keyPrefix() {
            return keyPrefix;
        }

        /**
         * 返回平台。
         */
        public String platform() {
            return platform;
        }

        /**
         * 返回rate Limit Qps。
         */
        public Integer rateLimitQps() {
            return rateLimitQps;
        }

        /**
         * 返回daily Quota。
         */
        public Long dailyQuota() {
            return dailyQuota;
        }

        /**
         * 按所有字段比较 CreateResult。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CreateResult that = (CreateResult) o;
            return java.util.Objects.equals(id, that.id)
                    && java.util.Objects.equals(name, that.name)
                    && java.util.Objects.equals(rawKey, that.rawKey)
                    && java.util.Objects.equals(keyPrefix, that.keyPrefix)
                    && java.util.Objects.equals(platform, that.platform)
                    && java.util.Objects.equals(rateLimitQps, that.rateLimitQps)
                    && java.util.Objects.equals(dailyQuota, that.dailyQuota);
        }

        /**
         * 根据所有字段计算 CreateResult 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, name, rawKey, keyPrefix, platform, rateLimitQps, dailyQuota);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "CreateResult[" + "id=" + id + ", name=" + name + ", rawKey=" + rawKey + ", keyPrefix=" + keyPrefix + ", platform=" + platform + ", rateLimitQps=" + rateLimitQps + ", dailyQuota=" + dailyQuota + "]";
        }
    }

    /**
     * 表示 KeyRow 的业务数据或处理组件。
     */
    final class KeyRow {

        /**
         * 唯一标识。
         */
        private final Long id;

        /**
         * 名称。
         */
        private final String name;

        /**
         * key Prefix。
         */
        private final String keyPrefix;

        /**
         * 平台。
         */
        private final String platform;

        /**
         * 状态。
         */
        private final String status;

        /**
         * rate Limit Qps。
         */
        private final Integer rateLimitQps;

        /**
         * daily Quota。
         */
        private final Long dailyQuota;

        /**
         * 描述。
         */
        private final String description;

        /**
         * 创建人。
         */
        private final String createdBy;

        /**
         * 创建时间。
         */
        private final LocalDateTime createdAt;

        /**
         * 更新时间。
         */
        private final LocalDateTime updatedAt;

        /**
         * 使用记录字段创建 KeyRow。
         */
        public KeyRow(
                Long id,
                String name,
                String keyPrefix,
                String platform,
                String status,
                Integer rateLimitQps,
                Long dailyQuota,
                String description,
                String createdBy,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
            this.id = id;
            this.name = name;
            this.keyPrefix = keyPrefix;
            this.platform = platform;
            this.status = status;
            this.rateLimitQps = rateLimitQps;
            this.dailyQuota = dailyQuota;
            this.description = description;
            this.createdBy = createdBy;
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
         * 返回名称。
         */
        public String name() {
            return name;
        }

        /**
         * 返回key Prefix。
         */
        public String keyPrefix() {
            return keyPrefix;
        }

        /**
         * 返回平台。
         */
        public String platform() {
            return platform;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回rate Limit Qps。
         */
        public Integer rateLimitQps() {
            return rateLimitQps;
        }

        /**
         * 返回daily Quota。
         */
        public Long dailyQuota() {
            return dailyQuota;
        }

        /**
         * 返回描述。
         */
        public String description() {
            return description;
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
         * 按所有字段比较 KeyRow。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            KeyRow that = (KeyRow) o;
            return java.util.Objects.equals(id, that.id)
                    && java.util.Objects.equals(name, that.name)
                    && java.util.Objects.equals(keyPrefix, that.keyPrefix)
                    && java.util.Objects.equals(platform, that.platform)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(rateLimitQps, that.rateLimitQps)
                    && java.util.Objects.equals(dailyQuota, that.dailyQuota)
                    && java.util.Objects.equals(description, that.description)
                    && java.util.Objects.equals(createdBy, that.createdBy)
                    && java.util.Objects.equals(createdAt, that.createdAt)
                    && java.util.Objects.equals(updatedAt, that.updatedAt);
        }

        /**
         * 根据所有字段计算 KeyRow 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, name, keyPrefix, platform, status, rateLimitQps, dailyQuota, description, createdBy, createdAt, updatedAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "KeyRow[" + "id=" + id + ", name=" + name + ", keyPrefix=" + keyPrefix + ", platform=" + platform + ", status=" + status + ", rateLimitQps=" + rateLimitQps + ", dailyQuota=" + dailyQuota + ", description=" + description + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
        }
    }
}
