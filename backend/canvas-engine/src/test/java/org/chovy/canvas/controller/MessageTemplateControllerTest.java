package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.template.MessageTemplateService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageTemplateControllerTest {

    @Test
    void createUsesTenantFromAuthenticatedContext() {
        MessageTemplateService service = mock(MessageTemplateService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        TenantContext tenant = operator();
        when(resolver.currentOrError()).thenReturn(Mono.just(tenant));
        MessageTemplateService.TemplateDraft draft =
                new MessageTemplateService.TemplateDraft("welcome_sms", "Welcome SMS", "SMS", "Hi {{firstName}}");
        when(service.create(tenant, draft)).thenReturn(new MessageTemplateService.Template(
                8L, "welcome_sms", "Welcome SMS", "SMS", "Hi {{firstName}}", List.of("firstName"), "DRAFT", "operator-1"));
        MessageTemplateController controller = new MessageTemplateController(service, resolver);

        StepVerifier.create(controller.create(draft))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().templateCode()).isEqualTo("welcome_sms");
                    assertThat(response.getData().createdBy()).isEqualTo("operator-1");
                })
                .verifyComplete();
    }

    @Test
    void searchAndPreviewUseTenantFromAuthenticatedContext() {
        MessageTemplateService service = mock(MessageTemplateService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        TenantContext tenant = operator();
        when(resolver.currentOrError()).thenReturn(Mono.just(tenant));
        when(service.search(tenant, "welcome", "SMS")).thenReturn(List.of(new MessageTemplateService.Template(
                8L, "welcome_sms", "Welcome SMS", "SMS", "Hi {{firstName}}", List.of("firstName"), "DRAFT", "operator-1")));
        when(service.preview(tenant, "welcome_sms", Map.of("firstName", "Alice")))
                .thenReturn(new MessageTemplateService.PreviewResult("Hi Alice", List.of()));
        MessageTemplateController controller = new MessageTemplateController(service, resolver);

        StepVerifier.create(controller.search("welcome", "SMS"))
                .assertNext(response -> assertThat(response.getData()).hasSize(1))
                .verifyComplete();
        StepVerifier.create(controller.preview("welcome_sms", Map.of("firstName", "Alice")))
                .assertNext(response -> assertThat(response.getData().renderedBody()).isEqualTo("Hi Alice"))
                .verifyComplete();

        verify(service).search(tenant, "welcome", "SMS");
        verify(service).preview(tenant, "welcome_sms", Map.of("firstName", "Alice"));
    }

    @Test
    void rejectsMissingTenantContext() {
        MessageTemplateService service = mock(MessageTemplateService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.error(new SecurityException("AUTH_003: missing tenant context")));
        MessageTemplateController controller = new MessageTemplateController(service, resolver);

        StepVerifier.create(controller.search(null, null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("AUTH_003"))
                .verify();
    }

    private TenantContext operator() {
        return new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
    }
}
