package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.engine.policy.ContactabilityExplainerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactabilityControllerTest {

    @Test
    void explainUsesSendPathDefaultsWhenOptionalParamsAreOmitted() {
        ContactabilityExplainerService service = mock(ContactabilityExplainerService.class);
        ContactabilityExplainerService.Report report = new ContactabilityExplainerService.Report(
                "user-1",
                "SMS",
                true,
                List.of(new ContactabilityExplainerService.Check("CONSENT", true, null, null)));
        when(service.explain(org.mockito.ArgumentMatchers.any())).thenReturn(report);

        R<ContactabilityExplainerService.Report> response = new ContactabilityController(service)
                .explain("user-1", "sms", null, null, null, null, null, null, null, null, null)
                .block();

        ArgumentCaptor<ContactabilityExplainerService.Request> captor =
                ArgumentCaptor.forClass(ContactabilityExplainerService.Request.class);
        verify(service).explain(captor.capture());
        ContactabilityExplainerService.Request request = captor.getValue();

        assertThat(response.getData()).isSameAs(report);
        assertThat(request.userId()).isEqualTo("user-1");
        assertThat(request.channel()).isEqualTo("sms");
        assertThat(request.requireExplicitConsent()).isTrue();
        assertThat(request.quietStart()).isEqualTo("22:00");
        assertThat(request.quietEnd()).isEqualTo("08:00");
        assertThat(request.quietTimezone()).isEqualTo("USER_LOCAL");
        assertThat(request.canvasId()).isEqualTo(0L);
        assertThat(request.nodeId()).isEqualTo("preflight");
        assertThat(request.frequencyScope()).isEqualTo("JOURNEY");
        assertThat(request.frequencyMax()).isEqualTo(1);
        assertThat(request.frequencyWindow()).isEqualTo(Duration.ofDays(1));
    }

    @Test
    void explainFallsBackToDefaultsWhenOptionalParamsAreInvalid() {
        ContactabilityExplainerService service = mock(ContactabilityExplainerService.class);
        ContactabilityExplainerService.Report report = new ContactabilityExplainerService.Report(
                "user-1",
                "EMAIL",
                true,
                List.of(new ContactabilityExplainerService.Check("CONSENT", true, null, null)));
        when(service.explain(org.mockito.ArgumentMatchers.any())).thenReturn(report);

        new ContactabilityController(service)
                .explain("user-1", "email", false, "not-time", "25:99", "Mars/Base", 12L,
                        "node-1", "CHANNEL", -1, -10L)
                .block();

        ArgumentCaptor<ContactabilityExplainerService.Request> captor =
                ArgumentCaptor.forClass(ContactabilityExplainerService.Request.class);
        verify(service).explain(captor.capture());
        ContactabilityExplainerService.Request request = captor.getValue();

        assertThat(request.requireExplicitConsent()).isFalse();
        assertThat(request.quietStart()).isEqualTo("22:00");
        assertThat(request.quietEnd()).isEqualTo("08:00");
        assertThat(request.quietTimezone()).isEqualTo("USER_LOCAL");
        assertThat(request.frequencyMax()).isEqualTo(1);
        assertThat(request.frequencyWindow()).isEqualTo(Duration.ofDays(1));
    }
}
