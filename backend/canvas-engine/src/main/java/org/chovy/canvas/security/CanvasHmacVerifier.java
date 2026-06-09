package org.chovy.canvas.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;

/**
 * CanvasHmacVerifier 处理 security 场景的认证、安全或签名逻辑。
 */
public class CanvasHmacVerifier {

    public static final String TIMESTAMP_HEADER = "X-Canvas-Timestamp";
    public static final String SIGNATURE_HEADER = "X-Canvas-Signature";

    private static final Duration MAX_SKEW = Duration.ofMinutes(5);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String secret;
    private final Clock clock;

    /**
     * 创建 CanvasHmacVerifier 实例并注入 security 场景依赖。
     * @param secret secret 参数，用于 CanvasHmacVerifier 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    public CanvasHmacVerifier(String secret, Clock clock) {
        this.secret = secret;
        this.clock = clock;
    }

    /**
     * verify 处理 security 场景的业务逻辑。
     * @param headers 待处理业务值，用于规则计算、转换或外部调用。
     * @param rawBody raw body 参数，用于 verify 流程中的校验、计算或对象转换。
     * @param subject 待处理业务值，用于规则计算、转换或外部调用。
     */
    public void verify(HttpHeaders headers, String rawBody, String subject) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (secret == null || secret.isBlank()
                || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw unauthorized(subject + "签名密钥未配置");
        }
        String timestamp = headers.getFirst(TIMESTAMP_HEADER);
        String signature = headers.getFirst(SIGNATURE_HEADER);
        if (timestamp == null || timestamp.isBlank() || signature == null || signature.isBlank()) {
            throw unauthorized(subject + "缺少签名头");
        }
        long timestampMs = parseTimestampMillis(timestamp, subject);
        long now = clock.millis();
        if (Math.abs(now - timestampMs) > MAX_SKEW.toMillis()) {
            throw unauthorized(subject + "签名已过期");
        }
        String expected = sign(timestamp.trim(), rawBody == null ? "" : rawBody, subject);
        String supplied = normalizeSignature(signature);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {
            throw unauthorized(subject + "签名无效");
        }
    }

    /**
     * 解析签名时间戳并统一为毫秒。
     *
     * @param timestamp 请求头中的时间戳，可为秒级或毫秒级
     * @param subject 错误信息中的业务主体说明
     * @return 毫秒级时间戳
     */
    private long parseTimestampMillis(String timestamp, String subject) {
        try {
            long value = Long.parseLong(timestamp.trim());
            return value < 10_000_000_000L ? value * 1000 : value;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException e) {
            throw unauthorized(subject + "时间戳不合法");
        }
    }

    /**
     * 使用共享密钥计算请求体签名。
     *
     * @param timestamp 已规范化的时间戳字符串
     * @param rawBody 原始请求体
     * @param subject 错误信息中的业务主体说明
     * @return 十六进制 HMAC-SHA256 签名
     */
    private String sign(String timestamp, String rawBody, String subject) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(
                    mac.doFinal((timestamp + "\n" + rawBody).getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw unauthorized(subject + "签名校验失败");
        }
    }

    /**
     * 规范化客户端传入的签名值。
     *
     * @param signature 请求头中的签名字符串
     * @return 去除可选 sha256= 前缀后的签名
     */
    private String normalizeSignature(String signature) {
        String trimmed = signature.trim();
        return trimmed.startsWith("sha256=") ? trimmed.substring("sha256=".length()) : trimmed;
    }

    /**
     * 构造统一的 401 响应异常。
     *
     * @param reason 认证失败原因
     * @return Spring 可识别的 401 异常
     */
    private ResponseStatusException unauthorized(String reason) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, reason);
    }
}
