package org.chovy.canvas.risk.domain.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 风控主体哈希工具。
 */
public final class RiskSubjectHashing {

    /**
     * 工具类禁止实例化。
     */
    private RiskSubjectHashing() {
    }

    /**
     * 对原始主体标识计算带算法前缀的 SHA-256 哈希。
     */
    public static String sha256(String rawSubject) {
        if (rawSubject == null) {
            return "sha256:";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(rawSubject.getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }
}
