package org.chovy.canvas.domain.creator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * CreatorProviderWriteGateway 编排 domain.creator 场景的领域业务规则。
 */
@Component
public class CreatorProviderWriteGateway {

    private final List<CreatorProviderWriteClient> clients;

    /**
     * 创建 CreatorProviderWriteGateway 实例并注入 domain.creator 场景依赖。
     * @param clients 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public CreatorProviderWriteGateway(ObjectProvider<CreatorProviderWriteClient> clients) {
        this(clients == null ? List.of() : clients.orderedStream().toList());
    }

    /**
     * 执行 CreatorProviderWriteGateway 流程，围绕 creator provider write gateway 完成校验、计算或结果组装。
     *
     * @param clients 依赖组件，用于完成数据访问或外部能力调用。
     */
    CreatorProviderWriteGateway(List<CreatorProviderWriteClient> clients) {
        this.clients = clients == null ? List.of() : List.copyOf(clients);
    }

    /**
     * unsupported 处理 domain.creator 场景的业务逻辑。
     * @return 返回 unsupported 流程生成的业务结果。
     */
    public static CreatorProviderWriteGateway unsupported() {
        return new CreatorProviderWriteGateway(List.of());
    }

    /**
     * execute 更新 domain.creator 场景的业务状态。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public CreatorProviderMutationResult execute(CreatorProviderMutationRequest request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request == null) {
            return CreatorProviderMutationResult.failure(
                    "INVALID_REQUEST",
                    "creator provider mutation request is required",
                    Map.of());
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        CreatorProviderWriteClient selectedClient = clients.stream()
                .filter(client -> client.supports(request))
                .findFirst()
                .orElse(null);
        if (selectedClient != null) {
            return selectedClient.execute(request);
        }
        if (request.dryRun()) {
            return CreatorProviderMutationResult.success("dry-run",
                    Map.of(
                            "validated", true,
                            "provider", request.provider(),
                            "mutationType", request.mutationType(),
                            "partialFailure", request.partialFailure()));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return CreatorProviderMutationResult.failure(
                "PROVIDER_CLIENT_UNAVAILABLE",
                "No live creator provider write client is registered for " + request.provider(),
                Map.of("provider", request.provider(), "mutationType", request.mutationType()));
    }
}
