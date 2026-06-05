package org.chovy.canvas.domain.cdp;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSubscriptionSchemaTest {

    @Test
    void migrationCreatesSubscriptionsAndDeliveryLogs() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V103__webhook_subscription_schema.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS webhook_subscription")
                .contains("event_types")
                .contains("secret_hash")
                .contains("secret_ciphertext")
                .contains("CREATE TABLE IF NOT EXISTS webhook_delivery_log")
                .contains("delivery_id")
                .contains("attempt")
                .contains("terminal_reason")
                .contains("idx_webhook_retry");
    }
}
