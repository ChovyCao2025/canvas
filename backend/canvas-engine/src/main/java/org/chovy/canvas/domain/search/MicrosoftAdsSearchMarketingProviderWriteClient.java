package org.chovy.canvas.domain.search;

import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MicrosoftAdsSearchMarketingProviderWriteClient 编排 domain.search 场景的领域业务规则。
 */
@Component
public class MicrosoftAdsSearchMarketingProviderWriteClient implements SearchMarketingProviderWriteClient {

    private final SearchMarketingCredentialResolver credentialResolver;
    private final MicrosoftAdsSearchMarketingTransport transport;

    /**
     * 创建 MicrosoftAdsSearchMarketingProviderWriteClient 实例并注入 domain.search 场景依赖。
     * @param credentialResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param transports transports 参数，用于 MicrosoftAdsSearchMarketingProviderWriteClient 流程中的校验、计算或对象转换。
     */
    @Autowired
    public MicrosoftAdsSearchMarketingProviderWriteClient(SearchMarketingCredentialResolver credentialResolver,
                                                         ObjectProvider<MicrosoftAdsSearchMarketingTransport> transports) {
        this(credentialResolver, transports == null ? null : transports.getIfAvailable());
    }

    /**
     * 执行 MicrosoftAdsSearchMarketingProviderWriteClient 流程，围绕 microsoft ads search marketing provider write client 完成校验、计算或结果组装。
     *
     * @param credentialResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param transport transport 参数，用于 MicrosoftAdsSearchMarketingProviderWriteClient 流程中的校验、计算或对象转换。
     */
    MicrosoftAdsSearchMarketingProviderWriteClient(SearchMarketingCredentialResolver credentialResolver,
                                                   MicrosoftAdsSearchMarketingTransport transport) {
        this.credentialResolver = credentialResolver;
        this.transport = transport == null ? unavailableTransport() : transport;
    }

    /**
     * supports 处理 domain.search 场景的业务逻辑。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 supports 的布尔判断结果。
     */
    @Override
    public boolean supports(SearchMarketingProviderMutationRequest request) {
        return request != null && "MICROSOFT_ADS".equalsIgnoreCase(request.provider());
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
                    "INVALID_REQUEST", "search marketing mutation request is required", Map.of());
        }
        SearchMarketingCredentialRef credential = credentialResolver.resolve(
                request.tenantId(),
                request.provider(),
                credentialKey(request.metadata()));
        if (credential == null || !credential.available()) {
            return SearchMarketingProviderMutationResult.failure(
                    "SEARCH_PROVIDER_CREDENTIAL_UNAVAILABLE",
                    credential == null ? "search provider credential is unavailable" : credential.errorMessage(),
                    credential == null ? Map.of("provider", request.provider()) : credential.safeEvidence());
        }
        if (request.dryRun() && !supportsProviderValidation(request)) {
            return SearchMarketingProviderMutationResult.success(
                    "microsoft-ads-local-validation-" + request.idempotencyKey(),
                    Map.of(
                            "validated", true,
                            "validationMode", "LOCAL",
                            "provider", request.provider(),
                            "mutationType", request.mutationType()));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return sanitize(transport.mutate(request, credential));
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 supports provider validation 的布尔判断结果。
     */
    private boolean supportsProviderValidation(SearchMarketingProviderMutationRequest request) {
        return "UPDATE_CAMPAIGN_BUDGET".equals(request.mutationType());
    }

    /**
     * 执行 credentialKey 流程，围绕 credential key 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 credentialKey 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 credentialKey 流程中的校验、计算或对象转换。
     * @return 返回 credential key 生成的文本或业务键。
     */
    private String credentialKey(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get("credentialKey");
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 执行 sanitize 流程，围绕 sanitize 完成校验、计算或结果组装。
     *
     * @param result result 参数，用于 sanitize 流程中的校验、计算或对象转换。
     * @return 返回 sanitize 流程生成的业务结果。
     */
    private SearchMarketingProviderMutationResult sanitize(SearchMarketingProviderMutationResult result) {
        if (result == null) {
            return SearchMarketingProviderMutationResult.failure(
                    "SEARCH_PROVIDER_EMPTY_RESPONSE",
                    "search provider returned no mutation response",
                    Map.of());
        }
        Map<String, Object> response = ProviderWriteEvidenceSanitizer.sanitizeMap(result.response());
        return result.success()
                ? SearchMarketingProviderMutationResult.success(result.providerOperationId(), response)
                : SearchMarketingProviderMutationResult.failure(result.errorCode(), result.errorMessage(), response);
    }

    /**
     * 执行 unavailableTransport 流程，围绕 unavailable transport 完成校验、计算或结果组装。
     *
     * @return 返回 unavailableTransport 流程生成的业务结果。
     */
    private MicrosoftAdsSearchMarketingTransport unavailableTransport() {
        return (request, credential) -> SearchMarketingProviderMutationResult.failure(
                "MICROSOFT_ADS_TRANSPORT_UNAVAILABLE",
                "Microsoft Ads search marketing transport is not configured",
                Map.of("provider", request.provider(), "mutationType", request.mutationType()));
    }
}
