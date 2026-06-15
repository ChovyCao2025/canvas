package org.chovy.canvas.platform.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.domain.ChannelConnectorCatalog;
import org.junit.jupiter.api.Test;

class ChannelConnectorApplicationServiceTest {

    @Test
    void exposesDeterministicLegacyChannelConnectorViews() {
        ChannelConnectorApplicationService service =
                new ChannelConnectorApplicationService(new ChannelConnectorCatalog());

        List<Map<String, Object>> connectors = service.connectors(42L);
        assertThat(connectors)
                .extracting(item -> item.get("connectorKey"))
                .containsExactly("email-sendgrid", "sms-twilio");
        assertThat(connectors.getFirst())
                .containsEntry("id", 1001L)
                .containsEntry("channel", "EMAIL")
                .containsEntry("provider", "SENDGRID")
                .containsEntry("mode", "SANDBOX")
                .containsEntry("healthStatus", "UNKNOWN");

        assertThat(service.limits(42L))
                .first()
                .satisfies(limit -> assertThat(limit)
                        .containsEntry("channel", "EMAIL")
                        .containsEntry("provider", "SENDGRID")
                        .containsEntry("operation", "SEND")
                        .containsEntry("perSecondLimit", 50)
                        .containsEntry("dailyLimit", 100000L)
                        .containsEntry("failClosed", true));

        assertThat(service.fallbackDecisions(42L))
                .first()
                .satisfies(decision -> assertThat(decision)
                        .containsEntry("originalChannel", "EMAIL")
                        .containsEntry("finalChannel", "SMS")
                        .containsEntry("decisionReason", "provider limit"));

        assertThat(service.dedupeRecords(42L))
                .first()
                .satisfies(record -> assertThat(record)
                        .containsEntry("dedupeGroup", "campaign-1")
                        .containsEntry("channel", "EMAIL")
                        .containsEntry("userId", "user-1"));
    }

    @Test
    void normalizesModeUpdatesHealthTestsAndFallbackValidation() {
        ChannelConnectorApplicationService service =
                new ChannelConnectorApplicationService(new ChannelConnectorCatalog());

        Map<String, Object> disabled = service.updateMode(42L, 1002L,
                Map.of("mode", " disabled ", "reason", "provider outage"), "operator-2");
        assertThat(disabled)
                .containsEntry("id", 1002L)
                .containsEntry("mode", "DISABLED")
                .containsEntry("disabledReason", "provider outage")
                .containsEntry("operator", "operator-2");
        assertThat(service.healthTest(42L, 1002L))
                .containsEntry("status", "DISABLED")
                .containsEntry("message", "provider outage");

        assertThat(service.updateMode(42L, 1002L, Map.of("mode", "sandbox"), " "))
                .containsEntry("mode", "SANDBOX")
                .containsEntry("disabledReason", null)
                .containsEntry("operator", "operator-1");
        assertThat(service.healthTest(42L, 1002L))
                .containsEntry("status", "UP")
                .containsEntry("message", "sandbox connector ready");

        assertThat(service.validateFallback(42L, Map.of(
                "channel", "whatsapp",
                "provider", "meta",
                "fallbackChannel", "sms",
                "fallbackProvider", "twilio")))
                .containsEntry("valid", true)
                .containsEntry("message", "ok");

        assertThat(service.validateFallback(42L, Map.of(
                "channel", "email",
                "provider", "sendgrid",
                "fallbackChannel", "email",
                "fallbackProvider", "sendgrid")))
                .containsEntry("valid", false)
                .containsEntry("message", "fallback connector must differ from primary connector");
    }
}
