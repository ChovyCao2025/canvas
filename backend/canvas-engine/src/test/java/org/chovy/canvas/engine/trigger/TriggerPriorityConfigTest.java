package org.chovy.canvas.engine.trigger;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TriggerPriorityConfigTest {

    @Test
    void defaultsClassifyTriggerTypesByPriority() {
        TriggerPriorityConfig config = new TriggerPriorityConfig();

        assertThat(config.of(null)).isEqualTo(TriggerPriorityConfig.Priority.NORMAL);
        assertThat(config.of("DIRECT_CALL")).isEqualTo(TriggerPriorityConfig.Priority.HIGH);
        assertThat(config.of("SCHEDULED")).isEqualTo(TriggerPriorityConfig.Priority.LOW);
        assertThat(config.of("UNKNOWN")).isEqualTo(TriggerPriorityConfig.Priority.NORMAL);
    }

    @Test
    void defaultsExposeConcurrencyAndRetrySettings() {
        TriggerPriorityConfig config = new TriggerPriorityConfig();

        assertThat(config.getLowRatio()).isEqualTo(0.5);
        assertThat(config.getHighMaxConcurrencyRatio()).isEqualTo(2.0);
        assertThat(config.getOverflowRetryDelayMs()).isEqualTo(5000L);
        assertThat(config.getOverflowMaxRetry()).isEqualTo(3);
    }

    @Test
    void configuredListsOverrideDefaultClassification() {
        TriggerPriorityConfig config = new TriggerPriorityConfig();
        config.setHigh(List.of("VIP"));
        config.setLow(List.of("BATCH"));

        assertThat(config.of("VIP")).isEqualTo(TriggerPriorityConfig.Priority.HIGH);
        assertThat(config.of("BATCH")).isEqualTo(TriggerPriorityConfig.Priority.LOW);
        assertThat(config.of("DIRECT_CALL")).isEqualTo(TriggerPriorityConfig.Priority.NORMAL);
    }
}
