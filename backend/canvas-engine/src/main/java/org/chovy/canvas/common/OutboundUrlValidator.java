package org.chovy.canvas.common;

import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

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
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("url 不允许包含用户信息");
        }
        String asciiHost = IDN.toASCII(host);
        if (isBlockedHostLiteral(asciiHost)) {
            throw new IllegalArgumentException("url host 不允许访问内网或本机地址");
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(asciiHost);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("url host 无法解析", e);
        }
        if (addresses.length == 0) {
            throw new IllegalArgumentException("url host 无法解析");
        }
        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw new IllegalArgumentException("url host 不允许访问内网或本机地址");
            }
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
    private static boolean isBlockedHostLiteral(String host) {
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

    private static boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        if (address instanceof Inet4Address) {
            return isBlockedIpv4(address.getAddress());
        }
        if (address instanceof Inet6Address) {
            byte[] bytes = address.getAddress();
            int first = bytes[0] & 0xff;
            if ((first & 0xfe) == 0xfc) {
                return true;
            }
            if (isIpv4Mapped(bytes)) {
                return isBlockedIpv4(new byte[] {bytes[12], bytes[13], bytes[14], bytes[15]});
            }
        }
        return false;
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        if (bytes.length != 16) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff;
    }

    private static boolean isBlockedIpv4(byte[] raw) {
        int b0 = raw[0] & 0xff;
        int b1 = raw[1] & 0xff;
        int b2 = raw[2] & 0xff;
        if (b0 == 0 || b0 == 10 || b0 == 127 || b0 >= 224) return true;
        if (b0 == 169 && b1 == 254) return true;
        if (b0 == 172 && b1 >= 16 && b1 <= 31) return true;
        if (b0 == 192 && b1 == 168) return true;
        if (b0 == 100 && b1 >= 64 && b1 <= 127) return true;
        if (b0 == 192 && b1 == 0) return true;
        if (b0 == 192 && b1 == 0 && b2 == 2) return true;
        if (b0 == 198 && (b1 == 18 || b1 == 19)) return true;
        if (b0 == 198 && b1 == 51 && b2 == 100) return true;
        return b0 == 203 && b1 == 0 && b2 == 113;
    }
}
