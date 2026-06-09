package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.engine.delivery.DeliveryReceiptLog;
import org.chovy.canvas.engine.delivery.DeliveryReceiptRequest;
import org.chovy.canvas.engine.delivery.DeliveryOutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DeliveryReceiptController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/delivery/receipts")
public class DeliveryReceiptController {

    /** 承接投递回执对发件箱状态的幂等更新逻辑。 */
    private final DeliveryOutboxService outboxService;
    /** 回执验签密钥，用于校验服务商回调来源可信。 */
    private final String receiptSecret;

    /**
     * 创建 DeliveryReceiptController 实例并注入 web 场景依赖。
     * @param outboxService 依赖组件，用于完成数据访问或外部能力调用。
     * @param receiptSecret receipt secret 参数，用于 DeliveryReceiptController 流程中的校验、计算或对象转换。
     */
    public DeliveryReceiptController(DeliveryOutboxService outboxService,
                                     @Value("${canvas.delivery.receipt.secret:}") String receiptSecret) {
        this.outboxService = outboxService;
        this.receiptSecret = receiptSecret;
    }
    /**
     * 接收Delivery Receipt回调接口，对应 POST 请求。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 主要委托 outboxService.recordReceipt 完成业务处理。
     * 副作用：会处理外部回调载荷。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param secret 请求头参数，可选。
     * @param req 请求体。
     * @return 异步返回统一响应，包含接收Delivery Receipt回调后的业务数据。
     */
    @PostMapping
    public Mono<R<DeliveryReceiptLog>> receive(@RequestHeader(value = "X-Canvas-Receipt-Secret", required = false) String secret,
                                               @RequestBody ReceiptCallbackReq req) {
        return Mono.fromCallable(() -> {
            requireValidSecret(secret);
            req.validate();
            DeliveryReceiptRequest request = new DeliveryReceiptRequest(
                    req.provider,
                    req.providerMessageId,
                    req.receiptType,
                    req.idempotencyKey,
                    req.receivedAt,
                    req.rawPayload == null ? Map.of() : req.rawPayload
            );
            return R.ok(outboxService.recordReceipt(request));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param candidate 时间参数，用于计算窗口、过期或审计时间。
     */
    private void requireValidSecret(String candidate) {
        if (receiptSecret == null || receiptSecret.isBlank()) {
            throw new AccessDeniedException("receipt secret is not configured");
        }
        byte[] expected = receiptSecret.getBytes(StandardCharsets.UTF_8);
        byte[] actual = (candidate == null ? "" : candidate).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new AccessDeniedException("invalid receipt signature");
        }
    }

    @Data
    /**
     * ReceiptCallbackReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class ReceiptCallbackReq {
        /** 投递服务商标识，用于匹配回执来源和发件箱记录。 */
        private String provider;
        /** 服务商侧消息 ID，用于关联投递请求和异步回执。 */
        private String providerMessageId;
        /** 回执类型，用于区分送达、失败、点击等投递事件。 */
        private String receiptType;
        /** 幂等键，用于避免重复投递或重复处理同一回执。 */
        private String idempotencyKey;
        /** 回执接收时间，用于记录投递事件进入平台的时间点。 */
        private LocalDateTime receivedAt;
        /** 服务商原始回执载荷，用于审计和异常排查。 */
        private Map<String, Object> rawPayload;

        /**
         * 校验输入、权限或业务前置条件。
         */
        void validate() {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (provider == null || provider.isBlank()) {
                throw new IllegalArgumentException("provider is required");
            }
            if (providerMessageId == null || providerMessageId.isBlank()) {
                throw new IllegalArgumentException("providerMessageId is required");
            }
            if (receiptType == null || receiptType.isBlank()) {
                throw new IllegalArgumentException("receiptType is required");
            }
        }
    }
}
