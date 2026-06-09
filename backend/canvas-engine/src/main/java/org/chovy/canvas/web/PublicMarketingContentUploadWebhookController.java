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

/**
 * PublicMarketingContentUploadWebhookController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/public/marketing/content/assets/upload-callbacks")
public class PublicMarketingContentUploadWebhookController {

    private final MarketingAssetUploadService uploadService;
    private final MarketingAssetUploadWebhookSignatureService signatureService;
    private final ObjectMapper objectMapper;

    /**
     * 创建 PublicMarketingContentUploadWebhookController 实例并注入 web 场景依赖。
     * @param uploadService 依赖组件，用于完成数据访问或外部能力调用。
     * @param signatureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public PublicMarketingContentUploadWebhookController(MarketingAssetUploadService uploadService,
                                                        MarketingAssetUploadWebhookSignatureService signatureService,
                                                        ObjectMapper objectMapper) {
        this.uploadService = uploadService;
        this.signatureService = signatureService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }
    /**
     * 处理 公开营销素材上传Webhook请求接口，对应 POST /{tenantId}/{provider}。
     * 接口按路径租户 ID 定位回调配置，签名、令牌或幂等控制由服务层完成。
     * 主要委托 signatureService.verifyOrThrow, uploadService.handleCallback 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param tenantId 租户 ID。
     * @param provider 供应商过滤条件。
     * @param timestamp 请求头参数。
     * @param signature 请求头参数。
     * @param rawBody 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，包含处理 公开营销素材上传Webhook请求后的业务数据。
     */
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

    /**
     * 解析并校验输入数据。
     *
     * @param provider provider 参数，用于 parse 流程中的校验、计算或对象转换。
     * @param rawBody raw body 参数，用于 parse 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "asset upload callback payload must be JSON", ex);
        }
    }
}
