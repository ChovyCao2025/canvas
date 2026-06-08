package org.chovy.canvas.domain.bi.query;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BiDatasourceHealthSloSummaryTest {

    @Test
    void marksAvailableButStaleDatasourceHealthAsGovernanceRisk() {
        BiDatasourceHealthSloSummary summary = BiDatasourceHealthSloSummary.from(
                List.of(
                        new BiDatasourceHealthSnapshot(
                                "warehouse",
                                "MYSQL",
                                true,
                                "connection ok",
                                LocalDateTime.parse("2026-06-05T08:00:00"))),
                LocalDateTime.parse("2026-06-05T11:30:00"),
                120);

        assertThat(summary.availabilityRate()).isEqualTo(100.0);
        assertThat(summary.sources()).singleElement().satisfies(source -> {
            assertThat(source.sourceKey()).isEqualTo("warehouse");
            assertThat(source.riskLevel()).isEqualTo("STALE");
            assertThat(source.recommendedAction()).contains("重新执行健康检查");
        });
    }
}
