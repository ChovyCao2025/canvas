package org.chovy.canvas.domain.programmatic;

import org.chovy.canvas.domain.providerwrite.ProviderWriteSandboxSupport;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SandboxProgrammaticDspProviderWriteClient 编排 domain.programmatic 场景的领域业务规则。
 */
@Component
public class SandboxProgrammaticDspProviderWriteClient implements ProgrammaticDspProviderWriteClient {

    /**
     * supports 处理 domain.programmatic 场景的业务逻辑。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 supports 的布尔判断结果。
     */
    @Override
    public boolean supports(ProgrammaticDspMutationRequest request) {
        return request != null && ProviderWriteSandboxSupport.supportsSandboxProvider(request.provider());
    }

    /**
     * execute 更新 domain.programmatic 场景的业务状态。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    @Override
    public ProgrammaticDspMutationResult execute(ProgrammaticDspMutationRequest request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request == null) {
            return ProgrammaticDspMutationResult.failure(
                    "INVALID_REQUEST",
                    "programmatic DSP mutation request is required",
                    Map.of());
        }
        if (!supports(request)) {
            return ProgrammaticDspMutationResult.failure(
                    "UNSUPPORTED_PROVIDER",
                    "Sandbox programmatic DSP provider client only supports sandbox providers",
                    Map.of("provider", request.provider()));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return ProgrammaticDspMutationResult.success(
                ProviderWriteSandboxSupport.operationId("programmatic", request.provider(), request.mutationType(),
                        request.idempotencyKey(), request.dryRun()),
                ProviderWriteSandboxSupport.response(
                        "programmatic",
                        request.provider(),
                        request.mutationType(),
                        request.entityType(),
                        request.externalEntityId(),
                        request.idempotencyKey(),
                        request.dryRun(),
                        request.partialFailure(),
                        request.payload(),
                        request.metadata()));
    }
}
