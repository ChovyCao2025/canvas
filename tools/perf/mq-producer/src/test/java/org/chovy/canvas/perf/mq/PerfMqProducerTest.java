package org.chovy.canvas.perf.mq;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
