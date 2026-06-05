package org.chovy.canvas.infrastructure.doris;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DORIS_ENABLED", matches = "true")
@TestPropertySource(properties = "canvas.doris.enabled=true")
class DorisConnectionTest {

    @Autowired
    @Qualifier("dorisDataSource")
    private DataSource dorisDataSource;

    @Test
    void dorisJdbcConnects() {
        JdbcTemplate dorisJdbc = new JdbcTemplate(dorisDataSource);
        Integer result = dorisJdbc.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }
}
