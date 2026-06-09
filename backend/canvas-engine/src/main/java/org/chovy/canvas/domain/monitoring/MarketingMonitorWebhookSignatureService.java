package org.chovy.canvas.domain.monitoring;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;

/**
 * MarketingMonitorWebhookSignatureService 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
public class MarketingMonitorWebhookSignatureService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final int DEFAULT_TOLERANCE_SECONDS = 300;

    private final Clock clock;

    /**
     * 创建 MarketingMonitorWebhookSignatureService 实例并注入 domain.monitoring 场景依赖。
     */
    public MarketingMonitorWebhookSignatureService() {
        this(Clock.systemUTC());
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingMonitorWebhookSignatureService(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    /**
     * 计算请求或回调签名，作为营销监控的服务入口。
     * <p>该方法不接收显式租户参数时，会依赖输入对象、密钥或已有记录携带的租户信息维持隔离。
     * 可能与外部供应商、Webhook 或上传交接端点交互。
     * @param secret secret 参数，用于 sign 流程中的校验、计算或对象转换。
     * @param timestamp 请求时间戳，参与签名计算并用于回放治理
     * @param rawBody 未经改写的请求正文，参与签名计算以避免回调被篡改
     * @return 返回十六进制或 Base64 表示的签名字符串
     */
    public String sign(String secret, String timestamp, String rawBody) {
        if (isBlank(secret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "monitoring webhook secret is not configured");
        }
        if (isBlank(timestamp)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing monitoring webhook timestamp");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(message(timestamp, rawBody).getBytes(StandardCharsets.UTF_8));
            return SIGNATURE_PREFIX + HexFormat.of().formatHex(digest);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (ResponseStatusException ex) {
            throw ex;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "monitoring webhook signature verification failed",
                    ex);
        }
    }

    /**
     * 校验回调签名并在失败时中断处理，作为营销监控的服务入口。
     * <p>该方法不接收显式租户参数时，会依赖输入对象、密钥或已有记录携带的租户信息维持隔离。
     * 可能与外部供应商、Webhook 或上传交接端点交互。
     * @param secret secret 参数，用于 verifyOrThrow 流程中的校验、计算或对象转换。
     * @param timestamp 请求时间戳，参与签名计算并用于回放治理
     * @param rawBody 未经改写的请求正文，参与签名计算以避免回调被篡改
     * @param suppliedSignature 调用方提供的签名值，用于和本地计算结果比对
     * @param toleranceSeconds tolerance seconds 参数，用于 verifyOrThrow 流程中的校验、计算或对象转换。
     */
    public void verifyOrThrow(String secret,
                              String timestamp,
                              String rawBody,
                              String suppliedSignature,
                              Integer toleranceSeconds) {
        // 准备本次处理所需的上下文和中间变量。
        long eventEpochSeconds = parseTimestamp(timestamp);
        int tolerance = toleranceSeconds == null || toleranceSeconds <= 0
                ? DEFAULT_TOLERANCE_SECONDS
                : toleranceSeconds;
        long now = clock.instant().getEpochSecond();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (Math.abs(now - eventEpochSeconds) > tolerance) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "stale monitoring webhook timestamp");
        }
        if (isBlank(suppliedSignature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing monitoring webhook signature");
        }
        String expected = sign(secret, timestamp.trim(), rawBody);
        String supplied = suppliedSignature.trim().toLowerCase(Locale.ROOT);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid monitoring webhook signature");
        }
    }

    /**
     * 解析并校验输入数据。
     *
     * @param timestamp 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private long parseTimestamp(String timestamp) {
        if (isBlank(timestamp)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing monitoring webhook timestamp");
        }
        try {
            return Long.parseLong(timestamp.trim());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid monitoring webhook timestamp");
        }
    }

    /**
     * 执行 message 流程，围绕 message 完成校验、计算或结果组装。
     *
     * @param timestamp 时间参数，用于计算窗口、过期或审计时间。
     * @param rawBody raw body 参数，用于 message 流程中的校验、计算或对象转换。
     * @return 返回 message 生成的文本或业务键。
     */
    private String message(String timestamp, String rawBody) {
        return timestamp.trim() + "\n" + (rawBody == null ? "" : rawBody);
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
