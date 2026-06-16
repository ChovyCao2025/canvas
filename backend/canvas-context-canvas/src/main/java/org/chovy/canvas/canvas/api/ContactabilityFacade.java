package org.chovy.canvas.canvas.api;

import java.time.Duration;
import java.util.List;

/**
 * 定义ContactabilityFacade对外提供的能力契约。
 */
public interface ContactabilityFacade {

    /**
     * 处理explain。
     */
    Report explain(Request request);

    /**
     * 承载请求的数据快照。
     */
    record Request(
            /**
             * 记录用户标识。
             */
            String userId,
            /**
             * 记录channel。
             */
            String channel,
            /**
             * 记录requireExplicitConsent。
             */
            boolean requireExplicitConsent,
            /**
             * 记录quietStart。
             */
            String quietStart,
            /**
             * 记录quietEnd。
             */
            String quietEnd,
            /**
             * 记录quietTimezone。
             */
            String quietTimezone,
            /**
             * 记录画布标识。
             */
            Long canvasId,
            /**
             * 记录节点标识。
             */
            String nodeId,
            /**
             * 记录frequencyScope。
             */
            String frequencyScope,
            /**
             * 记录frequencyMax。
             */
            int frequencyMax,
            /**
             * 记录frequencyWindow。
             */
            Duration frequencyWindow) {
    }

    /**
     * 承载Report的数据快照。
     */
    record Report(
            /**
             * 记录用户标识。
             */
            String userId,
            /**
             * 记录channel。
             */
            String channel,
            /**
             * 记录allowed。
             */
            boolean allowed,
            /**
             * 记录checks。
             */
            List<Check> checks) {
    }

    /**
     * 承载Check的数据快照。
     */
    record Check(
            /**
             * 记录checkKey。
             */
            String checkKey,
            /**
             * 记录allowed。
             */
            boolean allowed,
            /**
             * 记录reasonCode。
             */
            String reasonCode,
            /**
             * 记录reasonMessage。
             */
            String reasonMessage) {
    }
}
