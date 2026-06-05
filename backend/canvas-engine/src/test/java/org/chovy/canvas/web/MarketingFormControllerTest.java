package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.marketing.MarketingFormService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingFormControllerTest {

    @Test
    void operatorEndpointsUseFallbackTenant() {
        MarketingFormService service = mock(MarketingFormService.class);
        MarketingFormService.FormDefinitionView view = new MarketingFormService.FormDefinitionView(
                1L, "signup", "报名", null, MarketingFormService.STATUS_ACTIVE,
                "[]", "{}", "提交成功", null, null);
        when(service.list(0L)).thenReturn(List.of(view));
        when(service.create(org.mockito.ArgumentMatchers.eq(0L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(view);

        MarketingFormController controller = new MarketingFormController(service);
        R<List<MarketingFormService.FormDefinitionView>> listResponse = controller.list().block();
        R<MarketingFormService.FormDefinitionView> createResponse = controller.create(
                new MarketingFormController.FormDefinitionReq(
                        "signup", "报名", null, "[]", "{}", "提交成功", true, "tester")).block();

        assertThat(listResponse.getData()).containsExactly(view);
        assertThat(createResponse.getData()).isSameAs(view);
        ArgumentCaptor<MarketingFormService.FormDefinitionCommand> captor =
                ArgumentCaptor.forClass(MarketingFormService.FormDefinitionCommand.class);
        verify(service).create(org.mockito.ArgumentMatchers.eq(0L), captor.capture());
        assertThat(captor.getValue().publicKey()).isEqualTo("signup");
    }

    @Test
    void publicSubmitPassesPayloadAndRequestMetadata() {
        MarketingFormService service = mock(MarketingFormService.class);
        MarketingFormService.SubmitResult result =
                new MarketingFormService.SubmitResult(3L, "email:lead@example.com", "提交成功", null, false);
        when(service.submit(org.mockito.ArgumentMatchers.eq("signup"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(result);
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/public/marketing-forms/signup/submit")
                .header("User-Agent", "JUnit")
                .remoteAddress(new java.net.InetSocketAddress("127.0.0.1", 8080))
                .build();

        R<MarketingFormService.SubmitResult> response = new MarketingFormController(service).submit(
                request,
                "signup",
                new MarketingFormController.PublicSubmitReq(
                        Map.of("email", "lead@example.com"),
                        Map.of("utm_source", "newsletter"),
                        "anon-1",
                        "idem-1",
                        null,
                        null)).block();

        ArgumentCaptor<MarketingFormService.PublicSubmitCommand> captor =
                ArgumentCaptor.forClass(MarketingFormService.PublicSubmitCommand.class);
        verify(service).submit(org.mockito.ArgumentMatchers.eq("signup"), captor.capture());
        assertThat(response.getData()).isSameAs(result);
        assertThat(captor.getValue().response()).containsEntry("email", "lead@example.com");
        assertThat(captor.getValue().userAgent()).isEqualTo("JUnit");
        assertThat(captor.getValue().submitIpHash()).hasSize(64);
    }
}
