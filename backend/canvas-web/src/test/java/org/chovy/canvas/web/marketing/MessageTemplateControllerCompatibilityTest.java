package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.chovy.canvas.marketing.api.MessageTemplateFacade;
import org.chovy.canvas.marketing.application.MessageTemplateApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MessageTemplateControllerCompatibilityTest {

    @Test
    void exposesLegacyCreateSearchAndPreviewRoutesWithCompatibilityEnvelope() {
        WebTestClient client = webClient(new MessageTemplateApplicationService());

        client.post()
                .uri("/message-templates")
                .header("X-Tenant-Id", "8")
                .header("X-Actor", "operator-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "templateCode", "welcome_sms",
                        "displayName", "Welcome SMS",
                        "channel", "sms",
                        "body", "Hi {{firstName}}"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.templateCode").isEqualTo("welcome_sms")
                .jsonPath("$.data.channel").isEqualTo("SMS")
                .jsonPath("$.data.variables[0]").isEqualTo("firstName")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.get()
                .uri("/message-templates?keyword=welcome&channel=sms")
                .header("X-Tenant-Id", "8")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data[0].templateCode").isEqualTo("welcome_sms");

        client.post()
                .uri("/message-templates/welcome_sms/preview")
                .header("X-Tenant-Id", "8")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("firstName", "Ada"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.renderedBody").isEqualTo("Hi Ada")
                .jsonPath("$.data.missingVariables").isArray();
    }

    @Test
    void missingHeadersUseFinalCompatibilityDefaultsAndBadRequestsMapToApi001() {
        RecordingMessageTemplateFacade facade = new RecordingMessageTemplateFacade();

        webClient(facade)
                .post()
                .uri("/message-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "templateCode", "welcome_sms",
                        "displayName", "Welcome SMS",
                        "channel", "sms",
                        "body", "Hi"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(0)
                .jsonPath("$.data.createdBy").isEqualTo("system");

        assertThat(facade.lastTenantId).isEqualTo(0L);
        assertThat(facade.lastActor).isEqualTo("system");

        facade.failCreate = true;

        webClient(facade)
                .post()
                .uri("/message-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "templateCode", "bad",
                        "displayName", "Bad",
                        "channel", "fax",
                        "body", "Hi"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("unsupported template channel FAX")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(MessageTemplateFacade facade) {
        return WebTestClient.bindToController(new MessageTemplateController(facade)).build();
    }

    private static final class RecordingMessageTemplateFacade extends MessageTemplateApplicationService {
        private Long lastTenantId;
        private String lastActor;
        private boolean failCreate;

        @Override
        public TemplateView create(Long tenantId, String actor, TemplateDraft draft) {
            if (failCreate) {
                throw new IllegalArgumentException("unsupported template channel FAX");
            }
            lastTenantId = tenantId;
            lastActor = actor;
            return super.create(tenantId, actor, draft);
        }
    }
}
