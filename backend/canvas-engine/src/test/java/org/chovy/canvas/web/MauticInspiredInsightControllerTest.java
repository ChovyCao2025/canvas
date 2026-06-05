package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.engine.insights.MauticInspiredInsightService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MauticInspiredInsightControllerTest {

    @Test
    void delegatesAllReadOnlyInsightEndpoints() {
        MauticInspiredInsightService service = mock(MauticInspiredInsightService.class);
        MauticInspiredInsightService.AudienceMembershipReport audience =
                new MauticInspiredInsightService.AudienceMembershipReport(10L, "VIP", "user-1",
                        "MATCHED", "READY", 1L, "SUCCEEDED", List.of("matched"));
        MauticInspiredInsightService.JourneyPathReport path =
                new MauticInspiredInsightService.JourneyPathReport("exec-1", List.of(), 0, 0, 0);
        MauticInspiredInsightService.ChannelPreferenceReport preference =
                new MauticInspiredInsightService.ChannelPreferenceReport("user-1", "SMS", null, List.of());
        MauticInspiredInsightService.SuppressionTimeline timeline =
                new MauticInspiredInsightService.SuppressionTimeline("user-1", List.of());
        MauticInspiredInsightService.PublishHealthReport health =
                new MauticInspiredInsightService.PublishHealthReport(9L, "Welcome", 100, List.of());
        when(service.explainAudienceMembership(10L, "user-1")).thenReturn(audience);
        when(service.explainJourneyPath("exec-1")).thenReturn(path);
        when(service.resolveChannelPreference("user-1", "email")).thenReturn(preference);
        when(service.suppressionTimeline("user-1")).thenReturn(timeline);
        when(service.publishHealth(9L)).thenReturn(health);
        when(service.frequencyTemplates()).thenReturn(List.of(
                new MauticInspiredInsightService.FrequencyTemplate("journey_daily", "JOURNEY", 1, 86400, "旅程日频控")));
        MauticInspiredInsightController controller = new MauticInspiredInsightController(service);

        R<MauticInspiredInsightService.AudienceMembershipReport> audienceResponse =
                controller.audienceMembership(10L, "user-1").block();
        R<MauticInspiredInsightService.JourneyPathReport> pathResponse =
                controller.journeyPath("exec-1").block();
        R<MauticInspiredInsightService.ChannelPreferenceReport> preferenceResponse =
                controller.channelPreference("user-1", "email").block();
        R<MauticInspiredInsightService.SuppressionTimeline> timelineResponse =
                controller.suppressionTimeline("user-1").block();
        R<MauticInspiredInsightService.PublishHealthReport> healthResponse =
                controller.publishHealth(9L).block();
        R<List<MauticInspiredInsightService.FrequencyTemplate>> templatesResponse =
                controller.frequencyTemplates().block();

        assertThat(audienceResponse.getData()).isSameAs(audience);
        assertThat(pathResponse.getData()).isSameAs(path);
        assertThat(preferenceResponse.getData()).isSameAs(preference);
        assertThat(timelineResponse.getData()).isSameAs(timeline);
        assertThat(healthResponse.getData()).isSameAs(health);
        assertThat(templatesResponse.getData()).hasSize(1);
        verify(service).explainAudienceMembership(10L, "user-1");
        verify(service).resolveChannelPreference("user-1", "email");
    }
}
