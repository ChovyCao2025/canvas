package org.chovy.canvas.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PII 数据脱敏工具（设计文档 13.8节）。
 * 在写入 execution_trace 和日志前调用，保护用户隐私。
 */
public final class DataMaskingUtil {

    /**
     * 构造 DataMaskingUtil 实例。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    private DataMaskingUtil() {}

    /** 手机号：保留前3后4，中间用 **** 替换。 */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(\\d{3})\\d{4}(\\d{4})");

    /** 身份证：保留前3后4。 */
    private static final Pattern ID_CARD_PATTERN =
            Pattern.compile("(\\d{3})\\d{11}(\\w{4})");

    /** 脱敏手机号：138****8888 */
    public static String maskPhone(String phone) {
        if (phone == null) return null;
        return PHONE_PATTERN.matcher(phone).replaceAll("$1****$2");
    }

    /** 脱敏身份证：110***********1234 */
    public static String maskIdCard(String id) {
        if (id == null) return null;
        return ID_CARD_PATTERN.matcher(id).replaceAll("$1***********$2");
    }

    /** 对 JSON 字符串中的敏感字段值进行脱敏（按字段名模糊匹配）*/
    public static String maskJson(String json, Set<String> sensitiveKeys) {
        if (json == null || json.isBlank()) return json;
        String result = json;
        for (String key : sensitiveKeys) {
            result = result.replaceAll(
                    "(?i)\"" + key + "\"\\s*:\\s*\"[^\"]*\"",
                    "\"" + key + "\":\"******\""
            );
            // 替换 "phone":"13812345678" → "phone":"138****5678"
            result = result.replaceAll(
                    "\"" + key + "\"\\s*:\\s*\"(\\d{3})\\d{4}(\\d{4})\"",
                    "\"" + key + "\":\"$1****$2\""
            );
            // 替换身份证
            result = result.replaceAll(
                    "\"" + key + "\"\\s*:\\s*\"(\\d{3})\\d{11}(\\w{4})\"",
                    "\"" + key + "\":\"$1***********$2\""
            );
        }
        return result;
    }

    /** 递归脱敏 Map/List/String，可在写 trace 或日志前使用。 */
    @SuppressWarnings("unchecked")
    public static Object maskObject(Object value) {
        return maskObject(value, DEFAULT_SENSITIVE_KEYS);
    }

    @SuppressWarnings("unchecked")
    public static Object maskObject(Object value, Set<String> sensitiveKeys) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> masked = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (isSensitiveKey(key, sensitiveKeys)) {
                    masked.put(key, "******");
                } else {
                    masked.put(key, maskObject(entry.getValue(), sensitiveKeys));
                }
            }
            return masked;
        }
        if (value instanceof List<?> list) {
            List<Object> masked = new ArrayList<>(list.size());
            for (Object item : list) {
                masked.add(maskObject(item, sensitiveKeys));
            }
            return masked;
        }
        if (value instanceof String text) {
            return maskIdCard(maskPhone(maskJson(text, sensitiveKeys)));
        }
        return value;
    }

    public static String maskText(String text) {
        Object masked = maskObject(text);
        return masked == null ? null : String.valueOf(masked);
    }

    private static boolean isSensitiveKey(String key, Set<String> sensitiveKeys) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase();
        return sensitiveKeys.stream()
                .map(String::toLowerCase)
                .anyMatch(normalized::contains);
    }

    /** 默认敏感字段集合 */
    public static final Set<String> DEFAULT_SENSITIVE_KEYS = Set.of(
            "phone", "mobile", "phoneNumber", "mobileNumber",
            "idCard", "idNumber", "identityCard",
            "bankCard", "cardNumber",
            "password", "passwd", "pwd",
            "token", "accessToken", "refreshToken",
            "secret", "apiKey", "authorization",
            "cookie", "session", "credential", "body"
    );
}
