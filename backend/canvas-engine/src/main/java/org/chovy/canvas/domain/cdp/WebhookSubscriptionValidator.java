package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.common.OutboundUrlValidator;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WebhookSubscriptionValidator 编排 domain.cdp 场景的领域业务规则。
 */
@Service
public class WebhookSubscriptionValidator {

    /**
     * 校验 Webhook 订阅的回调地址和事件类型。
     *
     * <p>方法会拒绝不允许的出站 URL，并要求事件类型列表去空白、去重后非空；只做订阅输入校验，
     * 不发起回调探测，也不写入订阅记录。</p>
     *
     * @param callbackUrl 订阅方接收事件的 HTTP/HTTPS 回调地址
     * @param eventTypes 订阅的 CDP 事件类型列表
     */
    public void validate(String callbackUrl, List<String> eventTypes) {
        try {
            OutboundUrlValidator.validateHttpUrl(callbackUrl);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("callbackUrl is not allowed: " + ex.getMessage(), ex);
        }
        List<String> normalized = eventTypes == null ? List.of() : eventTypes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("eventTypes cannot be empty");
        }
    }
}
