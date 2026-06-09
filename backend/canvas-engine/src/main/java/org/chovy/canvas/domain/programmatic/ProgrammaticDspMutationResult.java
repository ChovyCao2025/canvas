package org.chovy.canvas.domain.programmatic;

import java.util.Map;

/**
 * ProgrammaticDspMutationResult 承载 domain.programmatic 场景中的不可变数据快照。
 * @param success success 字段。
 * @param providerOperationId providerOperationId 字段。
 * @param errorCode errorCode 字段。
 * @param errorMessage errorMessage 字段。
 * @param response response 字段。
 */
public record ProgrammaticDspMutationResult(
        boolean success,
        String providerOperationId,
        String errorCode,
        String errorMessage,
        Map<String, Object> response
) {

    /**
     * success 处理 domain.programmatic 场景的业务逻辑。
     * @param providerOperationId 业务对象 ID，用于定位具体记录。
     * @param response response 参数，用于 success 流程中的校验、计算或对象转换。
     * @return 返回 success 流程生成的业务结果。
     */
    public static ProgrammaticDspMutationResult success(String providerOperationId, Map<String, Object> response) {
        return new ProgrammaticDspMutationResult(true, providerOperationId, null, null,
                response == null ? Map.of() : response);
    }

    /**
     * failure 处理 domain.programmatic 场景的业务逻辑。
     * @param errorCode 业务编码，用于匹配对应类型或状态。
     * @param errorMessage error message 参数，用于 failure 流程中的校验、计算或对象转换。
     * @param response response 参数，用于 failure 流程中的校验、计算或对象转换。
     * @return 返回 failure 流程生成的业务结果。
     */
    public static ProgrammaticDspMutationResult failure(String errorCode,
                                                       String errorMessage,
                                                       Map<String, Object> response) {
        return new ProgrammaticDspMutationResult(false, null, errorCode, errorMessage,
                response == null ? Map.of() : response);
    }
}
