package org.chovy.canvas.common;

import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/**
 * OutboundUrlValidator 提供 common 场景的通用基础能力。
 */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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
                // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return normalized.startsWith("fc")
                || normalized.startsWith("fd")
                || normalized.startsWith("fe80:");
    }

    /**
     * 判断解析后的 IP 地址是否属于本机、内网、链路本地、多播或保留网段。
     *
     * @param address DNS 解析得到的地址
     * @return true 表示该地址禁止作为出站目标
     */
    private static boolean isBlockedAddress(InetAddress address) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return false;
    }

    /**
     * 判断 IPv6 字节数组是否为 IPv4-mapped 地址。
     *
     * @param bytes IPv6 地址字节
     * @return true 表示末尾 4 字节承载 IPv4 地址
     */
    private static boolean isIpv4Mapped(byte[] bytes) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (bytes.length != 16) {
            return false;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff;
    }

    /**
     * 判断 IPv4 地址是否落在内网、回环、链路本地、CGNAT、文档或多播等禁止网段。
     *
     * @param raw IPv4 地址的 4 字节表示
     * @return true 表示该 IPv4 地址禁止访问
     */
    private static boolean isBlockedIpv4(byte[] raw) {
        // 准备本次处理所需的上下文和中间变量。
        int b0 = raw[0] & 0xff;
        int b1 = raw[1] & 0xff;
        int b2 = raw[2] & 0xff;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (b0 == 0 || b0 == 10 || b0 == 127 || b0 >= 224) return true;
        if (b0 == 169 && b1 == 254) return true;
        if (b0 == 172 && b1 >= 16 && b1 <= 31) return true;
        if (b0 == 192 && b1 == 168) return true;
        if (b0 == 100 && b1 >= 64 && b1 <= 127) return true;
        if (b0 == 192 && b1 == 0) return true;
        if (b0 == 192 && b1 == 0 && b2 == 2) return true;
        if (b0 == 198 && (b1 == 18 || b1 == 19)) return true;
        if (b0 == 198 && b1 == 51 && b2 == 100) return true;
        // 汇总前面计算出的状态和明细，返回给调用方。
        return b0 == 203 && b1 == 0 && b2 == 113;
    }
}
