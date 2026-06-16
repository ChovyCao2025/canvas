package org.chovy.canvas.platform.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开入口目录，提供营销表单和第三方 webhook 的演示处理逻辑。
 */
public class PublicIngressCatalog {

    /**
     * 查询公开营销表单配置。
     *
     * @param publicKey 公开访问键
     * @return 表单配置记录
     */
    public Map<String, Object> publicMarketingForm(String publicKey) {
        requireText(publicKey, "publicKey is required");
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("publicKey", publicKey.trim());
        form.put("name", "Lead capture");
        form.put("formName", "Public form " + publicKey.trim());
        form.put("fieldSchema", List.of("email", "company", "message"));
        form.put("successMessage", "Thanks, we received your request.");
        form.put("active", true);
        return form;
    }

    /**
     * 提交公开营销表单。
     *
     * @param publicKey 公开访问键
     * @param payload 表单提交内容
     * @param headers 请求头
     * @return 提交结果
     */
    public Map<String, Object> submitMarketingForm(String publicKey, Map<String, Object> payload,
                                                   Map<String, String> headers) {
        requireText(publicKey, "publicKey is required");
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", true);
        result.put("publicKey", publicKey.trim());
        result.put("submissionId", stableId("submission", publicKey, String.valueOf(safePayload)));
        result.put("anonymousId", safePayload.get("anonymousId"));
        result.put("idempotencyKey", safePayload.get("idempotencyKey"));
        result.put("consentStatus", safePayload.get("consentStatus"));
        result.put("receivedAt", Instant.EPOCH.toString());
        result.put("userAgent", header(headers, "User-Agent"));
        return result;
    }

    /**
     * 兼容按字段传递的公开营销表单提交。
     *
     * @param publicKey 公开访问键
     * @param response 表单回答
     * @param utm UTM 参数
     * @param anonymousId 匿名访客标识
     * @param idempotencyKey 幂等键
     * @param consentChannel 授权渠道
     * @param consentStatus 授权状态
     * @param userAgent 用户代理
     * @param ipHash IP 哈希值
     * @return 提交结果
     */
    public Map<String, Object> submitMarketingForm(String publicKey, Map<String, Object> response,
                                                   Map<String, Object> utm, String anonymousId,
                                                   String idempotencyKey, String consentChannel,
                                                   String consentStatus, String userAgent, String ipHash) {
        requireText(publicKey, "publicKey is required");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", true);
        result.put("publicKey", publicKey.trim());
        result.put("submissionId", stableId("submission", publicKey, String.valueOf(response)));
        result.put("response", response == null ? Map.of() : response);
        result.put("utm", utm == null ? Map.of() : utm);
        result.put("anonymousId", anonymousId);
        result.put("idempotencyKey", idempotencyKey);
        result.put("consentChannel", consentChannel);
        result.put("consentStatus", consentStatus);
        result.put("userAgent", userAgent);
        result.put("ipHash", ipHash);
        return result;
    }

    /**
     * 校验 WhatsApp 接入挑战。
     *
     * @param tenantId 租户标识
     * @param mode 校验模式
     * @param verifyToken 校验令牌
     * @param challenge 挑战字符串
     * @return 原样返回的挑战字符串
     */
    public String verifyWhatsApp(Long tenantId, String mode, String verifyToken, String challenge) {
        requireTenant(tenantId);
        requireText(challenge, "hub.challenge is required");
        Map<String, Object> verification = new LinkedHashMap<>();
        verification.put("tenantId", tenantId);
        verification.put("mode", mode);
        verification.put("verifyTokenPresent", verifyToken != null && !verifyToken.isBlank());
        return challenge;
    }

    /**
     * 接收 WhatsApp webhook。
     *
     * @param tenantId 租户标识
     * @param signature 请求签名
     * @param rawBody 原始请求体
     * @return 解析后的事件记录列表
     */
    public List<Map<String, Object>> receiveWhatsApp(Long tenantId, String signature, String rawBody) {
        requireTenant(tenantId);
        requireJson(rawBody, "whatsapp webhook payload must be JSON");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("provider", "WHATSAPP");
        result.put("channel", "WHATSAPP");
        result.put("source", "whatsapp-webhook");
        result.put("accepted", true);
        result.put("signaturePresent", signature != null && !signature.isBlank());
        result.put("messageCount", rawBody.contains("entry") ? 1 : 0);
        result.put("ingressId", stableId("whatsapp", tenantId.toString(), rawBody));
        return List.of(result);
    }

