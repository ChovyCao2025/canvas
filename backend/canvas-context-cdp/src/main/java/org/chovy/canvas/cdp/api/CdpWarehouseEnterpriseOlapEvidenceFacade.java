package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 CdpWarehouseEnterpriseOlapEvidenceFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseEnterpriseOlapEvidenceFacade {

    /**
     * actor)。
     */
    Map<String, Object> record(Long tenantId, EvidenceCommand command, String actor);

    /**
     * tenant Id)。
     */
    Map<String, Object> latest(Long tenantId);

    /**
     * tenant Id)。
     */
    List<Map<String, Object>> proof(Long tenantId);

    /**
     * actor)。
     */
    Map<String, Object> collect(Long tenantId, String triggerType, String actor);

    /**
     * limit)。
     */
    List<Map<String, Object>> collections(Long tenantId, Integer limit);

    /**
     * 表示 EvidenceCommand 的业务数据或处理组件。
     */
    final class EvidenceCommand {

        /**
         * evidence Key。
         */
        private final String evidenceKey;

        /**
         * 状态。
         */
        private final String status;

        /**
         * 原因。
         */
        private final String reason;

        /**
         * measured At。
         */
        private final String measuredAt;

        /**
         * expires At。
         */
        private final String expiresAt;

        /**
         * evidence Json。
         */
        private final String evidenceJson;

        /**
         * 使用记录字段创建 EvidenceCommand。
         */
        public EvidenceCommand(
                String evidenceKey,
                String status,
                String reason,
                String measuredAt,
                String expiresAt,
                String evidenceJson) {
            this.evidenceKey = evidenceKey;
            this.status = status;
            this.reason = reason;
            this.measuredAt = measuredAt;
            this.expiresAt = expiresAt;
            this.evidenceJson = evidenceJson;
        }

        /**
         * 返回evidence Key。
         */
        public String evidenceKey() {
            return evidenceKey;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回原因。
         */
        public String reason() {
            return reason;
        }

        /**
         * 返回measured At。
         */
        public String measuredAt() {
            return measuredAt;
        }

        /**
         * 返回expires At。
         */
        public String expiresAt() {
            return expiresAt;
        }

        /**
         * 返回evidence Json。
         */
        public String evidenceJson() {
            return evidenceJson;
        }

        /**
         * 按所有字段比较 EvidenceCommand。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EvidenceCommand that = (EvidenceCommand) o;
            return java.util.Objects.equals(evidenceKey, that.evidenceKey)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(reason, that.reason)
                    && java.util.Objects.equals(measuredAt, that.measuredAt)
                    && java.util.Objects.equals(expiresAt, that.expiresAt)
                    && java.util.Objects.equals(evidenceJson, that.evidenceJson);
        }

        /**
         * 根据所有字段计算 EvidenceCommand 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(evidenceKey, status, reason, measuredAt, expiresAt, evidenceJson);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "EvidenceCommand[" + "evidenceKey=" + evidenceKey + ", status=" + status + ", reason=" + reason + ", measuredAt=" + measuredAt + ", expiresAt=" + expiresAt + ", evidenceJson=" + evidenceJson + "]";
        }
    }
}
