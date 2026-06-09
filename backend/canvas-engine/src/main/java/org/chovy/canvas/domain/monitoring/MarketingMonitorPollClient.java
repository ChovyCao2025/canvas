package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorPollClient 定义 domain.monitoring 场景中的扩展契约。
 */
public interface MarketingMonitorPollClient {

    /**
     * 判断业务条件是否成立。
     *
     * @param sourceType 类型标识，用于选择对应处理分支。
     * @return 返回 supports 的布尔判断结果。
     */
    boolean supports(String sourceType);

    /**
     * 执行 fetch 流程，围绕 fetch 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 fetch 流程生成的业务结果。
     */
    MarketingMonitorPollResponse fetch(MarketingMonitorPollRequest request);
}
