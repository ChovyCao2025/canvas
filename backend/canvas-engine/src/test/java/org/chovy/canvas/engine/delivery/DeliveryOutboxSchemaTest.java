package org.chovy.canvas.engine.delivery;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryOutboxSchemaTest {

    @Test
    void deliveryOutboxMigrationCreatesCrashSafeDeliveryTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V94__delivery_outbox_receipts.sql"));

        assertThat(sql)
                .contains("CREATE TABLE `delivery_outbox`")
                .contains("`tenant_id`")
                .contains("`message_send_record_id`")
                .contains("`idempotency_key`")
                .contains("UNIQUE KEY `uk_delivery_outbox_tenant_idempotency`")
                .contains("KEY `idx_delivery_outbox_status_retry`")
                .contains("CREATE TABLE `delivery_receipt_log`")
                .contains("UNIQUE KEY `uk_delivery_receipt_tenant_idempotency`");
    }

    @Test
    void sideEffectMigrationCreatesNodeScopedIdempotencyTable() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V95__node_side_effect_idempotency.sql"));

        assertThat(sql)
                .contains("CREATE TABLE `node_side_effect_idempotency`")
                .contains("`tenant_id`")
                .contains("`execution_id`")
                .contains("`canvas_id`")
                .contains("`node_id`")
                .contains("`node_type`")
                .contains("`operation_key`")
                .contains("`idempotency_key`")
                .contains("UNIQUE KEY `uk_node_side_effect_tenant_key`")
                .contains("KEY `idx_node_side_effect_execution`");
    }
}
