package org.chovy.canvas.domain.risk.modeling;

import java.util.Map;

/**
 * 风控模型评分请求。
 *
 * @param tenantId 租户编号
 * @param modelKey 模型业务键
 * @param features 模型特征输入
 * @param subject 主体属性
 */
public record RiskModelRequest(
        long tenantId,
        String modelKey,
        Map<String, Object> features,
        Map<String, Object> subject
) {
}
