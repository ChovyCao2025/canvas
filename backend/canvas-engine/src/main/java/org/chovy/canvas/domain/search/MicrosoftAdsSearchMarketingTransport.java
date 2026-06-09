package org.chovy.canvas.domain.search;

/**
 * MicrosoftAdsSearchMarketingTransport 定义 domain.search 场景中的扩展契约。
 */
public interface MicrosoftAdsSearchMarketingTransport {

    /**
     * 执行 mutate 流程，围绕 mutate 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param credential credential 参数，用于 mutate 流程中的校验、计算或对象转换。
     * @return 返回 mutate 流程生成的业务结果。
     */
    SearchMarketingProviderMutationResult mutate(SearchMarketingProviderMutationRequest request,
                                                 SearchMarketingCredentialRef credential);
}
