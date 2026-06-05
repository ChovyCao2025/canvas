package org.chovy.canvas.domain.cdp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookSubscriptionValidatorTest {

    @Test
    void rejectsLocalhostCallbackUrl() {
        WebhookSubscriptionValidator validator = new WebhookSubscriptionValidator();

        assertThatThrownBy(() -> validator.validate("http://localhost:8080/hook", List.of("cdp.event.ingested")))
                .hasMessageContaining("callbackUrl is not allowed");
    }

    @Test
    void rejectsBlankEventTypeList() {
        WebhookSubscriptionValidator validator = new WebhookSubscriptionValidator();

        assertThatThrownBy(() -> validator.validate("http://93.184.216.34/hook", List.of()))
                .hasMessageContaining("eventTypes cannot be empty");
    }

    @Test
    void acceptsPublicCallbackAndNonblankEventTypes() {
        WebhookSubscriptionValidator validator = new WebhookSubscriptionValidator();

        assertThatCode(() -> validator.validate("http://93.184.216.34/hook", List.of("cdp.event.ingested")))
                .doesNotThrowAnyException();
    }
}
