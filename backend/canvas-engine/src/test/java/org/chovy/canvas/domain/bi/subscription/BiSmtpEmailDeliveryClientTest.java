package org.chovy.canvas.domain.bi.subscription;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BiSmtpEmailDeliveryClientTest {

    @Test
    void configuredRequiresEnabledHostAndSender() {
        assertThat(client(false, "smtp.example.test", "bi@example.test").configured()).isFalse();
        assertThat(client(true, "", "bi@example.test").configured()).isFalse();
        assertThat(client(true, "smtp.example.test", "").configured()).isFalse();
        assertThat(client(true, "smtp.example.test", "bi@example.test").configured()).isTrue();
    }

    @Test
    void sendRejectsEmptyRecipientsBeforeNetworkCall() {
        BiSmtpEmailDeliveryClient client = client(true, "smtp.example.test", "bi@example.test");

        assertThatThrownBy(() -> client.send(new BiEmailDeliveryRequest(
                        "bi@example.test",
                        List.of(),
                        "Canvas Daily",
                        "BI subscription delivery is ready")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recipients");
    }

    @Test
    void smtpMessageUsesMultipartMixedWhenAttachmentsPresent() throws Exception {
        BiSmtpEmailDeliveryClient client = client(true, "smtp.example.test", "bi@example.test");
        Method message = BiSmtpEmailDeliveryClient.class
                .getDeclaredMethod("message", String.class, BiEmailDeliveryRequest.class);
        message.setAccessible(true);

        String raw = (String) message.invoke(client,
                "bi@example.test",
                new BiEmailDeliveryRequest(
                        "bi@example.test",
                        List.of("alice@example.test"),
                        "Canvas Daily",
                        "BI subscription delivery is ready",
                        List.of(new BiEmailAttachment(
                                "canvas-daily.csv",
                                "text/csv; charset=UTF-8",
                                "jobKey,canvas-daily\n".getBytes(StandardCharsets.UTF_8)))));

        assertThat(raw).contains("Content-Type: multipart/mixed");
        assertThat(raw).contains("Content-Disposition: attachment; filename=\"canvas-daily.csv\"");
        assertThat(raw).contains("am9iS2V5LGNhbnZhcy1kYWlseQo=");
    }

    private BiSmtpEmailDeliveryClient client(boolean enabled, String host, String from) {
        return new BiSmtpEmailDeliveryClient(
                enabled,
                host,
                25,
                "",
                "",
                from,
                false,
                false,
                1000);
    }
}
