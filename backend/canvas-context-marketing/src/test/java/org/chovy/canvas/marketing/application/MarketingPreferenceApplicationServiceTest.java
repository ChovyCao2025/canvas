package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.chovy.canvas.marketing.api.MarketingPreferenceFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证MarketingPreferenceApplicationService的关键兼容行为。
 */
class MarketingPreferenceApplicationServiceTest {

    /**
     * 验证 preference lifecycle is tenant scoped and summarized 场景的兼容行为。
     */
    @Test
    void preferenceLifecycleIsTenantScopedAndSummarized() {
        MarketingPreferenceFacade service = new MarketingPreferenceApplicationService();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(5);

        MarketingPreferenceFacade.ConsentRow consent = service.updateConsent(7L, "user-1",
                new MarketingPreferenceFacade.ConsentUpdateCommand("sms", "optin", "landing-page"));
        MarketingPreferenceFacade.ChannelRow channel = service.updateChannel(7L, "user-1",
                new MarketingPreferenceFacade.ChannelUpdateCommand("sms", "+15550001", true, true,
                        "{\"provider\":\"twilio\"}"));
        MarketingPreferenceFacade.SuppressionRow suppression = service.addSuppression(7L, "user-1",
                new MarketingPreferenceFacade.SuppressionCreateCommand("email", "complaint", true, expiresAt));

        assertThat(consent.channel()).isEqualTo("SMS");
        assertThat(consent.consentStatus()).isEqualTo("OPT_IN");
        assertThat(consent.source()).isEqualTo("landing-page");
        assertThat(channel.channel()).isEqualTo("SMS");
        assertThat(channel.reachable()).isTrue();
        assertThat(suppression.channel()).isEqualTo("EMAIL");
        assertThat(suppression.state()).isEqualTo("ACTIVE");

        MarketingPreferenceFacade.PreferenceReport report = service.report(7L, "user-1");
        assertThat(report.userId()).isEqualTo("user-1");
        assertThat(report.consents()).singleElement()
                .satisfies(row -> assertThat(row.channel()).isEqualTo("SMS"));
        assertThat(report.channels()).singleElement()
                .satisfies(row -> assertThat(row.reachable()).isTrue());
        assertThat(report.suppressions()).singleElement()
                .satisfies(row -> assertThat(row.channel()).isEqualTo("EMAIL"));
        assertThat(report.summary().totalChannels()).isEqualTo(1);
        assertThat(report.summary().optInCount()).isEqualTo(1);
        assertThat(report.summary().activeSuppressionCount()).isEqualTo(1);
        assertThat(report.summary().reachableChannelCount()).isEqualTo(1);

        assertThat(service.report(8L, "user-1").consents()).isEmpty();
    }

    /**
     * 验证 deactivate suppression only affects visible tenant and validation rejects bad status 场景的兼容行为。
     */
    @Test
    void deactivateSuppressionOnlyAffectsVisibleTenantAndValidationRejectsBadStatus() {
        MarketingPreferenceFacade service = new MarketingPreferenceApplicationService();
        MarketingPreferenceFacade.SuppressionRow own = service.addSuppression(7L, "user-1",
                new MarketingPreferenceFacade.SuppressionCreateCommand("sms", "stop", null, null));
        MarketingPreferenceFacade.SuppressionRow other = service.addSuppression(8L, "user-1",
                new MarketingPreferenceFacade.SuppressionCreateCommand("sms", "stop", null, null));

        service.deactivateSuppression(7L, own.id());

        assertThat(service.report(7L, "user-1").suppressions()).singleElement()
                .satisfies(row -> assertThat(row.state()).isEqualTo("INACTIVE"));
        assertThat(service.report(8L, "user-1").suppressions()).singleElement()
                .satisfies(row -> {
                    assertThat(row.id()).isEqualTo(other.id());
                    assertThat(row.state()).isEqualTo("ACTIVE");
                });

        assertThatThrownBy(() -> service.updateConsent(7L, "user-1",
                new MarketingPreferenceFacade.ConsentUpdateCommand("sms", "MAYBE", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported consent status: MAYBE");
    }
}
