package org.chovy.canvas.domain.cdp;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDatasourceCredentialMigrationTest {

    @Test
    void migrationReplacesRootRootDemoDatasourceCredentials() throws Exception {
        String sql = new ClassPathResource("db/migration/V354__sanitize_demo_datasource_credentials.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("UPDATE data_source_config");
        assertThat(sql).contains("UPDATE audience_definition");
        assertThat(sql).contains("canvas_demo_local_password");
        assertThat(sql).contains("username = 'root'");
        assertThat(sql).contains("password = 'root'");
    }
}
