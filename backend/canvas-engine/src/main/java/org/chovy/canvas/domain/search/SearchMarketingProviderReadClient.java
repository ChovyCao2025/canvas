package org.chovy.canvas.domain.search;

/**
 * SearchMarketingProviderReadClient 定义 domain.search 场景中的扩展契约。
 */
public interface SearchMarketingProviderReadClient {

    /**
     * 判断业务条件是否成立。
     *
     * @param provider provider 参数，用于 supports 流程中的校验、计算或对象转换。
     * @param runType 类型标识，用于选择对应处理分支。
     * @return 返回 supports 的布尔判断结果。
     */
    boolean supports(String provider, String runType);

    /**
     * 执行核心业务处理流程。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param credential credential 参数，用于 sync 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    SearchMarketingProviderSyncResult sync(SearchMarketingSyncCommand command,
                                           SearchMarketingCredentialRef credential);
}
