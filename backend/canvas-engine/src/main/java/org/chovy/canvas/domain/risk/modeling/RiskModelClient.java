package org.chovy.canvas.domain.risk.modeling;

/**
 * 风控模型客户端。
 */
public interface RiskModelClient {

    /**
     * 调用模型评分接口并返回原始响应 JSON。
     */
    String score(RiskModelClientCall call);
}
