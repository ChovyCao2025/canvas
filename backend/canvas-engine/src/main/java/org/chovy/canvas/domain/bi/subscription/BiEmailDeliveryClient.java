package org.chovy.canvas.domain.bi.subscription;

/**
 * BiEmailDeliveryClient 定义 domain.bi.subscription 场景中的扩展契约。
 */
public interface BiEmailDeliveryClient {

    /**
     * 执行 configured 流程，围绕 configured 完成校验、计算或结果组装。
     *
     * @return 返回 configured 的布尔判断结果。
     */
    boolean configured();

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     */
    void send(BiEmailDeliveryRequest request);
}
