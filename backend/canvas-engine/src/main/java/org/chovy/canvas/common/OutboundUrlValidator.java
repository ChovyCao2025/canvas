package org.chovy.canvas.common;

import java.net.URI;
import java.net.URISyntaxException;

public final class OutboundUrlValidator {
    /**
     * 构造 OutboundUrlValidator 实例。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    private OutboundUrlValidator() {
    }

    /**
     * 校验 validate Http Url 相关的业务数据。
     *
     * <p>实现会调用外部 HTTP 服务，并对响应或异常进行业务化处理。
     *
     * @param url url 方法执行所需的业务参数
     */
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
        // 只允许 HTTP(S)，避免 file/jar/gopher 等协议被配置成出站请求目标。
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("url 仅支持 http/https");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("url host 不能为空");
        }
        // 阻断本机、链路本地和私网地址，降低 SSRF 访问内网资源的风险。
        if (isBlockedHost(host)) {
            throw new IllegalArgumentException("url host 不允许访问内网或本机地址");
        }
    }

    /**
     * 判断 is Blocked Host 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param host host 方法执行所需的业务参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private static boolean isBlockedHost(String host) {
        String normalized = host.toLowerCase();
        if ("localhost".equals(normalized) || normalized.endsWith(".localhost")
                || "0.0.0.0".equals(normalized) || "::1".equals(normalized)) {
            return true;
        }
        // IPv4 私网、回环和链路本地网段按前缀快速拦截。
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
                    // 172.16.0.0/12 是 RFC1918 私网段，不能作为出站 API 目标。
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
