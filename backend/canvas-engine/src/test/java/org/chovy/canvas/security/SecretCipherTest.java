package org.chovy.canvas.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretCipherTest {

    @Test
    void encryptsAndDecrypts() {
        SecretCipher cipher = SecretCipher.fromBase64Key("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");

        String encrypted = cipher.encrypt("db-password");

        assertThat(encrypted).isNotEqualTo("db-password");
        assertThat(encrypted).startsWith("v1:");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("db-password");
    }

    @Test
    void decryptKeepsLegacyPlaintextForMigrationCompatibility() {
        SecretCipher cipher = SecretCipher.fromBase64Key("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");

        assertThat(cipher.decrypt("legacy-password")).isEqualTo("legacy-password");
    }
}
