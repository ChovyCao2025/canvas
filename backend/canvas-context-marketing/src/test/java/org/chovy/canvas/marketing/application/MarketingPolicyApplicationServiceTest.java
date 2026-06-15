package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.chovy.canvas.marketing.api.MarketingPolicyFacade;
import org.junit.jupiter.api.Test;

class MarketingPolicyApplicationServiceTest {

    @Test
    void upsertsConsentSuppressionAndChannelIntoTenantScopedPolicyState() {
        MarketingPolicyFacade service = new MarketingPolicyApplicationService();

        MarketingPolicyFacade.ConsentView consent = service.upsertConsent(7L,
                new MarketingPolicyFacade.ConsentCommand(" user-1 ", "email", "granted", " web "));
        MarketingPolicyFacade.SuppressionView suppression = service.upsertSuppression(7L,
                new MarketingPolicyFacade.SuppressionCommand("user-1", null, "complaint", null,
                        LocalDateTime.parse("2026-07-01T00:00:00")));
        MarketingPolicyFacade.ChannelView channel = service.upsertChannel(7L,
                new MarketingPolicyFacade.ChannelCommand("user-1", "email", " buyer@example.com ", null, null,
                        " {\"source\":\"crm\"} "));

        MarketingPolicyFacade.PolicyState state = service.policyState(7L, "user-1", " email ");

        assertThat(consent).returns(7L, MarketingPolicyFacade.ConsentView::tenantId)
                .returns("user-1", MarketingPolicyFacade.ConsentView::userId)
                .returns("EMAIL", MarketingPolicyFacade.ConsentView::channel)
                .returns("GRANTED", MarketingPolicyFacade.ConsentView::consentStatus)
                .returns("web", MarketingPolicyFacade.ConsentView::source);
        assertThat(suppression).returns("ALL", MarketingPolicyFacade.SuppressionView::channel)
                .returns(1, MarketingPolicyFacade.SuppressionView::active);
        assertThat(channel).returns(1, MarketingPolicyFacade.ChannelView::enabled)
                .returns(0, MarketingPolicyFacade.ChannelView::verified)
                .returns("buyer@example.com", MarketingPolicyFacade.ChannelView::address);
        assertThat(state.consent()).isEqualTo(consent);
        assertThat(state.suppressions()).singleElement().isEqualTo(suppression);
        assertThat(state.customerChannel()).isEqualTo(channel);
    }

    @Test
    void upsertsRowsByTenantUserChannelAndReasonWithoutCrossTenantLeakage() {
        MarketingPolicyFacade service = new MarketingPolicyApplicationService();

        MarketingPolicyFacade.ConsentView first = service.upsertConsent(7L,
                new MarketingPolicyFacade.ConsentCommand("user-1", "sms", "granted", "form"));
        MarketingPolicyFacade.ConsentView updated = service.upsertConsent(7L,
                new MarketingPolicyFacade.ConsentCommand("user-1", "SMS", "denied", "preference-center"));
        service.upsertConsent(8L, new MarketingPolicyFacade.ConsentCommand("user-1", "sms", "granted", "other"));

        service.upsertSuppression(7L,
                new MarketingPolicyFacade.SuppressionCommand("user-1", "sms", "bounce", false, null));
        service.upsertChannel(7L,
                new MarketingPolicyFacade.ChannelCommand("user-1", "sms", "+15551234567", 0, 1, null));

        assertThat(updated.id()).isEqualTo(first.id());
        assertThat(updated.consentStatus()).isEqualTo("DENIED");

        MarketingPolicyFacade.PolicyState state = service.policyState(7L, "user-1", "sms");
        assertThat(state.consent().tenantId()).isEqualTo(7L);
        assertThat(state.suppressions()).singleElement()
                .returns(0, MarketingPolicyFacade.SuppressionView::active);
        assertThat(state.customerChannel())
                .returns(0, MarketingPolicyFacade.ChannelView::enabled)
                .returns(1, MarketingPolicyFacade.ChannelView::verified);

        assertThat(service.policyState(8L, "user-1", "sms").consent().source()).isEqualTo("other");
    }

    @Test
    void validationAndDefaultsFollowLegacyCompatibility() {
        MarketingPolicyFacade service = new MarketingPolicyApplicationService();

        assertThatThrownBy(() -> service.policyState(7L, " ", "email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId is required");
        assertThatThrownBy(() -> service.upsertConsent(7L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consent request is required");
        assertThatThrownBy(() -> service.upsertSuppression(7L,
                new MarketingPolicyFacade.SuppressionCommand("user-1", "email", " ", true, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason is required");

        MarketingPolicyFacade.ChannelView channel = service.upsertChannel(null,
                new MarketingPolicyFacade.ChannelCommand("user-1", "push", " token ", null, null, " "));
        assertThat(channel).returns(0L, MarketingPolicyFacade.ChannelView::tenantId)
                .returns(1, MarketingPolicyFacade.ChannelView::enabled)
                .returns(0, MarketingPolicyFacade.ChannelView::verified)
                .returns(null, MarketingPolicyFacade.ChannelView::metadata);
    }
}
