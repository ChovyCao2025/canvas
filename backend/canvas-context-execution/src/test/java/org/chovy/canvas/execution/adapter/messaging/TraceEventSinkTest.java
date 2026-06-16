package org.chovy.canvas.execution.adapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TraceEventSinkTest {

    @Test
    void mysqlTraceEventSinkPersistsTraceEventsThroughExecutionOwnedWriter() {
        List<TraceEvent> written = new ArrayList<>();
        MySqlTraceEventSink sink = new MySqlTraceEventSink(written::add);
        TraceEvent event = new TraceEvent(
                5L,
                "exec-1",
                "node-a",
                "SEND_MESSAGE",
                "SUCCESS",
                Map.of("messageId", "m-1"),
                Instant.parse("2026-06-10T04:30:00Z"));

        sink.accept(event);

        assertThat(written).containsExactly(event);
    }
}
