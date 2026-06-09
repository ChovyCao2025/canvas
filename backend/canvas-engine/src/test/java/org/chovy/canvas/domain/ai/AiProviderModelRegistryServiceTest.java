package org.chovy.canvas.domain.ai;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.chovy.canvas.dal.dataobject.AiProviderDO;
import org.chovy.canvas.dal.mapper.AiModelRegistryMapper;
import org.chovy.canvas.dal.mapper.AiProviderMapper;
import org.chovy.canvas.security.SecretCipher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiProviderModelRegistryServiceTest {

    private static final String CIPHER_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

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

    @Test
    void mapperBackedProviderEncryptsSecretAndDecryptsOnlyForCall() {
        AiProviderMapper providerMapper = mock(AiProviderMapper.class);
        AiModelRegistryMapper modelMapper = mock(AiModelRegistryMapper.class);
        SecretCipher cipher = SecretCipher.fromBase64Key(CIPHER_KEY);
        AiProviderModelRegistryService service = new AiProviderModelRegistryService(
                providerMapper,
                modelMapper,
                cipher);
        doAnswer(invocation -> {
            AiProviderDO row = invocation.getArgument(0);
            row.setId(101L);
            return 1;
        }).when(providerMapper).insert(any(AiProviderDO.class));

        AiProviderModelRegistryService.ProviderView provider = service.createProvider(42L,
                new AiProviderModelRegistryService.ProviderCreateRequest(
                        "deepseek",
                        "DeepSeek",
                        "OPENAI_COMPATIBLE",
                        "https://api.deepseek.example/v1",
                        "sk-live-secret",
                        true,
                        List.of()));

        ArgumentCaptor<AiProviderDO> captor = ArgumentCaptor.forClass(AiProviderDO.class);
        verify(providerMapper).insert(captor.capture());
        AiProviderDO inserted = captor.getValue();
        assertThat(inserted.getEncryptedApiKey()).startsWith("v1:");
        assertThat(inserted.getEncryptedApiKey()).doesNotContain("sk-live-secret");
        assertThat(provider.maskedApiKey()).isEqualTo("****cret");
        assertThat(provider.toString()).doesNotContain("sk-live-secret");

        when(providerMapper.selectById(101L)).thenReturn(inserted);

        AiProviderModelRegistryService.ProviderCallView callView =
                service.requireEnabledProviderForCall(42L, 101L);

        assertThat(callView.apiKey()).isEqualTo("sk-live-secret");
        assertThat(callView.providerType()).isEqualTo("OPENAI_COMPATIBLE");
    }

    @Test
    void mapperBackedReadViewsMaskPersistedSecretWithoutDecrypting() {
        AiProviderMapper providerMapper = mock(AiProviderMapper.class);
        AiModelRegistryMapper modelMapper = mock(AiModelRegistryMapper.class);
        SecretCipher cipher = mock(SecretCipher.class);
        AiProviderModelRegistryService service = new AiProviderModelRegistryService(
                providerMapper,
                modelMapper,
                cipher);
        AiProviderDO row = providerRow(301L, 42L, "ciphertext-value");
        when(providerMapper.selectList(any(Wrapper.class))).thenReturn(List.of(row));
        when(providerMapper.selectById(301L)).thenReturn(row);
        when(modelMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        AiProviderModelRegistryService.ProviderView listed = service.listProviders(42L).get(0);
        AiProviderModelRegistryService.ProviderView detailed = service.getProvider(42L, 301L);
        AiProviderModelRegistryService.ProviderView updated = service.updateProvider(42L, 301L,
                new AiProviderModelRegistryService.ProviderUpdateRequest(
                        null,
                        "Tenant AI Updated",
                        null,
                        null,
                        null,
                        true));

        assertThat(listed.maskedApiKey()).isEqualTo("****");
        assertThat(detailed.maskedApiKey()).isEqualTo("****");
        assertThat(updated.maskedApiKey()).isEqualTo("****");
        verify(cipher, never()).decrypt(any());
    }

    @Test
    void mapperBackedProviderRejectsCrossTenantCall() {
        AiProviderMapper providerMapper = mock(AiProviderMapper.class);
        AiModelRegistryMapper modelMapper = mock(AiModelRegistryMapper.class);
        AiProviderModelRegistryService service = new AiProviderModelRegistryService(
                providerMapper,
                modelMapper,
                SecretCipher.fromBase64Key(CIPHER_KEY));
        AiProviderDO row = new AiProviderDO();
        row.setId(201L);
        row.setTenantId(7L);
        row.setProviderKey("tenant-ai");
        row.setDisplayName("Tenant AI");
        row.setProviderType("OPENAI_COMPATIBLE");
        row.setEndpoint("https://tenant.example/v1");
        row.setEnabled(1);
        row.setEncryptedApiKey("v1:placeholder:ciphertext");
        when(providerMapper.selectById(201L)).thenReturn(row);

        assertThatThrownBy(() -> service.requireEnabledProviderForCall(8L, 201L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    private static AiProviderDO providerRow(Long id, Long tenantId, String encryptedApiKey) {
        AiProviderDO row = new AiProviderDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setProviderKey("tenant-ai");
        row.setDisplayName("Tenant AI");
        row.setProviderType("OPENAI_COMPATIBLE");
        row.setEndpoint("https://tenant.example/v1");
        row.setEnabled(1);
        row.setEncryptedApiKey(encryptedApiKey);
        return row;
    }
}
