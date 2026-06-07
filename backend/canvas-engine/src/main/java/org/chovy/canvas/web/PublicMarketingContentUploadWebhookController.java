package org.chovy.canvas.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.domain.content.MarketingAssetUploadService;
import org.chovy.canvas.domain.content.MarketingAssetUploadWebhookSignatureService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/public/marketing/content/assets/upload-callbacks")
public class PublicMarketingContentUploadWebhookController {

    private final MarketingAssetUploadService uploadService;
    private final MarketingAssetUploadWebhookSignatureService signatureService;
    private final ObjectMapper objectMapper;

    public PublicMarketingContentUploadWebhookController(MarketingAssetUploadService uploadService,
                                                        MarketingAssetUploadWebhookSignatureService signatureService,
                                                        ObjectMapper objectMapper) {
        this.uploadService = uploadService;
        this.signatureService = signatureService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    @PostMapping("/{tenantId}/{provider}")
    public Mono<R<MarketingAssetUploadService.UploadIntentView>> handleProviderCallback(
            @PathVariable Long tenantId,
            @PathVariable String provider,
            @RequestHeader("X-Canvas-Asset-Timestamp") String timestamp,
            @RequestHeader("X-Canvas-Asset-Signature") String signature,
            @RequestBody String rawBody) {
        return Mono.fromCallable(() -> {
                    signatureService.verifyOrThrow(timestamp, rawBody, signature);
                    MarketingAssetUploadService.ProviderCallbackCommand command = parse(provider, rawBody);
                    return R.ok(uploadService.handleCallback(
                            new TenantContext(tenantId, RoleNames.OPERATOR, "asset-webhook:" + provider),
                            command));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private MarketingAssetUploadService.ProviderCallbackCommand parse(String provider, String rawBody) {
        try {
            MarketingAssetUploadService.ProviderCallbackCommand parsed =
                    objectMapper.readValue(rawBody, MarketingAssetUploadService.ProviderCallbackCommand.class);
            if (parsed.provider() != null && !parsed.provider().isBlank()
                    && !parsed.provider().trim().equalsIgnoreCase(provider)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "asset upload provider does not match route");
            }
            return new MarketingAssetUploadService.ProviderCallbackCommand(
                    provider,
                    parsed.uploadToken(),
                    parsed.providerAssetId(),
                    parsed.assetKey(),
                    parsed.assetType(),
                    parsed.mimeType(),
                    parsed.storageUrl(),
                    parsed.status(),
                    parsed.transcodeStatus(),
                    parsed.sizeBytes(),
                    parsed.durationMs(),
                    parsed.width(),
                    parsed.height(),
                    parsed.posterUrl(),
                    parsed.checksumSha256(),
                    parsed.scanStatus(),
                    parsed.metadata());
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "asset upload callback payload must be JSON", ex);
        }
    }
}
