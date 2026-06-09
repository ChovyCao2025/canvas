package org.chovy.canvas.domain.risk.modeling;

import java.time.Duration;
import java.util.Map;

/**
 * 风控模型调用请求。
 *
 * @param modelKey 模型业务键
 * @param modelVersion 模型版本号
 * @param endpoint 模型服务地址
 * @param timeout 调用超时时间
 * @param payload 调用载荷
 */
public record RiskModelClientCall(
        String modelKey,
        int modelVersion,
        String endpoint,
        Duration timeout,
        Map<String, Object> payload
) {
}
