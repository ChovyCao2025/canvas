package org.chovy.canvas.engine.channel;

import java.util.Map;

/**
 * DisabledChannelConnector 参与 engine.channel 场景的画布执行引擎处理。
 */
public class DisabledChannelConnector implements ChannelConnector {

    private final String reason;

    /**
     * 创建 DisabledChannelConnector 实例并注入 engine.channel 场景依赖。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     */
    public DisabledChannelConnector(String reason) {
        this.reason = reason == null || reason.isBlank() ? "connector disabled" : reason;
    }

    /**
     * mode 处理 engine.channel 场景的业务逻辑。
     * @return 返回 mode 流程生成的业务结果。
     */
    @Override
    public ConnectorMode mode() {
        return ConnectorMode.DISABLED;
    }

    /**
     * health 查询 engine.channel 场景的业务数据。
     * @return 返回 health 流程生成的业务结果。
     */
    @Override
    public ConnectorHealth health() {
        return new ConnectorHealth("DISABLED", reason);
    }

    /**
     * capabilities 处理 engine.channel 场景的业务逻辑。
     * @return 返回 capabilities 流程生成的业务结果。
     */
    @Override
    public ConnectorCapabilities capabilities() {
        return new ConnectorCapabilities(false, false, Map.of());
    }

    /**
     * send 创建或触发 engine.channel 场景的业务处理。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 send 流程生成的业务结果。
     */
    @Override
    public ConnectorSendResult send(ConnectorSendRequest request) {
        return new ConnectorSendResult(false, null, "DISABLED", reason);
    }

    /**
     * parseReceipt 校验或转换 engine.channel 场景的数据。
     * @param rawPayload raw payload 参数，用于 parseReceipt 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    @Override
    public ConnectorReceiptResult parseReceipt(Map<String, Object> rawPayload) {
        return new ConnectorReceiptResult(null, "UNSUPPORTED", Map.of());
    }
}
