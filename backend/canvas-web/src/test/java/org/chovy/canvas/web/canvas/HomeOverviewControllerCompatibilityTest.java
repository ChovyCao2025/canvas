package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.chovy.canvas.canvas.api.HomeOverviewFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class HomeOverviewControllerCompatibilityTest {

    @Test
    void overviewRoutePreservesLegacyEnvelopeAndNormalizesDays() {
        RecordingHomeOverviewFacade facade = new RecordingHomeOverviewFacade();

        webClient(facade)
                .get()
                .uri("/canvas/home/overview?days=99")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.range.days").isEqualTo(7)
                .jsonPath("$.data.summary.publishedCanvasCount").isEqualTo(2)
                .jsonPath("$.data.summary.totalExecutions").isEqualTo(3)
                .jsonPath("$.data.summary.uniqueUsers").isEqualTo(2)
                .jsonPath("$.data.summary.failedExecutions").isEqualTo(1)
                .jsonPath("$.data.summary.successRate").isEqualTo("66.7%")
                .jsonPath("$.data.trend[0].date").isEqualTo("2026-06-09")
                .jsonPath("$.data.trend[0].total").isEqualTo(3)
                .jsonPath("$.data.trend[0].failed").isEqualTo(1)
                .jsonPath("$.data.topCanvases[0].canvasId").isEqualTo(42)
                .jsonPath("$.data.attentionItems[0].type").isEqualTo("HAS_FAILURES");

        assertThat(facade.lastDays).isEqualTo(7);
    }

    private static WebTestClient webClient(HomeOverviewFacade facade) {
        return WebTestClient.bindToController(new HomeOverviewController(facade)).build();
    }

    private static final class RecordingHomeOverviewFacade implements HomeOverviewFacade {
        private int lastDays;

        @Override
        public HomeOverviewView overview(int days) {
            lastDays = days;
            return new HomeOverviewView(
                    new RangeView(days, "2026-06-09", "2026-06-15"),
                    new SummaryView(2L, 3L, 2L, 1L, "66.7%"),
                    List.of(new TrendPointView("2026-06-09", 3L, 1L)),
                    List.of(new TopCanvasView(42L, "Welcome Journey", 3L, 2L, "66.7%", 1L)),
                    List.of(new AttentionItemView(
                            42L,
                            "Welcome Journey",
                            "HAS_FAILURES",
                            "存在 1 次失败执行需要关注",
                            "warning")));
        }
    }
}
