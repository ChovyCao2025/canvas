package org.chovy.canvas.domain.cdp;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * WebhookSignatureService 编排 domain.cdp 场景的领域业务规则。
 */
@Service
public class WebhookSignatureService {
    private static final String ALGORITHM = "HmacSHA256";

    /**
     * 计算请求或回调签名，作为CDP 客户数据的服务入口。
     * <p>该方法不接收显式租户参数时，会依赖输入对象、密钥或已有记录携带的租户信息维持隔离。
     * 可能与外部供应商、Webhook 或上传交接端点交互。
     * @param secret secret 参数，用于 sign 流程中的校验、计算或对象转换。
     * @param timestamp 请求时间戳，参与签名计算并用于回放治理
     * @param rawPayload 未经改写的请求正文，参与签名计算以避免回调被篡改
     * @return 返回十六进制或 Base64 表示的签名字符串
     */
    public String sign(String secret, String timestamp, String rawPayload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal((timestamp + "\n" + rawPayload).getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(digest);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("webhook signature failed", e);
        }
    }

    /**
     * 校验请求或回调签名，作为CDP 客户数据的服务入口。
     * <p>该方法不接收显式租户参数时，会依赖输入对象、密钥或已有记录携带的租户信息维持隔离。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param secret secret 参数，用于 verify 流程中的校验、计算或对象转换。
     * @param timestamp 请求时间戳，参与签名计算并用于回放治理
     * @param rawPayload 未经改写的请求正文，参与签名计算以避免回调被篡改
     * @param supplied 调用方提供的签名值，用于和本地计算结果比对
     * @return 如果目标记录在租户边界内被成功更新或规则匹配则返回 true，否则返回 false
     */
    public boolean verify(String secret, String timestamp, String rawPayload, String supplied) {
        if (supplied == null) {
            return false;
        }
        String expected = sign(secret, timestamp, rawPayload);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
