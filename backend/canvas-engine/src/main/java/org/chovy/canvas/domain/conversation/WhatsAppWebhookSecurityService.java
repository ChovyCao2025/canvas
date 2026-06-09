package org.chovy.canvas.domain.conversation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * WhatsAppWebhookSecurityService 编排 domain.conversation 场景的领域业务规则。
 */
@Service
public class WhatsAppWebhookSecurityService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final String verifyToken;
    private final String appSecret;

    /**
     * 创建 WhatsAppWebhookSecurityService 实例并注入 domain.conversation 场景依赖。
     * @param verifyToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @param appSecret app secret 参数，用于 WhatsAppWebhookSecurityService 流程中的校验、计算或对象转换。
     */
    public WhatsAppWebhookSecurityService(
            @Value("${canvas.conversation.whatsapp.webhook.verify-token:${canvas.conversation.whatsapp.verify-token:}}")
            String verifyToken,
            @Value("${canvas.conversation.whatsapp.webhook.app-secret:${canvas.conversation.whatsapp.app-secret:}}")
            String appSecret) {
        this.verifyToken = verifyToken == null ? "" : verifyToken;
        this.appSecret = appSecret == null ? "" : appSecret;
    }

    /**
     * 校验 WhatsApp Webhook 订阅挑战。
     * 只有 mode=subscribe、token 与配置一致且 challenge 非空时返回 challenge，否则抛出对应 HTTP 状态异常。
     */
    public String verifyChallenge(String mode, String token, String challenge) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (verifyToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "WhatsApp webhook verify token is not configured");
        }
        if (!"subscribe".equalsIgnoreCase(text(mode)) || isBlank(challenge)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid WhatsApp webhook challenge");
        }
        if (!constantTimeEquals(verifyToken.trim(), text(token))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid WhatsApp webhook verify token");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return challenge.trim();
    }

    /**
     * 校验 WhatsApp Webhook 请求体签名。
     * 使用配置的 appSecret 计算 HMAC-SHA256，并用常量时间比较校验 header，失败时以未授权异常拒绝入站消息。
     */
    public void verifySignature(String rawBody, String signatureHeader) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (appSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "WhatsApp webhook app secret is not configured");
        }
        if (isBlank(signatureHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing WhatsApp webhook signature");
        }
        String expected = SIGNATURE_PREFIX + hmacSha256(rawBody == null ? "" : rawBody);
        if (!constantTimeEquals(expected, signatureHeader.trim().toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid WhatsApp webhook signature");
        }
    }

    /**
     * 执行 hmacSha256 流程，围绕 hmac sha256 完成校验、计算或结果组装。
     *
     * @param rawBody raw body 参数，用于 hmacSha256 流程中的校验、计算或对象转换。
     * @return 返回 hmac sha256 生成的文本或业务键。
     */
    private String hmacSha256(String rawBody) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "WhatsApp webhook signature verification failed",
                    ex);
        }
    }

    /**
     * 执行 constantTimeEquals 流程，围绕 constant time equals 完成校验、计算或结果组装。
     *
     * @param expected 待处理业务值，用于规则计算、转换或外部调用。
     * @param actual actual 参数，用于 constantTimeEquals 流程中的校验、计算或对象转换。
     * @return 返回 constant time equals 的布尔判断结果。
     */
    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                (actual == null ? "" : actual).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 执行 text 流程，围绕 text 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 text 生成的文本或业务键。
     */
    private static String text(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
