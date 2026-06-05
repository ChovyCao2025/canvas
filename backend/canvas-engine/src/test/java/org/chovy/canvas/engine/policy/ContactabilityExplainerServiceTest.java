package org.chovy.canvas.engine.policy;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContactabilityExplainerServiceTest {

    @Test
    void explainReturnsOrderedChecksAndOverallBlockedState() {
        MarketingPolicyService policyService = mock(MarketingPolicyService.class);
        when(policyService.consentAllowed("user-1", "SMS", true))
                .thenReturn(MarketingPolicyService.PolicyDecision.allow());
        when(policyService.suppressionAllowed("user-1", "SMS"))
                .thenReturn(MarketingPolicyService.PolicyDecision.blocked("MARKETING_SUPPRESSED", "suppressed"));
        when(policyService.channelAvailable("user-1", "SMS"))
                .thenReturn(MarketingPolicyService.PolicyDecision.allow());
        when(policyService.quietHoursAllowed("user-1", "22:00", "08:00", "USER_LOCAL"))
                .thenReturn(MarketingPolicyService.PolicyDecision.allow());
        when(policyService.previewFrequency("user-1", 20L, "node-1", "JOURNEY", "SMS", 1, Duration.ofDays(1)))
                .thenReturn(MarketingPolicyService.PolicyDecision.allow());

        ContactabilityExplainerService.Report report = new ContactabilityExplainerService(policyService)
                .explain(new ContactabilityExplainerService.Request(
                        "user-1",
                        "sms",
                        true,
                        "22:00",
                        "08:00",
                        "USER_LOCAL",
                        20L,
                        "node-1",
                        "JOURNEY",
                        1,
                        Duration.ofDays(1)));

        assertThat(report.allowed()).isFalse();
        assertThat(report.userId()).isEqualTo("user-1");
        assertThat(report.channel()).isEqualTo("SMS");
        assertThat(report.checks()).extracting(ContactabilityExplainerService.Check::checkKey)
                .containsExactly("CONSENT", "SUPPRESSION", "CHANNEL", "QUIET_HOURS", "FREQUENCY");
        assertThat(blockedChecks(report)).containsExactly("SUPPRESSION");
    }

    private List<String> blockedChecks(ContactabilityExplainerService.Report report) {
        return report.checks().stream()
                .filter(check -> !check.allowed())
                .map(ContactabilityExplainerService.Check::checkKey)
                .toList();
    }
}
