package org.chovy.canvas.domain.bi.subscription;

/**
 * BiDeliveryAdapterResult record.
 * @param status 适配器处理状态，常见值为 DELIVERED、PENDING_ADAPTER 和 FAILED.
 * @param message 面向运维或订阅日志展示的投递说明.
 * @param errorMessage 失败时记录的错误详情，成功或等待异步适配时为空.
 */
public record BiDeliveryAdapterResult(
        String status,
        String message,
        String errorMessage
) {
    /**
     * 构造已投递成功的适配器结果。
     *
     * @param message 成功说明或外部消息 ID
     * @return 状态为 DELIVERED 的结果
     */
    public static BiDeliveryAdapterResult delivered(String message) {
        return new BiDeliveryAdapterResult("DELIVERED", message, null);
    }

    /**
     * 构造等待外部适配器异步完成的结果。
     *
     * @param message 排队、限流或异步回调说明
     * @return 状态为 PENDING_ADAPTER 的结果
     */
    public static BiDeliveryAdapterResult pending(String message) {
        return new BiDeliveryAdapterResult("PENDING_ADAPTER", message, null);
    }

    /**
     * 构造投递失败的适配器结果。
     *
     * @param message 失败摘要
     * @param errorMessage 外部渠道或适配器返回的详细错误
     * @return 状态为 FAILED 的结果
     */
    public static BiDeliveryAdapterResult failed(String message, String errorMessage) {
        return new BiDeliveryAdapterResult("FAILED", message, errorMessage);
    }
}
