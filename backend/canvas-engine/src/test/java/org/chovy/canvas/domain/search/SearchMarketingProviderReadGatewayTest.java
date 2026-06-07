package org.chovy.canvas.domain.search;

import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SearchMarketingProviderReadGatewayTest {

    @Test
    void sandboxClientReturnsPerformanceAndUrlInspectionRowsWithoutRawSecrets() {
        SandboxSearchMarketingProviderReadClient client = new SandboxSearchMarketingProviderReadClient();
        SearchMarketingSyncCommand command = command("SANDBOX_SEARCH", "PERFORMANCE",
                Map.of("access_token", "raw-access-token", "source", "test"));

        SearchMarketingProviderSyncResult result = client.sync(command, SearchMarketingCredentialRef.sandbox());

        assertThat(client.supports("SANDBOX_SEARCH", "PERFORMANCE")).isTrue();
        assertThat(result.success()).isTrue();
        assertThat(result.providerRequestId()).startsWith("sandbox-search-read-");
        assertThat(result.performanceRows()).isNotEmpty();
        assertThat(result.urlInspectionRows()).isEmpty();
        assertThat(result.evidence().toString()).doesNotContain("raw-access-token");
        Map<?, ?> metadata = (Map<?, ?>) result.evidence().get("metadata");
        assertThat(metadata.get("access_token")).isEqualTo(ProviderWriteEvidenceSanitizer.REDACTED);
        assertThat(metadata.get("source")).isEqualTo("test");
    }

    @Test
    void sandboxClientReturnsDeterministicRequestIdForSameWindow() {
        SandboxSearchMarketingProviderReadClient client = new SandboxSearchMarketingProviderReadClient();
        SearchMarketingSyncCommand command = command("SANDBOX_SEARCH", "SEO_TECHNICAL", Map.of());

        SearchMarketingProviderSyncResult first = client.sync(command, SearchMarketingCredentialRef.sandbox());
        SearchMarketingProviderSyncResult second = client.sync(command, SearchMarketingCredentialRef.sandbox());

        assertThat(first.providerRequestId()).isEqualTo(second.providerRequestId());
        assertThat(first.performanceRows()).isEmpty();
        assertThat(first.urlInspectionRows()).isNotEmpty();
    }

    @Test
    void gatewayFailsClosedWhenNoReadClientSupportsProvider() {
        SearchMarketingProviderReadGateway gateway = new SearchMarketingProviderReadGateway(List.of());

        SearchMarketingProviderSyncResult result = gateway.sync(command("GOOGLE_ADS", "PERFORMANCE", Map.of()),
                SearchMarketingCredentialRef.sandbox());

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("SEARCH_READ_CLIENT_UNAVAILABLE");
        assertThat(result.evidence()).containsEntry("provider", "GOOGLE_ADS");
    }

    @Test
    void gatewayFailsClosedWhenCredentialIsUnavailable() {
        SearchMarketingProviderReadGateway gateway =
                new SearchMarketingProviderReadGateway(List.of(new SandboxSearchMarketingProviderReadClient()));

        SearchMarketingProviderSyncResult result = gateway.sync(command("SANDBOX_SEARCH", "PERFORMANCE", Map.of()),
                SearchMarketingCredentialRef.unavailable("missing", "SANDBOX_SEARCH",
                        "SEARCH_PROVIDER_CREDENTIAL_NOT_FOUND", "not found"));

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("SEARCH_PROVIDER_CREDENTIAL_UNAVAILABLE");
    }

    private SearchMarketingSyncCommand command(String provider, String runType, Map<String, Object> metadata) {
        return new SearchMarketingSyncCommand(
                7L,
                10L,
                provider,
                "sandbox-account",
                runType,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 2),
                null,
                metadata);
    }
}
