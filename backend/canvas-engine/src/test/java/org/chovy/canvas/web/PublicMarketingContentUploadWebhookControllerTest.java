package org.chovy.canvas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.domain.content.MarketingAssetUploadService;
import org.chovy.canvas.domain.content.MarketingAssetUploadWebhookSignatureService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicMarketingContentUploadWebhookControllerTest {

    @Test
    void verifiedProviderCallbackUsesTenantRouteAndSignedRawBody() {
        MarketingAssetUploadService uploadService = mock(MarketingAssetUploadService.class);
        MarketingAssetUploadWebhookSignatureService signatureService = signatureService();
        PublicMarketingContentUploadWebhookController controller =
                new PublicMarketingContentUploadWebhookController(uploadService, signatureService, new ObjectMapper());
        String rawBody = """
                {
                  "uploadToken": "upload-token",
                  "providerAssetId": "mux-asset-1",
                  "assetKey": "hero-video",
                  "assetType": "VIDEO",
                  "mimeType": "video/mp4",
                  "storageUrl": "https://stream.example.com/hero.mp4",
                  "status": "READY",
                  "transcodeStatus": "READY",
                  "sizeBytes": 12000,
                  "durationMs": 61000,
                  "posterUrl": "https://stream.example.com/poster.jpg",
                  "scanStatus": "PROVIDER_VERIFIED",
                  "metadata": {"providerEventId": "evt-1"}
                }
                """;
        String timestamp = timestamp();
        MarketingAssetUploadService.UploadIntentView view = new MarketingAssetUploadService.UploadIntentView(
                "mux-hero-video-upload",
                "hero-video",
                "VIDEO",
                "MUX",
                "upload-token",
                "/provider/mux/direct-upload",
                Map.of("assetKey", "hero-video"),
                "COMPLETED",
                "mux-asset-1",
                null);
        when(uploadService.handleCallback(
                argThat((TenantContext tenant) -> tenant.tenantId().equals(8L)
                        && RoleNames.OPERATOR.equals(tenant.role())
                        && "asset-webhook:mux".equals(tenant.username())),
                argThat((MarketingAssetUploadService.ProviderCallbackCommand command) ->
                        "mux".equals(command.provider())
                                && "upload-token".equals(command.uploadToken())
                                && "mux-asset-1".equals(command.providerAssetId()))))
                .thenReturn(view);

        StepVerifier.create(controller.handleProviderCallback(
                        8L,
                        "mux",
                        timestamp,
                        signatureService.sign(timestamp, rawBody),
                        rawBody))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().status()).isEqualTo("COMPLETED");
                })
                .verifyComplete();

        verify(uploadService).handleCallback(
                argThat((TenantContext tenant) -> tenant.tenantId().equals(8L)),
                argThat((MarketingAssetUploadService.ProviderCallbackCommand command) ->
                        "mux".equals(command.provider()) && "hero-video".equals(command.assetKey())));
    }

    @Test
    void invalidSignatureRejectsBeforeServiceCall() {
        MarketingAssetUploadService uploadService = mock(MarketingAssetUploadService.class);
        MarketingAssetUploadWebhookSignatureService signatureService = signatureService();
        PublicMarketingContentUploadWebhookController controller =
                new PublicMarketingContentUploadWebhookController(uploadService, signatureService, new ObjectMapper());

        StepVerifier.create(controller.handleProviderCallback(
                        8L,
                        "mux",
                        timestamp(),
                        "sha256=bad",
                        "{\"uploadToken\":\"upload-token\"}"))
                .expectErrorMatches(error -> error.getMessage().contains("invalid asset upload webhook signature"))
                .verify();
    }

    private MarketingAssetUploadWebhookSignatureService signatureService() {
        return new MarketingAssetUploadWebhookSignatureService(
                "asset-webhook-secret-asset-webhook-1234",
                300);
    }

    private String timestamp() {
        return String.valueOf(Instant.now().getEpochSecond());
    }
}
