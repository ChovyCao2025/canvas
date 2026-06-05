package org.chovy.canvas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.ai.AiProviderModelRegistryService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderControllerTest {

    @Test
    void createListUpdateAndDisableProviderWithoutLeakingSecret() {
        AiProviderModelRegistryService service = new AiProviderModelRegistryService();
        AiProviderController controller = new AiProviderController(service);
        AiProviderModelRegistryService.ProviderCreateRequest req =
                new AiProviderModelRegistryService.ProviderCreateRequest(
                        "openai",
                        "OpenAI",
                        "OPENAI_COMPATIBLE",
                        "https://api.example.test/v1",
                        "sk-raw-secret",
                        true,
                        List.of(new AiProviderModelRegistryService.ModelCreateRequest(
                                "gpt-test",
                                "GPT Test",
                                "TEXT_JSON",
                                128000,
                                true)));

        StepVerifier.create(controller.create(req))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().maskedApiKey()).isEqualTo("****cret");
                    assertThat(toJson(response.getData())).doesNotContain("sk-raw-secret");
                })
                .verifyComplete();

        Long providerId = service.listProviders(0L).stream()
                .filter(provider -> "openai".equals(provider.providerKey()))
                .findFirst()
                .orElseThrow()
                .id();

        StepVerifier.create(controller.detail(providerId))
                .assertNext(response -> assertThat(toJson(response.getData())).doesNotContain("sk-raw-secret"))
                .verifyComplete();

        StepVerifier.create(controller.update(providerId,
                        new AiProviderModelRegistryService.ProviderUpdateRequest(
                                "openai",
                                "OpenAI Updated",
                                "OPENAI_COMPATIBLE",
                                "https://api2.example.test/v1",
                                null,
                                true)))
                .assertNext(response -> assertThat(response.getData().displayName()).isEqualTo("OpenAI Updated"))
                .verifyComplete();

        StepVerifier.create(controller.disable(providerId))
                .assertNext(response -> assertThat(response.getCode()).isEqualTo(0))
                .verifyComplete();
        assertThat(service.getProvider(0L, providerId).enabled()).isFalse();
    }

    @Test
    void modelsEndpointListsProviderModels() {
        AiProviderModelRegistryService service = new AiProviderModelRegistryService();
        AiProviderController controller = new AiProviderController(service);

        StepVerifier.create(controller.models(1L))
                .assertNext(response -> assertThat(response.getData())
                        .extracting(AiProviderModelRegistryService.ModelView::modelKey)
                        .containsExactly("mock-marketing-v1"))
                .verifyComplete();
    }

    private static String toJson(Object value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
