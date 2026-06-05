package org.chovy.canvas.domain.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpCdpWarehouseExternalRealtimeJobProbeClientTest {

    @Test
    void mapsFlinkFailedStateToFailedRuntime() throws Exception {
        HttpCdpWarehouseExternalRealtimeJobProbeClient client = client("{\"state\":\"FAILED\"}");

        CdpWarehouseExternalRealtimeJobProbeClient.ProbeResult result =
                client.probe(target("FLINK_REST"));

        assertThat(result.runtimeStatus()).isEqualTo("FAILED");
        assertThat(result.payloadJson()).contains("FLINK_REST");
    }

    @Test
    void mapsKafkaConnectFailedTaskToFailedRuntime() throws Exception {
        HttpCdpWarehouseExternalRealtimeJobProbeClient client = client("""
                {"connector":{"state":"RUNNING"},"tasks":[{"id":0,"state":"RUNNING"},{"id":1,"state":"FAILED"}]}
                """);

        CdpWarehouseExternalRealtimeJobProbeClient.ProbeResult result =
                client.probe(target("KAFKA_CONNECT"));

        assertThat(result.runtimeStatus()).isEqualTo("FAILED");
    }

    @Test
    void mapsDorisPausedRoutineLoadToPausedRuntime() throws Exception {
        HttpCdpWarehouseExternalRealtimeJobProbeClient client = client("{\"JobState\":\"PAUSED\"}");

        CdpWarehouseExternalRealtimeJobProbeClient.ProbeResult result =
                client.probe(target("DORIS_ROUTINE_LOAD"));

        assertThat(result.runtimeStatus()).isEqualTo("PAUSED");
    }

    @SuppressWarnings("unchecked")
    private HttpCdpWarehouseExternalRealtimeJobProbeClient client(String body) throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        return new HttpCdpWarehouseExternalRealtimeJobProbeClient(
                new ObjectMapper(), httpClient, Duration.ofSeconds(1));
    }

    private CdpWarehouseExternalRealtimeJobProbeClient.ProbeTarget target(String engineType) {
        return new CdpWarehouseExternalRealtimeJobProbeClient.ProbeTarget(
                1L,
                9L,
                "pipe",
                "job-a",
                engineType,
                "http://probe/status",
                null,
                "external-1",
                null,
                null);
    }
}
