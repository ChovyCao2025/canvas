package org.chovy.canvas.domain.search;

import org.chovy.canvas.domain.providerwrite.ProviderWriteSandboxSupport;
import org.springframework.stereotype.Component;

/**
 * SandboxSearchMarketingProviderWriteClient 编排 domain.search 场景的领域业务规则。
 */
@Component
public class SandboxSearchMarketingProviderWriteClient implements SearchMarketingProviderWriteClient {

    /**
     * supports 处理 domain.search 场景的业务逻辑。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 supports 的布尔判断结果。
     */
    @Override
    public boolean supports(SearchMarketingProviderMutationRequest request) {
        return request != null && ProviderWriteSandboxSupport.supportsSandboxProvider(request.provider());
    }

    /**
     * execute 更新 domain.search 场景的业务状态。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    @Override
    public SearchMarketingProviderMutationResult execute(SearchMarketingProviderMutationRequest request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request == null) {
            return SearchMarketingProviderMutationResult.failure(
                    "INVALID_REQUEST",
                    "search marketing provider mutation request is required",
                    java.util.Map.of());
        }
        if (!supports(request)) {
            return SearchMarketingProviderMutationResult.failure(
                    "UNSUPPORTED_PROVIDER",
                    "Sandbox search marketing client only supports sandbox providers",
                    java.util.Map.of("provider", request.provider()));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return SearchMarketingProviderMutationResult.success(
                ProviderWriteSandboxSupport.operationId("search", request.provider(), request.mutationType(),
                        request.idempotencyKey(), request.dryRun()),
                ProviderWriteSandboxSupport.response(
                        "search",
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
