package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasAttributionSchemaTest {

    @Test
    void migrationAddsConfigurableMultiTouchAttributionColumns() throws IOException {
        InputStream migration = getClass().getResourceAsStream(
                "/db/migration/V274__attribution_multi_touch_models.sql");

        assertThat(migration).isNotNull();
        String sql = new String(migration.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(sql).contains("ADD COLUMN `attribution_model` VARCHAR(32) NOT NULL DEFAULT 'LAST_TOUCH'");
        assertThat(sql).contains("ADD COLUMN `attribution_weight` DECIMAL(12,8) NOT NULL DEFAULT 1.00000000");
        assertThat(sql).contains("ADD COLUMN `touch_created_at` DATETIME NULL");
        assertThat(sql).contains("SET `send_record_id` = 0");
        assertThat(sql).contains("MODIFY COLUMN `send_record_id` BIGINT NOT NULL DEFAULT 0");
        assertThat(sql).contains("DROP INDEX `uk_canvas_attr_event`");
        assertThat(sql).contains("UNIQUE KEY `uk_canvas_attr_event_model_touch`");
    }
}
