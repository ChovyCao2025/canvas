package org.chovy.canvas.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SecretCipher 处理 security 场景的认证、安全或签名逻辑。
 */
@Component
public class SecretCipher {

    private static final String PREFIX = "v1:";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom;

    /**
     * 创建 SecretCipher 实例并注入 security 场景依赖。
     * @param base64Key 业务键，用于在同一租户下定位资源。
     */
    @Autowired
    public SecretCipher(@Value("${canvas.secret-cipher.key:}") String base64Key) {
        this(decodeKey(base64Key), new SecureRandom());
    }

    /**
     * 使用已解码密钥和随机源创建加解密器。
     *
     * @param keyBytes AES-256 原始密钥字节
     * @param secureRandom IV 随机数来源
     */
    private SecretCipher(byte[] keyBytes, SecureRandom secureRandom) {
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("secret cipher key must be 32 bytes for AES-256");
        }
        this.key = new SecretKeySpec(keyBytes, ALGORITHM);
        this.secureRandom = secureRandom;
    }

    /**
     * fromBase64Key 校验或转换 security 场景的数据。
     * @param base64Key 业务键，用于在同一租户下定位资源。
     * @return 返回组装或转换后的结果对象。
     */
    public static SecretCipher fromBase64Key(String base64Key) {
        return new SecretCipher(decodeKey(base64Key), new SecureRandom());
    }

    /**
     * encrypt 处理 security 场景的业务逻辑。
     * @param plaintext plaintext 参数，用于 encrypt 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return PREFIX
                    + Base64.getEncoder().encodeToString(iv)
                    + ":"
                    + Base64.getEncoder().encodeToString(ciphertext);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("failed to encrypt secret", e);
        }
    }

    /**
     * decrypt 处理 security 场景的业务逻辑。
     * @param ciphertext ciphertext 参数，用于 decrypt 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank() || !isEncrypted(ciphertext)) {
            return ciphertext;
        }
        String[] parts = ciphertext.split(":", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("invalid encrypted secret format");
        }
        try {
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to decrypt secret", e);
        }
    }

    /**
     * 判断密文是否使用当前版本前缀。
     *
     * @param value 待判断的存储值
     * @return true 表示该值需要走解密流程
     */
    private boolean isEncrypted(String value) {
        return value.startsWith(PREFIX);
    }

    /**
     * 解码 Base64 格式的 AES-256 密钥。
     *
     * @param base64Key 配置中的 Base64 密钥
     * @return 原始密钥字节
     */
    private static byte[] decodeKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("secret cipher key is required");
        }
        return Base64.getDecoder().decode(base64Key);
    }
}
