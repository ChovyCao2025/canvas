package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookIngestView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookIngestionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * PublicMarketingMonitoringWebhookController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/public/marketing-monitoring/webhooks")
public class PublicMarketingMonitoringWebhookController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final MarketingMonitorWebhookIngestionService service;

    /**
     * 创建 PublicMarketingMonitoringWebhookController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     */
    public PublicMarketingMonitoringWebhookController(MarketingMonitorWebhookIngestionService service) {
        this.service = service;
    }
    /**
     * 接收 公开营销监控 Webhook 入站数据接口，对应 POST /{tenantId}/{sourceKey}。
     * 接口按路径租户 ID 定位回调配置，签名、令牌或幂等控制由服务层完成。
     * 副作用：会接收入站事件并写入处理结果。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param tenantId 租户 ID。
     * @param sourceKey source 唯一键。
     * @param timestamp 请求头参数。
     * @param signature 请求头参数。
     * @param rawBody 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，包含接收 公开营销监控 Webhook 入站数据后的业务数据。
     */
    @PostMapping("/{tenantId}/{sourceKey}")
    public Mono<R<MarketingMonitorWebhookIngestView>> ingestWebhook(
            @PathVariable Long tenantId,
            @PathVariable String sourceKey,
            @RequestHeader("X-Canvas-Monitoring-Timestamp") String timestamp,
            @RequestHeader("X-Canvas-Monitoring-Signature") String signature,
            @RequestBody String rawBody) {
        return Mono.fromCallable(() -> R.ok(service.ingestWebhook(
                        tenantId,
                        sourceKey,
                        timestamp,
                        signature,
                        rawBody)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
