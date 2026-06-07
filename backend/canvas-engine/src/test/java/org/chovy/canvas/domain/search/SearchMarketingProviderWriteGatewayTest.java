package org.chovy.canvas.domain.search;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SearchMarketingProviderWriteGatewayTest {

    @Test
    void dryRunDelegatesToRegisteredClientWhenSupported() {
        AtomicBoolean called = new AtomicBoolean(false);
        SearchMarketingProviderWriteClient client = new SearchMarketingProviderWriteClient() {
            @Override
            public boolean supports(SearchMarketingProviderMutationRequest request) {
                return "GOOGLE_ADS".equals(request.provider());
            }

            @Override
            public SearchMarketingProviderMutationResult execute(SearchMarketingProviderMutationRequest request) {
                called.set(true);
                assertThat(request.dryRun()).isTrue();
                return SearchMarketingProviderMutationResult.success("google-validate-1",
                        Map.of("adapter", "google_ads", "validatedByProvider", true));
            }
        };
        SearchMarketingProviderWriteGateway gateway = new SearchMarketingProviderWriteGateway(List.of(client));

        SearchMarketingProviderMutationResult result = gateway.execute(request(true));

        assertThat(called).isTrue();
        assertThat(result.success()).isTrue();
        assertThat(result.providerOperationId()).isEqualTo("google-validate-1");
        assertThat(result.response()).containsEntry("validatedByProvider", true);
    }

    @Test
    void dryRunFallsBackToLocalValidationWhenNoClientIsRegistered() {
        SearchMarketingProviderMutationResult result = SearchMarketingProviderWriteGateway.unsupported()
                .execute(request(true));

        assertThat(result.success()).isTrue();
        assertThat(result.providerOperationId()).isEqualTo("dry-run");
        assertThat(result.response()).containsEntry("validated", true);
    }

    @Test
    void liveApplyFailsClosedWhenNoClientIsRegistered() {
        SearchMarketingProviderMutationResult result = SearchMarketingProviderWriteGateway.unsupported()
                .execute(request(false));

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("PROVIDER_CLIENT_UNAVAILABLE");
    }

    private SearchMarketingProviderMutationRequest request(boolean dryRun) {
        return new SearchMarketingProviderMutationRequest(
                7L,
                10L,
                "GOOGLE_ADS",
                "ads-main",
                "123-456",
                "UPDATE_KEYWORD_BID",
                "KEYWORD",
                "customers/1/adGroupCriteria/2~3",
                "idem-1",
                dryRun,
                true,
                Map.of("bidMicros", 1500000),
                Map.of("source", "test"));
    }
}
