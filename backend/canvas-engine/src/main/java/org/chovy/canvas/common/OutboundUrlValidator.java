package org.chovy.canvas.common;

import java.net.URI;
import java.net.URISyntaxException;

public final class OutboundUrlValidator {
    private OutboundUrlValidator() {
    }

    public static void validateHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url 不能为空");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("url 格式不合法", e);
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("url 仅支持 http/https");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("url host 不能为空");
        }
        if (isBlockedHost(host)) {
            throw new IllegalArgumentException("url host 不允许访问内网或本机地址");
        }
    }

    private static boolean isBlockedHost(String host) {
        String normalized = host.toLowerCase();
        if ("localhost".equals(normalized) || normalized.endsWith(".localhost")
                || "0.0.0.0".equals(normalized) || "::1".equals(normalized)) {
            return true;
        }
        if (normalized.startsWith("127.") || normalized.startsWith("169.254.")) {
            return true;
        }
        if (normalized.startsWith("10.") || normalized.startsWith("192.168.")) {
            return true;
        }
        if (normalized.startsWith("172.")) {
            String[] parts = normalized.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return normalized.startsWith("fc")
                || normalized.startsWith("fd")
                || normalized.startsWith("fe80:");
    }
}
