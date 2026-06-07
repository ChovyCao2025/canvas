package org.chovy.canvas.domain.providerwrite;

import org.chovy.canvas.domain.creator.CreatorProviderMutationRequest;
import org.chovy.canvas.domain.creator.CreatorProviderMutationResult;
import org.chovy.canvas.domain.creator.SandboxCreatorProviderWriteClient;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationRequest;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationResult;
import org.chovy.canvas.domain.programmatic.SandboxProgrammaticDspProviderWriteClient;
import org.chovy.canvas.domain.search.SandboxSearchMarketingProviderWriteClient;
import org.chovy.canvas.domain.search.SearchMarketingProviderMutationRequest;
import org.chovy.canvas.domain.search.SearchMarketingProviderMutationResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SandboxProviderWriteClientTest {

    @Test
    void searchSandboxClientSupportsOnlySandboxProvidersAndReturnsDeterministicLiveEvidence() {
        SandboxSearchMarketingProviderWriteClient client = new SandboxSearchMarketingProviderWriteClient();

        assertThat(client.supports(searchRequest("GOOGLE_ADS_SANDBOX", false))).isTrue();
        assertThat(client.supports(searchRequest("GOOGLE_ADS", false))).isFalse();

        SearchMarketingProviderMutationResult first = client.execute(searchRequest("GOOGLE_ADS_SANDBOX", false));
        SearchMarketingProviderMutationResult second = client.execute(searchRequest("GOOGLE_ADS_SANDBOX", false));

        assertThat(first.success()).isTrue();
        assertThat(first.providerOperationId()).isEqualTo(second.providerOperationId());
        assertThat(first.response())
                .containsEntry("adapter", "sandbox")
                .containsEntry("domain", "search")
                .containsEntry("applied", true)
                .containsEntry("dryRun", false)
                .containsEntry("access_token", ProviderWriteEvidenceSanitizer.REDACTED);
    }

    @Test
    void creatorSandboxClientReturnsValidationEvidenceForDryRun() {
        SandboxCreatorProviderWriteClient client = new SandboxCreatorProviderWriteClient();

        CreatorProviderMutationResult result = client.execute(creatorRequest(true));

        assertThat(client.supports(creatorRequest(true))).isTrue();
        assertThat(result.success()).isTrue();
        assertThat(result.response())
                .containsEntry("adapter", "sandbox")
                .containsEntry("domain", "creator")
                .containsEntry("validated", true)
                .containsEntry("applied", false);
    }

    @Test
    void dspSandboxClientSupportsOnlySandboxProvidersAndReturnsLiveEvidence() {
        SandboxProgrammaticDspProviderWriteClient client = new SandboxProgrammaticDspProviderWriteClient();

        assertThat(client.supports(dspRequest("DV360_SANDBOX", false))).isTrue();
        assertThat(client.supports(dspRequest("DV360", false))).isFalse();

        ProgrammaticDspMutationResult result = client.execute(dspRequest("DV360_SANDBOX", false));

        assertThat(result.success()).isTrue();
        assertThat(result.response())
                .containsEntry("adapter", "sandbox")
                .containsEntry("domain", "programmatic")
                .containsEntry("applied", true);
    }

    private SearchMarketingProviderMutationRequest searchRequest(String provider, boolean dryRun) {
        return new SearchMarketingProviderMutationRequest(
                7L,
                10L,
                provider,
                "ads-main",
                "123-456",
                "UPDATE_KEYWORD_BID",
                "KEYWORD",
                "customers/1/adGroupCriteria/2~3",
                "idem-search-1",
                dryRun,
                true,
                Map.of("bidMicros", 1500000),
                Map.of("access_token", "raw-token"));
    }

    private CreatorProviderMutationRequest creatorRequest(boolean dryRun) {
        return new CreatorProviderMutationRequest(
                7L,
                20L,
                30L,
                40L,
                10L,
                "TIKTOK_SANDBOX",
                "REQUEST_CONTENT_AUTHORIZATION",
                "DELIVERABLE",
                "video-remote-1",
                "idem-creator-1",
                dryRun,
                true,
                Map.of("sparkAuthorizationCode", "AUTH-123"),
                Map.of());
    }

    private ProgrammaticDspMutationRequest dspRequest(String provider, boolean dryRun) {
        return new ProgrammaticDspMutationRequest(
                7L,
                10L,
                20L,
                30L,
                40L,
                provider,
                "seat-main",
                "advertisers/1",
                "UPDATE_LINE_ITEM_BID",
                "LINE_ITEM",
                "advertisers/1/lineItems/2",
                "idem-dsp-1",
                dryRun,
                true,
                Map.of("bidCpmMicros", 12500000),
                Map.of());
    }
}
