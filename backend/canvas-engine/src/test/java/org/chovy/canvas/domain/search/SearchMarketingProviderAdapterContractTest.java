package org.chovy.canvas.domain.search;

import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchMarketingProviderAdapterContractTest {

    @Test
    void googleDryRunDelegatesValidationModeAndRedactsResponseSecrets() {
        SearchMarketingCredentialResolver credentialResolver = mock(SearchMarketingCredentialResolver.class);
        when(credentialResolver.resolve(eq(7L), eq("GOOGLE_ADS"), eq("google-main")))
                .thenReturn(credential("google-main", "GOOGLE_ADS"));
        GoogleAdsSearchMarketingTransport transport = (request, credential) ->
                SearchMarketingProviderMutationResult.success("google-operation-1", Map.of(
                        "validateOnly", request.dryRun(),
                        "partialFailure", request.partialFailure(),
                        "access_token", "raw-token"));
        GoogleAdsSearchMarketingProviderWriteClient client =
                new GoogleAdsSearchMarketingProviderWriteClient(credentialResolver, transport);

        SearchMarketingProviderMutationResult result = client.execute(request("GOOGLE_ADS", true));

        assertThat(result.success()).isTrue();
        assertThat(result.providerOperationId()).isEqualTo("google-operation-1");
        assertThat(result.response()).containsEntry("validateOnly", true);
        assertThat(result.response()).containsEntry("access_token", ProviderWriteEvidenceSanitizer.REDACTED);
    }

    @Test
    void microsoftDryRunUsesLocalValidationWhenProviderValidationIsUnavailable() {
        SearchMarketingCredentialResolver credentialResolver = mock(SearchMarketingCredentialResolver.class);
        MicrosoftAdsSearchMarketingTransport transport = mock(MicrosoftAdsSearchMarketingTransport.class);
        when(credentialResolver.resolve(eq(7L), eq("MICROSOFT_ADS"), eq("microsoft-main")))
                .thenReturn(credential("microsoft-main", "MICROSOFT_ADS"));
        MicrosoftAdsSearchMarketingProviderWriteClient client =
                new MicrosoftAdsSearchMarketingProviderWriteClient(credentialResolver, transport);

        SearchMarketingProviderMutationResult result = client.execute(request("MICROSOFT_ADS", true));

        assertThat(result.success()).isTrue();
        assertThat(result.response()).containsEntry("validationMode", "LOCAL");
        verify(transport, never()).mutate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void liveApplyReturnsProviderOperationAndSanitizedPartialFailureEvidence() {
        SearchMarketingCredentialResolver credentialResolver = mock(SearchMarketingCredentialResolver.class);
        when(credentialResolver.resolve(eq(7L), eq("GOOGLE_ADS"), eq("google-main")))
                .thenReturn(credential("google-main", "GOOGLE_ADS"));
        GoogleAdsSearchMarketingTransport transport = (request, credential) ->
                SearchMarketingProviderMutationResult.success("google-batch-1", Map.of(
                        "partialFailure", request.partialFailure(),
                        "operationErrors", List.of(Map.of(
                                "index", 1,
                                "client_secret", "raw-secret",
                                "message", "keyword already exists"))));
        GoogleAdsSearchMarketingProviderWriteClient client =
                new GoogleAdsSearchMarketingProviderWriteClient(credentialResolver, transport);

        SearchMarketingProviderMutationResult result = client.execute(request("GOOGLE_ADS", false));

        assertThat(result.success()).isTrue();
        assertThat(result.providerOperationId()).isEqualTo("google-batch-1");
        List<?> errors = (List<?>) result.response().get("operationErrors");
        Map<?, ?> error = (Map<?, ?>) errors.getFirst();
        assertThat(error.get("client_secret")).isEqualTo(ProviderWriteEvidenceSanitizer.REDACTED);
        assertThat(error.get("message")).isEqualTo("keyword already exists");
    }

    @Test
    void missingCompatibleCredentialFailsClosed() {
        SearchMarketingCredentialResolver credentialResolver = mock(SearchMarketingCredentialResolver.class);
        when(credentialResolver.resolve(eq(7L), eq("GOOGLE_ADS"), eq("google-main")))
                .thenReturn(SearchMarketingCredentialRef.unavailable("google-main", "GOOGLE_ADS",
                        "SEARCH_PROVIDER_CREDENTIAL_EXPIRED", "expired"));
        GoogleAdsSearchMarketingProviderWriteClient client =
                new GoogleAdsSearchMarketingProviderWriteClient(credentialResolver,
                        (request, credential) -> SearchMarketingProviderMutationResult.success("unexpected", Map.of()));

        SearchMarketingProviderMutationResult result = client.execute(request("GOOGLE_ADS", false));

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("SEARCH_PROVIDER_CREDENTIAL_UNAVAILABLE");
        assertThat(result.errorMessage()).contains("expired");
    }

    private SearchMarketingProviderMutationRequest request(String provider, boolean dryRun) {
        return new SearchMarketingProviderMutationRequest(
                7L,
                10L,
                provider,
                "ads-main",
                "123-456",
                "UPDATE_KEYWORD_BID",
                "KEYWORD",
                "customers/1/adGroupCriteria/2~3",
                "idem-1",
                dryRun,
                true,
                Map.of("bidMicros", 1500000),
                Map.of("credentialKey", provider.equals("GOOGLE_ADS") ? "google-main" : "microsoft-main"));
    }

    private SearchMarketingCredentialRef credential(String credentialKey, String providerType) {
        return new SearchMarketingCredentialRef(80L, credentialKey, providerType, "OAUTH2",
                "access-token", "developer-token", "refresh-token", null, Map.of("scope", "ads"),
                true, null, null);
    }
}
