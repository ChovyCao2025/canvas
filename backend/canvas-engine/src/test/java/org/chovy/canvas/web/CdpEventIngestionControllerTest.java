package org.chovy.canvas.web;

import org.chovy.canvas.domain.cdp.CdpEventIngestionService;
import org.chovy.canvas.domain.cdp.CdpWriteKeyAuthService;
import org.chovy.canvas.dto.cdp.BatchTrackReq;
import org.chovy.canvas.dto.cdp.IngestionResult;
import org.chovy.canvas.dto.cdp.TrackEventReq;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpEventIngestionControllerTest {

    @Test
    void trackAuthenticatesBeforeIngestion() {
        CdpWriteKeyAuthService auth = mock(CdpWriteKeyAuthService.class);
        CdpEventIngestionService ingestion = mock(CdpEventIngestionService.class);
        CdpEventIngestionController controller = new CdpEventIngestionController(auth, ingestion);
        var key = new CdpWriteKeyAuthService.AuthenticatedWriteKey(7L, 42L, "ck_test_abc", "WEB", 100, null);
        when(auth.authenticate(any())).thenReturn(key);
        when(ingestion.ingestBatch(eq(key), any())).thenReturn(new IngestionResult(1, 0, List.of()));

        BatchTrackReq req = new BatchTrackReq(List.of(validEvent()), OffsetDateTime.now());
        var httpReq = MockServerHttpRequest.post("/cdp/events/track").build();

        IngestionResult result = controller.track(httpReq, Mono.just(req)).block().getData();

        assertThat(result.accepted()).isEqualTo(1);
        verify(auth).authenticate(httpReq.getHeaders());
        verify(ingestion).ingestBatch(eq(key), eq(req));
    }

    private TrackEventReq validEvent() {
        return new TrackEventReq(
                "msg-1",
                "track",
                "OrderComplete",
                "user-1",
                "anon-1",
                "idem-1",
                Map.of("amount", 20),
                Map.of("sessionId", "sess-1", "deviceId", "dev-1", "platform", "WEB"),
                OffsetDateTime.parse("2026-05-30T10:00:00Z"),
                null);
    }
}
