package org.chovy.canvas.domain.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SearchMarketingProviderWriteGateway 编排 domain.search 场景的领域业务规则。
 */
@Component
public class SearchMarketingProviderWriteGateway {

    private final List<SearchMarketingProviderWriteClient> clients;

    /**
     * 创建 SearchMarketingProviderWriteGateway 实例并注入 domain.search 场景依赖。
     * @param clients 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public SearchMarketingProviderWriteGateway(ObjectProvider<SearchMarketingProviderWriteClient> clients) {
        this(clients == null ? List.of() : clients.orderedStream().toList());
    }

    /**
     * 查询或读取业务数据。
     *
     * @param clients 依赖组件，用于完成数据访问或外部能力调用。
     */
    SearchMarketingProviderWriteGateway(List<SearchMarketingProviderWriteClient> clients) {
        this.clients = clients == null ? List.of() : List.copyOf(clients);
    }

    /**
     * unsupported 处理 domain.search 场景的业务逻辑。
     * @return 返回 unsupported 流程生成的业务结果。
     */
    public static SearchMarketingProviderWriteGateway unsupported() {
        return new SearchMarketingProviderWriteGateway(List.of());
    }

    /**
     * supportsLiveApply 处理 domain.search 场景的业务逻辑。
     * @param provider provider 参数，用于 supportsLiveApply 流程中的校验、计算或对象转换。
     * @return 返回 supports live apply 的布尔判断结果。
     */
    public boolean supportsLiveApply(String provider) {
        if (provider == null || provider.isBlank()) {
            return false;
        }
        SearchMarketingProviderMutationRequest probe = new SearchMarketingProviderMutationRequest(
                0L,
                0L,
                provider,
                "readiness-probe",
                null,
                "UPDATE_KEYWORD_BID",
                "KEYWORD",
                "readiness-probe",
                "readiness-probe",
                false,
                true,
                Map.of(),
                Map.of());
        return clients.stream().anyMatch(client -> client.supports(probe));
    }

    /**
     * execute 更新 domain.search 场景的业务状态。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public SearchMarketingProviderMutationResult execute(SearchMarketingProviderMutationRequest request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request == null) {
            return SearchMarketingProviderMutationResult.failure(
                    "INVALID_REQUEST",
                    "provider mutation request is required",
                    Map.of());
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        SearchMarketingProviderWriteClient selectedClient = clients.stream()
                .filter(client -> client.supports(request))
                .findFirst()
                .orElse(null);
        if (selectedClient != null) {
            return selectedClient.execute(request);
        }
        if (request.dryRun()) {
            return SearchMarketingProviderMutationResult.success("dry-run",
                    Map.of(
                            "validated", true,
                            "provider", request.provider(),
                            "mutationType", request.mutationType(),
                            "partialFailure", request.partialFailure()));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return SearchMarketingProviderMutationResult.failure(
                "PROVIDER_CLIENT_UNAVAILABLE",
                "No live search marketing provider write client is registered for " + request.provider(),
                Map.of("provider", request.provider(), "mutationType", request.mutationType()));
    }
}
