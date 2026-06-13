package org.chovy.canvas.marketing.application.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JourneyRiskAuditServiceTest {

    private final JourneyRiskAuditService service = new JourneyRiskAuditService();

    @Test
    void returnsStructuredRiskFindingsForMinimumAiAuditRules() {
        JourneyRiskAuditService.AuditResult result = service.audit(new JourneyRiskAuditService.AuditRequest(
                7L,
                "coupon-blast",
                List.of(
                        node("start", "webhook", Map.of()),
                        node("message-1", "message", Map.of("channel", "sms")),
                        node("coupon", "coupon", Map.of("couponId", "coupon-1")),
                        node("message-2", "message", Map.of("channel", "push"))),
                List.of(
                        edge("start", "message-1"),
                        edge("message-1", "coupon"),
                        edge("coupon", "message-2"))));

        assertThat(result.auditStatus()).isEqualTo("BLOCKED");
        assertThat(result.safeForPreview()).isTrue();
        assertThat(result.findings())
                .extracting(JourneyRiskAuditService.RiskFinding::code)
                .containsExactly(
                        "MISSING_FREQUENCY_CAP",
                        "MISSING_APPROVAL",
                        "COUPON_WITHOUT_CAP",
                        "TOUCHES_TOO_DENSE",
                        "MISSING_FAILURE_OR_EXIT_PATH");
        assertThat(result.findings())
                .allSatisfy(finding -> assertThat(finding.message()).isNotBlank());
    }

    @Test
    void returnsReadyWhenJourneyContainsCapsApprovalSpacingAndExitPath() {
        JourneyRiskAuditService.AuditResult result = service.audit(new JourneyRiskAuditService.AuditRequest(
                7L,
                "approved-welcome",
                List.of(
                        node("start", "webhook", Map.of()),
                        node("approval", "approval", Map.of("approverRole", "campaign-owner")),
                        node("message", "message", Map.of("channel", "sms", "frequencyCap", "1/day")),
                        node("coupon", "coupon", Map.of("maxRedemptions", 1000, "cooldownHours", 48)),
                        node("end", "end", Map.of())),
                List.of(
                        edge("start", "approval"),
                        edge("approval", "message"),
                        edge("message", "coupon"),
                        edge("coupon", "end"))));

        assertThat(result.auditStatus()).isEqualTo("READY");
        assertThat(result.findings()).isEmpty();
    }

    private static JourneyRiskAuditService.JourneyNode node(String id, String type, Map<String, Object> config) {
        return new JourneyRiskAuditService.JourneyNode(id, type, config);
    }

    private static JourneyRiskAuditService.JourneyEdge edge(String from, String to) {
        return new JourneyRiskAuditService.JourneyEdge(from, to);
    }
}
