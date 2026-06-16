package org.chovy.canvas.canvas.application;

import java.util.Map;

/**
 * 承载UserInputResumeRequest的数据快照。
 */
public record UserInputResumeRequest(
        /**
         * 记录租户标识。
         */
        Long tenantId,
        /**
         * 记录画布标识。
         */
        Long canvasId,
        /**
         * 记录版本标识。
         */
        Long versionId,
        /**
         * 记录execution标识。
         */
        String executionId,
        /**
         * 记录节点标识。
         */
        String nodeId,
        /**
         * 记录用户标识。
         */
        String userId,
        /**
         * 记录响应标识。
         */
        Long responseId,
        /**
         * 记录resumeStatus。
         */
        String resumeStatus,
        /**
         * 记录载荷。
         */
        Map<String, Object> payload) {

    public UserInputResumeRequest {
        payload = Map.copyOf(payload == null ? Map.of() : payload);
    }
}
