package org.chovy.canvas.domain.compliance;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PiiMaskingService {

    private static final Pattern PHONE_TEXT_PATTERN = Pattern.compile("(?<!\\d)\\d{11}(?!\\d)");
    private static final Pattern EMAIL_TEXT_PATTERN = Pattern.compile(
            "([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
    private static final Pattern SECRET_ASSIGNMENT_PATTERN = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|token|secret|apiKey|api_key|credential|authorization)=([^\\s&]+)");

    public String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        if (phone.matches("\\d{11}")) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return maskSecret(phone);
    }

    public String maskEmail(String email) {
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
        return local.charAt(0) + "***" + local.substring(local.length() - 1) + domain;
    }

    public String maskOpenId(String openId) {
        if (openId == null) {
            return null;
        }
        if (openId.length() <= 8) {
            return maskSecret(openId);
        }
        return openId.substring(0, 4) + "**********" + openId.substring(openId.length() - 4);
    }

    public String maskSecret(String secret) {
        if (secret == null) {
            return null;
        }
        if (secret.length() <= 4) {
            return "****";
        }
        return "****" + secret.substring(secret.length() - 4);
    }

    public String maskText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String masked = maskRegex(text, PHONE_TEXT_PATTERN, match -> maskPhone(match.group()));
        masked = maskRegex(masked, EMAIL_TEXT_PATTERN, match -> maskEmail(match.group()));
        return maskRegex(masked, SECRET_ASSIGNMENT_PATTERN,
                match -> match.group(1) + "=" + maskSecret(match.group(2)));
    }

    public Map<String, Object> maskMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return Map.of();
        }
        Map<String, Object> masked = new LinkedHashMap<>();
        metadata.forEach((key, value) -> masked.put(key, maskValue(key, value)));
        return masked;
    }

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
                        ? maskValue(key, (Map<String, Object>) map)
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

    private String normalize(String key) {
        return key == null ? "" : key.toLowerCase(Locale.ROOT);
    }

    private String maskRegex(String text, Pattern pattern, MatchMasker masker) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(masker.mask(matcher)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    @FunctionalInterface
    private interface MatchMasker {
        String mask(Matcher matcher);
    }
}
