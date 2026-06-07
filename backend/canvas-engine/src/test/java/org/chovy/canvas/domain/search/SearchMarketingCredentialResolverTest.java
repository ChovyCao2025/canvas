package org.chovy.canvas.domain.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialMapper;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.chovy.canvas.security.SecretCipher;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchMarketingCredentialResolverTest {

    private static final String CIPHER_KEY = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneId.of("UTC"));

    @Test
    void activeProviderCredentialResolvesRuntimeTokensWithoutJsonOrStringLeakage() throws Exception {
        SecretCipher cipher = SecretCipher.fromBase64Key(CIPHER_KEY);
        MarketingMonitorProviderCredentialMapper mapper = mock(MarketingMonitorProviderCredentialMapper.class);
        when(mapper.selectOne(any())).thenReturn(credential(cipher, "ACTIVE",
                LocalDateTime.of(2026, 6, 7, 0, 0)));
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        SearchMarketingCredentialResolver resolver =
                new SearchMarketingCredentialResolver(mapper, cipher, objectMapper, CLOCK);

        SearchMarketingCredentialRef ref = resolver.resolve(7L, "GOOGLE_ADS", "google-main");

        assertThat(ref.available()).isTrue();
        assertThat(ref.credentialId()).isEqualTo(55L);
        assertThat(ref.accessToken()).isEqualTo("raw-access-token");
        assertThat(ref.developerToken()).isEqualTo("raw-developer-token");
        assertThat(ref.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(ref.safeMetadata().get("access_token")).isEqualTo(ProviderWriteEvidenceSanitizer.REDACTED);
        String serialized = objectMapper.writeValueAsString(ref);
        assertThat(serialized)
                .doesNotContain("raw-access-token")
                .doesNotContain("raw-developer-token")
                .doesNotContain("raw-refresh-token")
                .doesNotContain("raw-client-secret")
                .doesNotContain("accessToken")
                .doesNotContain("developerToken")
                .doesNotContain("refreshToken");
        assertThat(ref.toString())
                .doesNotContain("raw-access-token")
                .doesNotContain("raw-developer-token")
                .doesNotContain("raw-refresh-token");
    }

    @Test
    void disabledCredentialReturnsUnavailableReferenceWithoutDecryptingSecrets() {
        SecretCipher cipher = SecretCipher.fromBase64Key(CIPHER_KEY);
        MarketingMonitorProviderCredentialMapper mapper = mock(MarketingMonitorProviderCredentialMapper.class);
        when(mapper.selectOne(any())).thenReturn(credential(cipher, "DISABLED",
                LocalDateTime.of(2026, 6, 7, 0, 0)));
        SearchMarketingCredentialResolver resolver =
                new SearchMarketingCredentialResolver(mapper, cipher, new ObjectMapper(), CLOCK);

        SearchMarketingCredentialRef ref = resolver.resolve(7L, "GOOGLE_ADS", "google-main");

        assertThat(ref.available()).isFalse();
        assertThat(ref.errorCode()).isEqualTo("SEARCH_PROVIDER_CREDENTIAL_DISABLED");
        assertThat(ref.accessToken()).isNull();
    }

    @Test
    void expiredCredentialReturnsUnavailableReference() {
        SecretCipher cipher = SecretCipher.fromBase64Key(CIPHER_KEY);
        MarketingMonitorProviderCredentialMapper mapper = mock(MarketingMonitorProviderCredentialMapper.class);
        when(mapper.selectOne(any())).thenReturn(credential(cipher, "ACTIVE",
                LocalDateTime.of(2026, 6, 5, 23, 59)));
        SearchMarketingCredentialResolver resolver =
                new SearchMarketingCredentialResolver(mapper, cipher, new ObjectMapper(), CLOCK);

        SearchMarketingCredentialRef ref = resolver.resolve(7L, "GOOGLE_ADS", "google-main");

        assertThat(ref.available()).isFalse();
        assertThat(ref.errorCode()).isEqualTo("SEARCH_PROVIDER_CREDENTIAL_EXPIRED");
    }

    @Test
    void sandboxProviderResolvesWithoutStoredCredential() {
        SearchMarketingCredentialResolver resolver = new SearchMarketingCredentialResolver(
                mock(MarketingMonitorProviderCredentialMapper.class),
                SecretCipher.fromBase64Key(CIPHER_KEY),
                new ObjectMapper(),
                CLOCK);

        SearchMarketingCredentialRef ref = resolver.resolve(7L, "SANDBOX_SEARCH", null);

        assertThat(ref.available()).isTrue();
        assertThat(ref.providerType()).isEqualTo("SANDBOX_SEARCH");
        assertThat(ref.safeMetadata()).containsEntry("mode", "sandbox");
    }

    private MarketingMonitorProviderCredentialDO credential(SecretCipher cipher,
                                                           String status,
                                                           LocalDateTime expiresAt) {
        MarketingMonitorProviderCredentialDO row = new MarketingMonitorProviderCredentialDO();
        row.setId(55L);
        row.setTenantId(7L);
        row.setCredentialKey("google-main");
        row.setProviderType("GOOGLE_ADS");
        row.setAuthType("OAUTH2");
        row.setStatus(status);
        row.setAccessTokenCiphertext(cipher.encrypt("raw-access-token"));
        row.setRefreshTokenCiphertext(cipher.encrypt("raw-refresh-token"));
        row.setApiKeyCiphertext(cipher.encrypt("raw-developer-token"));
        row.setClientSecretCiphertext(cipher.encrypt("raw-client-secret"));
        row.setExpiresAt(expiresAt);
        row.setMetadataJson("""
                {
                  "accountId": "123-456",
                  "access_token": "metadata-access-token",
                  "nested": {
                    "developer_token": "metadata-developer-token",
                    "note": "safe"
                  }
                }
                """);
        return row;
    }
}
