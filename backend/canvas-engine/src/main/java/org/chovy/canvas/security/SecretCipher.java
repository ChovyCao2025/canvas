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

@Component
public class SecretCipher {

    private static final String PREFIX = "v1:";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom;

    @Autowired
    public SecretCipher(@Value("${canvas.secret-cipher.key:}") String base64Key) {
        this(decodeKey(base64Key), new SecureRandom());
    }

    private SecretCipher(byte[] keyBytes, SecureRandom secureRandom) {
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("secret cipher key must be 32 bytes for AES-256");
        }
        this.key = new SecretKeySpec(keyBytes, ALGORITHM);
        this.secureRandom = secureRandom;
    }

    public static SecretCipher fromBase64Key(String base64Key) {
        return new SecretCipher(decodeKey(base64Key), new SecureRandom());
    }

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
        } catch (Exception e) {
            throw new IllegalStateException("failed to encrypt secret", e);
        }
    }

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
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to decrypt secret", e);
        }
    }

    private boolean isEncrypted(String value) {
        return value.startsWith(PREFIX);
    }

    private static byte[] decodeKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("secret cipher key is required");
        }
        return Base64.getDecoder().decode(base64Key);
    }
}
