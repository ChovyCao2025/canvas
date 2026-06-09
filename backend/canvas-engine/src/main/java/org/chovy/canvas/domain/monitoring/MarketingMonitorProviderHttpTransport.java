package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorProviderHttpTransport 定义 domain.monitoring 场景中的扩展契约。
 */
public interface MarketingMonitorProviderHttpTransport {

    /**
     * 执行核心业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    MarketingMonitorProviderHttpResponse execute(MarketingMonitorProviderHttpRequest request);
}
