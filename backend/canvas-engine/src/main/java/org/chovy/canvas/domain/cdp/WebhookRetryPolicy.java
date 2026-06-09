package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.dal.dataobject.WebhookDeliveryLogDO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * WebhookRetryPolicy 编排 domain.cdp 场景的领域业务规则。
 */
@Service
public class WebhookRetryPolicy {

    /**
     * Decision 数据记录。
     */
    public record Decision(String status, LocalDateTime nextRetryAt, String terminalReason) {
    }

    /**
     * 根据 HTTP 结果和尝试次数判定 Webhook 投递后续状态。
     *
     * <p>2xx 结果判定成功；网络失败、空状态、429 和 5xx 视为可重试；其它 4xx 直接失败。
     * 可重试但已达到最大次数时进入 DEAD，否则按尝试次数计算指数退避的下一次重试时间。</p>
     *
     * @param httpStatus Webhook 回调 HTTP 状态码，网络失败时可为空
     * @param networkFailure 是否发生连接、超时或其它网络异常
     * @param attempt 当前投递尝试次数
     * @param maxAttempts 允许的最大尝试次数
     * @return 投递状态、下一次重试时间和终止原因
     */
    public Decision classify(Integer httpStatus, boolean networkFailure, int attempt, int maxAttempts) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!networkFailure && httpStatus != null && httpStatus >= 200 && httpStatus < 300) {
            return new Decision(WebhookDeliveryLogDO.SUCCESS, null, null);
        }
        boolean retryable = networkFailure || httpStatus == null || httpStatus == 429 || httpStatus >= 500;
        if (!retryable) {
            return new Decision(WebhookDeliveryLogDO.FAILED, null, "HTTP_" + httpStatus);
        }
        if (attempt >= maxAttempts) {
            return new Decision(WebhookDeliveryLogDO.DEAD, null, "MAX_ATTEMPTS_REACHED");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new Decision(
                WebhookDeliveryLogDO.RETRYING,
                LocalDateTime.now().plusSeconds((long) Math.pow(2, Math.max(1, attempt))),
                null);
    }
}
