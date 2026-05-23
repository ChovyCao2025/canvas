package org.chovy.canvas.perf.mq;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerfMqProducerTest {

    @Test
    void sourceMsgIdIsDeterministic() {
        assertThat(PerfMqProducer.sourceMsgId("perf_20260523_001", 12))
                .isEqualTo("perf_20260523_001:mq:12");
    }

    @Test
    void messageBodyContainsPerfRunIdAndInputId() {
        String body = PerfMqProducer.messageBody("perf_20260523_001", "perf_user_2", 12);

        assertThat(body)
                .contains("\"userId\":\"perf_user_2\"")
                .contains("\"messageCode\":\"PERF_MQ\"")
                .contains("\"perfRunId\":\"perf_20260523_001\"")
                .contains("\"perfInputId\":\"perf_20260523_001:mq:12\"");
    }

    @Test
    void zeroCountDoesNotNeedRocketMqProducer() {
        PerfMqProducer.Config config = PerfMqProducer.Config.fromArgs(new String[]{
                "--perf-run-id", "perf_test",
                "--count", "0"
        });

        assertThat(config.shouldSend()).isFalse();
    }

    @Test
    void negativeCountIsRejected() {
        assertThatThrownBy(() -> PerfMqProducer.Config.fromArgs(new String[]{
                "--perf-run-id", "perf_test",
                "--count", "-1"
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--count must be >= 0");
    }

    @Test
    void userModuloMustBePositive() {
        assertThatThrownBy(() -> PerfMqProducer.Config.fromArgs(new String[]{
                "--perf-run-id", "perf_test",
                "--user-modulo", "0"
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--user-modulo must be > 0");
    }

    @Test
    void messageBodyEscapesJsonControlCharacters() {
        String body = PerfMqProducer.messageBody("perf\n\t\"\\", "perf_user\n\t\"\\", 12);

        assertThat(body)
                .contains("\"userId\":\"perf_user\\n\\t\\\"\\\\\"")
                .contains("\"perfRunId\":\"perf\\n\\t\\\"\\\\\"")
                .doesNotContain("\n")
                .doesNotContain("\t");
    }

    @Test
    void sequencesAreOneBasedThroughCount() {
        assertThat(PerfMqProducer.sequences(3))
                .containsExactly(1, 2, 3);
    }
}
