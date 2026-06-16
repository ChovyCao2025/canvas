package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.chovy.canvas.cdp.api.CdpUserReadFacade;
import org.chovy.canvas.cdp.api.CdpUserReadFacade.CdpUserRowView;
import org.junit.jupiter.api.Test;

/**
 * 验证 CdpUserReadApplicationService 的核心行为。
 */
class CdpUserReadApplicationServiceTest {

    /**
     * 查询Filters By Keyword And Keeps Legacy Row Shape列表。
     */
    @Test
    void listFiltersByKeywordAndKeepsLegacyRowShape() {
        CdpUserReadFacade service = new CdpUserReadApplicationService();

        List<CdpUserRowView> rows = service.listUsers(7L, "ali");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).userId()).isEqualTo("user-alice");
        assertThat(rows.get(0).displayName()).isEqualTo("Alice Chen");
        assertThat(rows.get(0).executionCount()).isEqualTo(5);
        assertThat(rows.get(0).successCount()).isEqualTo(4);
        assertThat(rows.get(0).failedCount()).isEqualTo(1);
        assertThat(rows.get(0).latestStatus()).isEqualTo("SUCCESS");
        assertThat(rows.get(0).tags()).extracting(CdpUserReadFacade.CdpUserTagSummaryView::tagCode)
                .containsExactly("vip");
    }

    /**
     * 执行 detailAndInsightUseTenantScopedProfileAndRejectUnknownUser 对应的 CDP 业务操作。
     */
    @Test
    void detailAndInsightUseTenantScopedProfileAndRejectUnknownUser() {
        CdpUserReadFacade service = new CdpUserReadApplicationService();

        CdpUserReadFacade.CdpUserProfileView detail = service.getUser(7L, "user-alice");
        CdpUserReadFacade.CdpUserInsightView insight = service.getInsight(7L, "user-alice");

        assertThat(detail.displayName()).isEqualTo("Alice Chen");
        assertThat(detail.phone()).isEqualTo("138****0001");
        assertThat(detail.email()).isEqualTo("alice@example.com");
        assertThat(detail.status()).isEqualTo("ACTIVE");
        assertThat(insight.userId()).isEqualTo("user-alice");
        assertThat(insight.profile()).isEqualTo(detail);
        assertThat(insight.tags()).extracting(CdpUserReadFacade.CdpUserTagSummaryView::tagValue)
                .containsExactly("gold");
        assertThat(insight.canvasRows()).extracting(CdpUserReadFacade.CdpUserCanvasSummaryView::canvasName)
                .containsExactly("Welcome Journey");

        assertThatThrownBy(() -> service.getUser(7L, "missing-user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CDP user not found: missing-user");
    }
}
