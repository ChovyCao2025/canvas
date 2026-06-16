package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 CdpWarehouseDataPathProbeFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseDataPathProbeFacade {

    /**
     * command)。
     */
    Map<String, Object> run(Long tenantId, RunCommand command);

    /**
     * limit)。
     */
    List<Map<String, Object>> recent(Long tenantId, int limit);

    /**
     * 表示 RunCommand 的业务数据或处理组件。
     */
    final class RunCommand {

        /**
         * probe Key。
         */
        private final String probeKey;

        /**
         * 事件编码。
         */
        private final String eventCode;

        /**
         * strict。
         */
        private final boolean strict;

        /**
         * verify Attempts。
         */
        private final int verifyAttempts;

        /**
         * verify Delay Ms。
         */
        private final int verifyDelayMs;

        /**
         * source Mode。
         */
        private final String sourceMode;

        /**
         * 使用记录字段创建 RunCommand。
         */
        public RunCommand(
                String probeKey,
                String eventCode,
                boolean strict,
                int verifyAttempts,
                int verifyDelayMs,
                String sourceMode) {
            this.probeKey = probeKey;
            this.eventCode = eventCode;
            this.strict = strict;
            this.verifyAttempts = verifyAttempts;
            this.verifyDelayMs = verifyDelayMs;
            this.sourceMode = sourceMode;
        }

        /**
         * 返回probe Key。
         */
        public String probeKey() {
            return probeKey;
        }

        /**
         * 返回事件编码。
         */
        public String eventCode() {
            return eventCode;
        }

        /**
         * 返回strict。
         */
        public boolean strict() {
            return strict;
        }

        /**
         * 返回verify Attempts。
         */
        public int verifyAttempts() {
            return verifyAttempts;
        }

        /**
         * 返回verify Delay Ms。
         */
        public int verifyDelayMs() {
            return verifyDelayMs;
        }

        /**
         * 返回source Mode。
         */
        public String sourceMode() {
            return sourceMode;
        }

        /**
         * 按所有字段比较 RunCommand。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RunCommand that = (RunCommand) o;
            return java.util.Objects.equals(probeKey, that.probeKey)
                    && java.util.Objects.equals(eventCode, that.eventCode)
                    && java.util.Objects.equals(strict, that.strict)
                    && java.util.Objects.equals(verifyAttempts, that.verifyAttempts)
                    && java.util.Objects.equals(verifyDelayMs, that.verifyDelayMs)
                    && java.util.Objects.equals(sourceMode, that.sourceMode);
        }

        /**
         * 根据所有字段计算 RunCommand 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(probeKey, eventCode, strict, verifyAttempts, verifyDelayMs, sourceMode);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "RunCommand[" + "probeKey=" + probeKey + ", eventCode=" + eventCode + ", strict=" + strict + ", verifyAttempts=" + verifyAttempts + ", verifyDelayMs=" + verifyDelayMs + ", sourceMode=" + sourceMode + "]";
        }
    }

}
