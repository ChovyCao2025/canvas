package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.dal.dataobject.WebhookDeliveryLogDO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WebhookRetryPolicy {

    public record Decision(String status, LocalDateTime nextRetryAt, String terminalReason) {
    }

    public Decision classify(Integer httpStatus, boolean networkFailure, int attempt, int maxAttempts) {
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
        return new Decision(
                WebhookDeliveryLogDO.RETRYING,
                LocalDateTime.now().plusSeconds((long) Math.pow(2, Math.max(1, attempt))),
                null);
    }
}
