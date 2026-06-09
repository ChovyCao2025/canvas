package org.chovy.canvas.domain.compliance;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PiiMaskingService 编排 domain.compliance 场景的领域业务规则。
 */
@Service
public class PiiMaskingService {

    private static final Pattern PHONE_TEXT_PATTERN = Pattern.compile("(?<!\\d)\\d{11}(?!\\d)");
    private static final Pattern EMAIL_TEXT_PATTERN = Pattern.compile(
            "([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
    private static final Pattern SECRET_ASSIGNMENT_PATTERN = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|token|secret|apiKey|api_key|credential|authorization)=([^\\s&]+)");

    /**
     * 脱敏手机号或疑似电话文本。
     * 标准 11 位手机号保留前三后四，其它格式按密钥脱敏策略处理。
     */
    public String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        if (phone.matches("\\d{11}")) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return maskSecret(phone);
    }

    /**
     * 脱敏邮箱地址。
     * 保留本地部分首尾字符和完整域名，非邮箱格式退回密钥脱敏，避免审计日志暴露完整联系信息。
     */
    public String maskEmail(String email) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (email == null) {
            return null;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return maskSecret(email);
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() == 1) {
            return local + "***" + domain;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return local.charAt(0) + "***" + local.substring(local.length() - 1) + domain;
    }

    /**
     * 脱敏外部平台 openId。
     * 较长 ID 保留首尾各四位，短 ID 按密钥处理，便于排查时关联同一主体但不暴露完整标识。
     */
    public String maskOpenId(String openId) {
        if (openId == null) {
            return null;
        }
        if (openId.length() <= 8) {
            return maskSecret(openId);
        }
        return openId.substring(0, 4) + "**********" + openId.substring(openId.length() - 4);
    }

    /**
     * 脱敏密钥、令牌或其它敏感字符串。
     * 返回值只保留末四位或固定星号，用于日志和导出包中的敏感字段展示。
     */
    public String maskSecret(String secret) {
        if (secret == null) {
            return null;
        }
        if (secret.length() <= 4) {
            return "****";
        }
        return "****" + secret.substring(secret.length() - 4);
    }

    /**
     * 对自由文本中的手机号、邮箱和 key=value 形式密钥做批量脱敏。
     * 空白文本原样返回，非敏感内容不会被修改。
     */
    public String maskText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String masked = maskRegex(text, PHONE_TEXT_PATTERN, match -> maskPhone(match.group()));
        masked = maskRegex(masked, EMAIL_TEXT_PATTERN, match -> maskEmail(match.group()));
        return maskRegex(masked, SECRET_ASSIGNMENT_PATTERN,
                match -> match.group(1) + "=" + maskSecret(match.group(2)));
    }

    /**
     * 递归脱敏审计或事件元数据中的敏感值。
     *
     * <p>方法会保留原有 Map/List 结构，按字段名识别手机号、邮箱、openId、密钥等敏感语义，
     * 对字符串内容也会执行自由文本脱敏。入参为 {@code null} 时返回空 Map，不修改原始 metadata。</p>
     *
     * @param metadata 需要进入日志、审计或导出结果的原始元数据
     * @return 已脱敏的新 Map，字段顺序尽量保持输入顺序
     */
    public Map<String, Object> maskMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return Map.of();
        }
        Map<String, Object> masked = new LinkedHashMap<>();
        metadata.forEach((key, value) -> masked.put(key, maskValue(key, value)));
        return masked;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    @SuppressWarnings("unchecked")
    private Object maskValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            map.forEach((nestedKey, nestedValue) ->
                    nested.put(String.valueOf(nestedKey), maskValue(String.valueOf(nestedKey), nestedValue)));
            return nested;
        }
        if (value instanceof List<?> list) {
            List<Object> masked = new ArrayList<>(list.size());
            for (Object item : list) {
                masked.add(item instanceof Map<?, ?> map
                        /**
                         * 解析、归一化或保护输入值，生成安全可用的中间结果。
                         *
                         * @param key 业务键，用于在同一租户下定位资源。
                         * @return 返回解析、归一化或安全处理后的值。
                         */
                        ? maskValue(key, (Map<String, Object>) map)
                        /**
                         * 解析、归一化或保护输入值，生成安全可用的中间结果。
                         *
                         * @param key 业务键，用于在同一租户下定位资源。
                         * @return 返回解析、归一化或安全处理后的值。
                         */
                        : maskValue(key, item));
            }
            return masked;
        }
        if (!(value instanceof String text)) {
            return value;
        }
        String normalized = normalize(key);
        if (normalized.contains("phone") || normalized.contains("mobile")) {
            return maskPhone(text);
        }
        if (normalized.contains("email") || normalized.contains("mail")) {
            return maskEmail(text);
        }
        if (normalized.contains("openid") || normalized.contains("open_id") || normalized.contains("unionid")
                || normalized.contains("union_id")) {
            return maskOpenId(text);
        }
        if (isSecretKey(normalized)) {
            return maskSecret(text);
        }
        return text;
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param normalizedKey 业务键，用于在同一租户下定位资源。
     * @return 返回布尔判断结果。
     */
    private boolean isSecretKey(String normalizedKey) {
        return normalizedKey.contains("secret")
                || normalizedKey.contains("token")
                || normalizedKey.contains("password")
                || normalizedKey.contains("passwd")
                || normalizedKey.contains("credential")
                || normalizedKey.contains("authorization")
                || normalizedKey.contains("apikey")
                || normalizedKey.contains("api_key")
                || normalizedKey.endsWith("key")
                || normalizedKey.contains("cookie")
                || normalizedKey.contains("session");
    }

    /**
     * 规范化输入值。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalize(String key) {
        return key == null ? "" : key.toLowerCase(Locale.ROOT);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param text text 参数，用于 maskRegex 流程中的校验、计算或对象转换。
     * @param pattern pattern 参数，用于 maskRegex 流程中的校验、计算或对象转换。
     * @param masker masker 参数，用于 maskRegex 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String maskRegex(String text, Pattern pattern, MatchMasker masker) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(masker.mask(matcher)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * MatchMasker 接口契约。
     */
    @FunctionalInterface
    private interface MatchMasker {
        /**
         * 解析、归一化或保护输入值，生成安全可用的中间结果。
         *
         * @param matcher matcher 参数，用于 mask 流程中的校验、计算或对象转换。
         * @return 返回解析、归一化或安全处理后的值。
         */
        String mask(Matcher matcher);
    }
}
