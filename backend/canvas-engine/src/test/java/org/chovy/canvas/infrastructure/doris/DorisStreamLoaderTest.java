package org.chovy.canvas.infrastructure.doris;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DorisStreamLoaderTest {

    @Test
    void jsonLinesPayloadUsesDorisCompatibleFieldNamesAndDateTimeFormat() {
        DorisStreamLoader loader = new DorisStreamLoader(
                true,
                "http://localhost:8040/api/canvas_ods/canvas_execution_trace/_stream_load",
                "root",
                "",
                Duration.ofSeconds(1),
                HttpClient.newHttpClient(),
                new ObjectMapper());
        CanvasExecutionTraceDO trace = CanvasExecutionTraceDO.builder()
                .id(7L)
                .tenantId(9L)
                .executionId("exec-1")
                .nodeId("node-1")
                .nodeType("API_CALL")
                .nodeName("Call API")
                .status(1)
                .outputData("{\"ok\":true}")
                .startedAt(LocalDateTime.of(2026, 1, 2, 3, 4, 5))
                .finishedAt(LocalDateTime.of(2026, 1, 2, 3, 4, 8))
                .durationMs(3000L)
                .build();

        String jsonLines = loader.toJsonLines(List.of(trace));

        assertThat(jsonLines).contains("\"trace_id\":7");
        assertThat(jsonLines).contains("\"tenant_id\":9");
        assertThat(jsonLines).contains("\"execution_id\":\"exec-1\"");
        assertThat(jsonLines).contains("\"node_type\":\"API_CALL\"");
        assertThat(jsonLines).contains("\"started_at\":\"2026-01-02 03:04:05\"");
        assertThat(jsonLines).contains("\"finished_at\":\"2026-01-02 03:04:08\"");
        assertThat(jsonLines).contains("\"duration_ms\":3000");
    }

    @Test
    void disabledLoaderSkipsHttpWork() {
        DorisStreamLoader loader = new DorisStreamLoader(
                false,
                "http://localhost:1/unreachable",
                "root",
                "",
                Duration.ofMillis(1),
                HttpClient.newHttpClient(),
                new ObjectMapper());

        boolean loaded = loader.load(List.of(CanvasExecutionTraceDO.builder()
                .executionId("exec-1")
                .nodeId("node-1")
                .build()));

        assertThat(loaded).isFalse();
    }
}
