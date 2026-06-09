package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorInferenceGenerator 定义 domain.monitoring 场景中的扩展契约。
 */
public interface MarketingMonitorInferenceGenerator {

    /**
     * 执行 generate 流程，围绕 generate 完成校验、计算或结果组装。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 generate 流程生成的业务结果。
     */
    MarketingMonitorInferenceGenerationResult generate(MarketingMonitorInferenceGenerationContext context);
}
