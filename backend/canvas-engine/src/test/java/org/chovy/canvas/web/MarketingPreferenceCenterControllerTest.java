package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.policy.MarketingPreferenceCenterService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingPreferenceCenterControllerTest {

    @Test
    void delegatesReportWithFallbackTenant() {
        MarketingPreferenceCenterService service = mock(MarketingPreferenceCenterService.class);
        MarketingPreferenceCenterService.PreferenceReport report =
                new MarketingPreferenceCenterService.PreferenceReport("user-1", List.of(), List.of(), List.of(),
                        new MarketingPreferenceCenterService.PreferenceSummary(0, 0, 0, 0, 0));
        when(service.report(0L, "user-1")).thenReturn(report);

        R<MarketingPreferenceCenterService.PreferenceReport> response =
                new MarketingPreferenceCenterController(service).report("user-1").block();

        assertThat(response.getData()).isSameAs(report);
        verify(service).report(0L, "user-1");
    }

    @Test
    void bindsPathChannelForWrites() {
        MarketingPreferenceCenterService service = mock(MarketingPreferenceCenterService.class);
        MarketingPreferenceCenterController controller = new MarketingPreferenceCenterController(service);

        controller.updateConsent("user-1", "email",
                new MarketingPreferenceCenterController.ConsentUpdateReq("OPT_OUT", "operator")).block();
        controller.updateChannel("user-1", "sms",
                new MarketingPreferenceCenterController.ChannelUpdateReq("13800000000", true, false, null)).block();
        controller.addSuppression("user-1",
                new MarketingPreferenceCenterController.SuppressionCreateReq("all", "complaint", true, null)).block();
        controller.deactivateSuppression(9L).block();

        ArgumentCaptor<MarketingPreferenceCenterService.ConsentUpdateCommand> consentCaptor =
                ArgumentCaptor.forClass(MarketingPreferenceCenterService.ConsentUpdateCommand.class);
        ArgumentCaptor<MarketingPreferenceCenterService.ChannelUpdateCommand> channelCaptor =
                ArgumentCaptor.forClass(MarketingPreferenceCenterService.ChannelUpdateCommand.class);
        ArgumentCaptor<MarketingPreferenceCenterService.SuppressionCreateCommand> suppressionCaptor =
                ArgumentCaptor.forClass(MarketingPreferenceCenterService.SuppressionCreateCommand.class);
        verify(service).upsertConsent(org.mockito.ArgumentMatchers.eq(0L), org.mockito.ArgumentMatchers.eq("user-1"),
                consentCaptor.capture());
        verify(service).upsertChannel(org.mockito.ArgumentMatchers.eq(0L), org.mockito.ArgumentMatchers.eq("user-1"),
                channelCaptor.capture());
        verify(service).addSuppression(org.mockito.ArgumentMatchers.eq(0L), org.mockito.ArgumentMatchers.eq("user-1"),
                suppressionCaptor.capture());
        verify(service).deactivateSuppression(0L, 9L);

        assertThat(consentCaptor.getValue().channel()).isEqualTo("email");
        assertThat(channelCaptor.getValue().channel()).isEqualTo("sms");
        assertThat(suppressionCaptor.getValue().channel()).isEqualTo("all");
    }
}
