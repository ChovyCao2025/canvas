package org.chovy.canvas.web.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.chovy.canvas.platform.api.PlatformWorkstreamFacade;
import org.chovy.canvas.platform.api.WorkstreamStatusView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class PlatformWorkstreamControllerCompatibilityTest {

    @Test
    void listsWorkstreamStatusesThroughLegacyPlatformRoute() {
        RecordingPlatformWorkstreamFacade facade = new RecordingPlatformWorkstreamFacade();

        webClient(facade)
                .get()
                .uri("/platform/workstreams")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data[0].workstreamKey").isEqualTo("data-assets")
                .jsonPath("$.data[0].displayName").isEqualTo("Data Assets")
                .jsonPath("$.data[0].priority").isEqualTo("P2")
                .jsonPath("$.data[0].status").isEqualTo("READY_FOR_CHILD_EXECUTION")
                .jsonPath("$.data[0].childSpecPath")
                .isEqualTo("docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md")
                .jsonPath("$.data[0].summary").isEqualTo("Event pipeline");

        assertThat(facade.statusesCalled).isTrue();
    }

    private static WebTestClient webClient(PlatformWorkstreamFacade facade) {
        return WebTestClient.bindToController(new PlatformWorkstreamController(facade)).build();
    }

    private static final class RecordingPlatformWorkstreamFacade implements PlatformWorkstreamFacade {
        private boolean statusesCalled;

        @Override
        public List<WorkstreamStatusView> statuses() {
            statusesCalled = true;
            return List.of(new WorkstreamStatusView(
                    "data-assets",
                    "Data Assets",
                    "P2",
                    "READY_FOR_CHILD_EXECUTION",
                    "docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md",
                    "Event pipeline"));
        }

        @Override
        public WorkstreamStatusView requireExecutableChildSpec(String workstreamKey) {
            throw new UnsupportedOperationException("not used by list route");
        }
    }
}
