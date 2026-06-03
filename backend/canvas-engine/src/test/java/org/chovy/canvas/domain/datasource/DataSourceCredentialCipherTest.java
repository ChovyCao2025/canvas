package org.chovy.canvas.domain.datasource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceCredentialCipherTest {

    @Test
    void encryptsPasswordWithVersionPrefixAndDecryptsIt() {
        DataSourceCredentialCipher cipher =
                new DataSourceCredentialCipher("test-datasource-credential-secret-32b");

        String encrypted = cipher.encrypt("root-password");

        assertThat(encrypted).startsWith("enc:v1:");
        assertThat(encrypted).doesNotContain("root-password");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("root-password");
    }

    @Test
    void decryptAcceptsLegacyPlaintextForBackfillCompatibility() {
        DataSourceCredentialCipher cipher =
                new DataSourceCredentialCipher("test-datasource-credential-secret-32b");

        assertThat(cipher.decrypt("legacy-password")).isEqualTo("legacy-password");
    }
}