    /**
     * 接收素材上传回调。
     *
     * @param tenantId 租户标识
     * @param provider 路由中的供应方
     * @param timestamp 回调时间戳
     * @param signature 请求签名
     * @param rawBody 原始请求体
     * @return 回调处理结果
     */
    public Map<String, Object> receiveAssetUploadCallback(Long tenantId, String provider, String timestamp,
                                                          String signature, String rawBody) {
        requireTenant(tenantId);
        requireText(provider, "provider is required");
        requireJson(rawBody, "asset upload callback payload must be JSON");
        String payloadProvider = jsonString(rawBody, "provider");
        if (payloadProvider != null && !payloadProvider.equalsIgnoreCase(provider.trim())) {
            // 路由供应方和负载供应方不一致时拒绝，避免回调误投递到错误租户配置。
            throw new IllegalArgumentException("asset upload provider does not match route");
        }
        Map<String, Object> result = callback("asset-upload", tenantId, rawBody);
        result.put("provider", provider.trim());
        result.put("uploadToken", valueOrDefault(jsonString(rawBody, "uploadToken"), "upload-" + tenantId));
        result.put("status", valueOrDefault(jsonString(rawBody, "status"), "RECEIVED"));
        result.put("timestamp", timestamp);
        result.put("signaturePresent", signature != null && !signature.isBlank());
        return result;
    }

    /**
     * 接收监控 webhook。
     *
     * @param tenantId 租户标识
     * @param sourceKey 监控来源键
     * @param timestamp 回调时间戳
     * @param signature 请求签名
     * @param rawBody 原始请求体
     * @return webhook 处理结果
     */
    public Map<String, Object> receiveMonitoringWebhook(Long tenantId, String sourceKey, String timestamp,
                                                        String signature, String rawBody) {
        requireTenant(tenantId);
        requireText(sourceKey, "sourceKey is required");
        requireJson(rawBody, "marketing monitoring webhook payload must be JSON");
        Map<String, Object> result = callback("monitoring", tenantId, rawBody);
        result.put("sourceKey", sourceKey.trim());
        result.put("timestamp", timestamp);
        result.put("signaturePresent", signature != null && !signature.isBlank());
        return result;
    }

    /**
     * 构造通用回调结果。
     *
     * @param kind 回调类型
     * @param tenantId 租户标识
     * @param rawBody 原始请求体
     * @return 回调结果
     */
    private static Map<String, Object> callback(String kind, Long tenantId, String rawBody) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("kind", kind);
        result.put("accepted", true);
        result.put("rawSize", rawBody.length());
        result.put("callbackId", stableId(kind, tenantId.toString(), rawBody));
        return result;
    }

    /**
     * 从请求头中读取指定字段。
     *
     * @param headers 请求头
     * @param name 请求头名称
     * @return 请求头值；不存在时返回 null
     */
    private static String header(Map<String, String> headers, String name) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        return headers.get(name);
    }

    /**
     * 校验租户标识有效。
     *
     * @param tenantId 租户标识
     */
    private static void requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId is required");
        }
    }

    /**
     * 校验文本必填。
     *
     * @param value 原始文本
     * @param message 校验失败时使用的异常消息
     */
    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 校验请求体外形是 JSON。
     *
     * @param rawBody 原始请求体
     * @param message 校验失败时使用的异常消息
     */
    private static void requireJson(String rawBody, String message) {
        requireText(rawBody, message);
        String trimmed = rawBody.trim();
        if (!(trimmed.startsWith("{") && trimmed.endsWith("}")) && !(trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 从简单 JSON 字符串中读取字符串字段。
     *
     * @param rawBody 原始 JSON 文本
     * @param field 字段名
     * @return 字符串字段值；不存在时返回 null
     */
    private static String jsonString(String rawBody, String field) {
        String marker = "\"" + field + "\"";
        int fieldIndex = rawBody.indexOf(marker);
        if (fieldIndex < 0) {
            return null;
        }
        int colon = rawBody.indexOf(':', fieldIndex + marker.length());
        int startQuote = colon < 0 ? -1 : rawBody.indexOf('"', colon + 1);
        int endQuote = startQuote < 0 ? -1 : rawBody.indexOf('"', startQuote + 1);
        if (endQuote < 0) {
            return null;
        }
        String value = rawBody.substring(startQuote + 1, endQuote);
        return value.isBlank() ? null : value;
    }

    /**
     * 返回非空文本或默认值。
     *
     * @param value 原始文本
     * @param defaultValue 默认值
     * @return 可用文本
     */
    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 根据输入生成稳定短标识。
     *
     * @param prefix 标识前缀
     * @param left 左侧输入
     * @param right 右侧输入
     * @return 稳定短标识
     */
    private static String stableId(String prefix, String left, String right) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((left + ":" + right).getBytes(StandardCharsets.UTF_8));
            return prefix + "-" + HexFormat.of().formatHex(bytes).substring(0, 12);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }
}
