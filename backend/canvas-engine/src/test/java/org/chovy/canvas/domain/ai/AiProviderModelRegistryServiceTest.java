package org.chovy.canvas.domain.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiProviderModelRegistryServiceTest {

    @Test
    void createProviderMasksSecretAndRegistersModels() {
        AiProviderModelRegistryService service = new AiProviderModelRegistryService();

        AiProviderModelRegistryService.ProviderView provider = service.createProvider(42L,
                new AiProviderModelRegistryService.ProviderCreateRequest(
                        "openai",
                        "OpenAI",
                        "OPENAI_COMPATIBLE",
                        "https://api.example.test/v1",
                        "sk-test-secret",
                        true,
                        List.of(new AiProviderModelRegistryService.ModelCreateRequest(
                                "gpt-test",
                                "GPT Test",
                                "TEXT_JSON",
                                128000,
                                true))));

        assertThat(provider.tenantId()).isEqualTo(42L);
        assertThat(provider.maskedApiKey()).isEqualTo("****cret");
        assertThat(provider.toString()).doesNotContain("sk-test-secret");
        assertThat(service.listModels(42L, provider.id()))
                .extracting(AiProviderModelRegistryService.ModelView::modelKey)
                .containsExactly("gpt-test");
    }

    @Test
    void tenantScopedProvidersAreIsolatedButBuiltInProviderIsShared() {
        AiProviderModelRegistryService service = new AiProviderModelRegistryService();
        AiProviderModelRegistryService.ProviderView tenantProvider = service.createProvider(7L,
                new AiProviderModelRegistryService.ProviderCreateRequest(
                        "tenant-ai",
                        "Tenant AI",
                        "OPENAI_COMPATIBLE",
                        "https://tenant.example.test",
                        "tenant-secret",
                        true,
                        List.of()));

        assertThat(service.listProviders(7L))
                .extracting(AiProviderModelRegistryService.ProviderView::id)
                .contains(1L, tenantProvider.id());
        assertThat(service.listProviders(8L))
                .extracting(AiProviderModelRegistryService.ProviderView::id)
                .contains(1L)
                .doesNotContain(tenantProvider.id());
    }

    @Test
    void disabledProviderIsRejectedForCalls() {
        AiProviderModelRegistryService service = new AiProviderModelRegistryService();
        AiProviderModelRegistryService.ProviderView provider = service.createProvider(0L,
                new AiProviderModelRegistryService.ProviderCreateRequest(
                        "disabled-ai",
                        "Disabled AI",
                        "OPENAI_COMPATIBLE",
                        "https://disabled.example.test",
                        "disabled-secret",
                        true,
                        List.of()));

        service.disableProvider(0L, provider.id());

        assertThatThrownBy(() -> service.requireEnabledProvider(0L, provider.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disabled");
    }
}
