package org.chovy.canvas.infrastructure.doris;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DorisCdpEventStreamLoaderTest {

    @Test
    void jsonLinesPayloadUsesCdpWarehouseFieldNamesAndDateTimeFormat() {
        DorisCdpEventStreamLoader loader = new DorisCdpEventStreamLoader(
                true,
                "http://localhost:8040/api/canvas_ods/cdp_event_log/_stream_load",
                "root",
                "",
                Duration.ofSeconds(1),
                HttpClient.newHttpClient(),
                new ObjectMapper());
        CdpEventLogDO row = eventRow();

        String jsonLines = loader.toJsonLines(List.of(row));

        assertThat(jsonLines).contains("\"tenant_id\":9");
        assertThat(jsonLines).contains("\"event_log_id\":101");
        assertThat(jsonLines).contains("\"message_id\":\"msg-1\"");
        assertThat(jsonLines).contains("\"event_code\":\"OrderPaid\"");
        assertThat(jsonLines).contains("\"user_id\":\"user-1\"");
        assertThat(jsonLines).contains("\"anonymous_id\":\"anon-1\"");
        assertThat(jsonLines).contains("\"session_id\":\"session-1\"");
        assertThat(jsonLines).contains("\"device_id\":\"device-1\"");
        assertThat(jsonLines).contains("\"platform\":\"web\"");
        assertThat(jsonLines).contains("\"properties\":{\"amount\":20}");
        assertThat(jsonLines).contains("\"event_time\":\"2026-06-05 10:11:12\"");
        assertThat(jsonLines).contains("\"received_at\":\"2026-06-05 10:11:13\"");
    }

    @Test
    void disabledLoaderSkipsHttpWork() {
        DorisCdpEventStreamLoader loader = new DorisCdpEventStreamLoader(
                false,
                "http://localhost:1/unreachable",
                "root",
                "",
                Duration.ofMillis(1),
                HttpClient.newHttpClient(),
                new ObjectMapper());

        assertThat(loader.load(List.of(eventRow()))).isFalse();
    }

    @Test
    void writeAcceptedThrowsWhenEnabledStreamLoadFails() {
        DorisCdpEventStreamLoader loader = new DorisCdpEventStreamLoader(
                true,
                "http://localhost:1/unreachable",
                "root",
                "",
                Duration.ofMillis(1),
                HttpClient.newHttpClient(),
                new ObjectMapper());

        assertThatThrownBy(() -> loader.writeAccepted(eventRow()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Doris CDP event Stream Load failed");
    }

    private CdpEventLogDO eventRow() {
        CdpEventLogDO row = new CdpEventLogDO();
        row.setId(101L);
        row.setTenantId(9L);
        row.setMessageId("msg-1");
        row.setEventCode("OrderPaid");
        row.setUserId("user-1");
        row.setAnonymousId("anon-1");
        row.setSessionId("session-1");
        row.setDeviceId("device-1");
        row.setPlatform("web");
        row.setProperties("{\"amount\":20}");
        row.setEventTime(LocalDateTime.of(2026, 6, 5, 10, 11, 12));
        row.setReceivedAt(LocalDateTime.of(2026, 6, 5, 10, 11, 13));
        return row;
    }
}
