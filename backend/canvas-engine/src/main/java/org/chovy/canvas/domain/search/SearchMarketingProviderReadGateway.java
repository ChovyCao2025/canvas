package org.chovy.canvas.domain.search;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SearchMarketingProviderReadGateway 编排 domain.search 场景的领域业务规则。
 */
@Component
public class SearchMarketingProviderReadGateway {

    private final List<SearchMarketingProviderReadClient> clients;

    /**
     * 创建 SearchMarketingProviderReadGateway 实例并注入 domain.search 场景依赖。
     * @param clients 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public SearchMarketingProviderReadGateway(ObjectProvider<SearchMarketingProviderReadClient> clients) {
        this(clients == null ? List.of() : clients.orderedStream().toList());
    }

    /**
     * 查询或读取业务数据。
     *
     * @param clients 依赖组件，用于完成数据访问或外部能力调用。
     */
    SearchMarketingProviderReadGateway(List<SearchMarketingProviderReadClient> clients) {
        this.clients = clients == null ? List.of() : List.copyOf(clients);
    }

    /**
     * unsupported 处理 domain.search 场景的业务逻辑。
     * @return 返回 unsupported 流程生成的业务结果。
     */
    public static SearchMarketingProviderReadGateway unsupported() {
        return new SearchMarketingProviderReadGateway(List.of());
    }

    /**
     * sync 创建或触发 domain.search 场景的业务处理。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param credential credential 参数，用于 sync 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public SearchMarketingProviderSyncResult sync(SearchMarketingSyncCommand command,
                                                  SearchMarketingCredentialRef credential) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            return SearchMarketingProviderSyncResult.failure(
                    "INVALID_SEARCH_SYNC_REQUEST",
                    "search marketing sync command is required",
                    false,
                    Map.of());
        }
        if (credential == null || !credential.available()) {
            return SearchMarketingProviderSyncResult.failure(
                    "SEARCH_PROVIDER_CREDENTIAL_UNAVAILABLE",
                    credential == null ? "search provider credential is unavailable" : credential.errorMessage(),
                    false,
                    Map.of("provider", command.provider(), "runType", command.runType()));
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        SearchMarketingProviderReadClient selectedClient = clients.stream()
                .filter(client -> client.supports(command.provider(), command.runType()))
                .findFirst()
                .orElse(null);
        if (selectedClient == null) {
            return SearchMarketingProviderSyncResult.failure(
                    "SEARCH_READ_CLIENT_UNAVAILABLE",
                    "No search marketing provider read client is registered for " + command.provider(),
                    false,
                    Map.of("provider", command.provider(), "runType", command.runType()));
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return selectedClient.sync(command, credential);
    }
}
