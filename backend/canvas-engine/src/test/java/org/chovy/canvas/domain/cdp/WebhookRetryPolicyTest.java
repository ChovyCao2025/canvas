package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.dal.dataobject.WebhookDeliveryLogDO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookRetryPolicyTest {

    private final WebhookRetryPolicy policy = new WebhookRetryPolicy();

    @Test
    void retriesNetworkFailuresHttp429AndHttp5xx() {
        assertThat(policy.classify(null, true, 1, 3).status()).isEqualTo(WebhookDeliveryLogDO.RETRYING);
        assertThat(policy.classify(429, false, 1, 3).status()).isEqualTo(WebhookDeliveryLogDO.RETRYING);
        assertThat(policy.classify(503, false, 1, 3).status()).isEqualTo(WebhookDeliveryLogDO.RETRYING);
    }

    @Test
    void marksNon429Http4xxAsFailed() {
        WebhookRetryPolicy.Decision decision = policy.classify(400, false, 1, 3);

        assertThat(decision.status()).isEqualTo(WebhookDeliveryLogDO.FAILED);
        assertThat(decision.nextRetryAt()).isNull();
        assertThat(decision.terminalReason()).contains("HTTP_400");
    }

    @Test
    void marksDeadWhenMaxAttemptsReached() {
        WebhookRetryPolicy.Decision decision = policy.classify(500, false, 3, 3);

        assertThat(decision.status()).isEqualTo(WebhookDeliveryLogDO.DEAD);
        assertThat(decision.nextRetryAt()).isNull();
        assertThat(decision.terminalReason()).isEqualTo("MAX_ATTEMPTS_REACHED");
    }
}
