package org.chovy.canvas.cdp.api;

import java.util.List;

/**
 * 定义 CdpEventAttributeDiscoveryFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpEventAttributeDiscoveryFacade {

    /**
     * 查询Discovered列表。
     */
    List<DiscoveredAttributeView> listDiscovered(String status);

    /**
     * 表示 DiscoveredAttributeView 的业务数据或处理组件。
     */
    final class DiscoveredAttributeView {

        /**
         * 唯一标识。
         */
        private final Long id;

        /**
         * 事件编码。
         */
        private final String eventCode;

        /**
         * attr Name。
         */
        private final String attrName;

        /**
         * attr Type。
         */
        private final String attrType;

        /**
         * 状态。
         */
        private final String status;

        /**
         * sample Value。
         */
        private final String sampleValue;

        /**
         * 首次出现时间。
         */
        private final String firstSeenAt;

        /**
         * 最近出现时间。
         */
        private final String lastSeenAt;

        /**
         * 使用记录字段创建 DiscoveredAttributeView。
         */
        public DiscoveredAttributeView(
                Long id,
                String eventCode,
                String attrName,
                String attrType,
                String status,
                String sampleValue,
                String firstSeenAt,
                String lastSeenAt) {
            this.id = id;
            this.eventCode = eventCode;
            this.attrName = attrName;
            this.attrType = attrType;
            this.status = status;
            this.sampleValue = sampleValue;
            this.firstSeenAt = firstSeenAt;
            this.lastSeenAt = lastSeenAt;
        }

        /**
         * 返回唯一标识。
         */
        public Long id() {
            return id;
        }

        /**
         * 返回事件编码。
         */
        public String eventCode() {
            return eventCode;
        }

        /**
         * 返回attr Name。
         */
        public String attrName() {
            return attrName;
        }

        /**
         * 返回attr Type。
         */
        public String attrType() {
            return attrType;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回sample Value。
         */
        public String sampleValue() {
            return sampleValue;
        }

        /**
         * 返回首次出现时间。
         */
        public String firstSeenAt() {
            return firstSeenAt;
        }

        /**
         * 返回最近出现时间。
         */
        public String lastSeenAt() {
            return lastSeenAt;
        }

        /**
         * 按所有字段比较 DiscoveredAttributeView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DiscoveredAttributeView that = (DiscoveredAttributeView) o;
            return java.util.Objects.equals(id, that.id)
                    && java.util.Objects.equals(eventCode, that.eventCode)
                    && java.util.Objects.equals(attrName, that.attrName)
                    && java.util.Objects.equals(attrType, that.attrType)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(sampleValue, that.sampleValue)
                    && java.util.Objects.equals(firstSeenAt, that.firstSeenAt)
                    && java.util.Objects.equals(lastSeenAt, that.lastSeenAt);
        }

        /**
         * 根据所有字段计算 DiscoveredAttributeView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, eventCode, attrName, attrType, status, sampleValue, firstSeenAt, lastSeenAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "DiscoveredAttributeView[" + "id=" + id + ", eventCode=" + eventCode + ", attrName=" + attrName + ", attrType=" + attrType + ", status=" + status + ", sampleValue=" + sampleValue + ", firstSeenAt=" + firstSeenAt + ", lastSeenAt=" + lastSeenAt + "]";
        }
    }
}
