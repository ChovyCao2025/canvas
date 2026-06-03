package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.chovy.canvas.security.SecretCipher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcConfigResolverTest {

    @Test
    void resolveDecryptsStoredPassword() throws Exception {
        SecretCipher cipher = SecretCipher.fromBase64Key("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        DataSourceConfigDO dataSource = new DataSourceConfigDO();
        dataSource.setId(7L);
        dataSource.setType("JDBC");
        dataSource.setUrl("jdbc:mysql://localhost:3306/cdp");
        dataSource.setUsername("cdp_app");
        dataSource.setPassword(cipher.encrypt("db-password"));
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setEnabled(1);
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        when(mapper.selectById(7L)).thenReturn(dataSource);
        JdbcConfigResolver resolver = new JdbcConfigResolver(new ObjectMapper(), mapper, cipher);

        JdbcConfig config = resolver.resolve("""
                {"dataSourceId":7,"baseTable":"users","userIdColumn":"user_id"}
                """);

        assertThat(config.password()).isEqualTo("db-password");
    }
}
