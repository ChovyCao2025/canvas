package org.chovy.canvas.domain.risk.modeling;

import java.time.Duration;
import java.util.Map;

/**
 * 已注册模型端点元数据，包含兜底分和是否允许发送原始主体 PII。
 *
 * @param modelKey 模型业务键
 * @param version 模型版本号
 * @param active 是否为活跃版本
 * @param endpoint 模型服务地址
 * @param timeout 调用超时时间
 * @param fallbackScore 超时或降级兜底分
 * @param rawPiiApproved 是否允许发送原始主体 PII
 * @param inputSchema 输入字段结构
 * @param outputSchema 输出字段结构
 */
public record RiskModelDefinition(
        String modelKey,
        int version,
        boolean active,
        String endpoint,
        Duration timeout,
        int fallbackScore,
        boolean rawPiiApproved,
        Map<String, String> inputSchema,
        Map<String, String> outputSchema
) {

    /**
     * 返回替换兜底分后的模型定义副本。
     */
    public RiskModelDefinition withFallbackScore(int fallbackScore) {
        return new RiskModelDefinition(
                modelKey,
                version,
                active,
                endpoint,
                timeout,
                fallbackScore,
                rawPiiApproved,
                inputSchema,
                outputSchema);
    }
}
