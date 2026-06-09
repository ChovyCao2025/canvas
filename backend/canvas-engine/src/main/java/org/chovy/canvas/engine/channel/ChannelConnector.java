package org.chovy.canvas.engine.channel;

import java.util.Map;

/**
 * ChannelConnector 定义 engine.channel 场景中的扩展契约。
 */
public interface ChannelConnector {

    /**
     * 返回连接器运行模式。
     *
     * @return 真实、沙箱或禁用模式
     */
    ConnectorMode mode();

    /**
     * 查询连接器健康状态。
     *
     * @return 连接器健康状态
     */
    ConnectorHealth health();

    /**
     * 查询连接器能力声明。
     *
     * @return 发送、回执和扩展属性能力
     */
    ConnectorCapabilities capabilities();

    /**
     * 发送渠道消息。
     *
     * @param request 渠道发送请求
     * @return 渠道发送结果
     */
    ConnectorSendResult send(ConnectorSendRequest request);

    /**
     * 解析渠道回执载荷。
     *
     * @param rawPayload 供应商原始回执
     * @return 标准化回执结果
     */
    ConnectorReceiptResult parseReceipt(Map<String, Object> rawPayload);

    /**
     * 渠道连接器运行模式。
     */
    enum ConnectorMode {
        REAL,
        SANDBOX,
        DISABLED
    }

    /**
     * 渠道连接器健康状态。
     *
     * @param status 状态值
     * @param message 状态说明
     */
    record ConnectorHealth(String status, String message) {
    }

    /**
     * 渠道连接器能力声明。
     *
     * @param send 是否支持发送
     * @param receipt 是否支持回执解析
     * @param attributes 扩展能力属性
     */
    record ConnectorCapabilities(boolean send, boolean receipt, Map<String, Object> attributes) {
    }

    /**
     * 渠道消息发送请求。
     *
     * @param tenantId 租户 ID
     * @param channel 渠道标识
     * @param provider 供应商标识
     * @param userId 目标用户标识
     * @param payload 发送载荷
     */
    record ConnectorSendRequest(
            Long tenantId,
            String channel,
            String provider,
            String userId,
            Map<String, Object> payload) {
    }

    /**
     * 渠道消息发送结果。
     *
     * @param accepted 是否已被供应商接受
     * @param externalMessageId 供应商消息 ID
     * @param status 发送状态
     * @param reason 失败或跳过原因
     */
    record ConnectorSendResult(boolean accepted, String externalMessageId, String status, String reason) {
    }

    /**
     * 渠道回执解析结果。
     *
     * @param externalMessageId 供应商消息 ID
     * @param status 回执状态
     * @param attributes 回执扩展属性
     */
    record ConnectorReceiptResult(String externalMessageId, String status, Map<String, Object> attributes) {
    }
}
