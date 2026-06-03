package org.chovy.canvas.controller;

import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.chovy.canvas.security.SecretCipher;
import org.chovy.canvas.web.DataSourceConfigController;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DataSourceConfigControllerTest {

    @Test
    void createEncryptsPasswordBeforePersistence() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        SecretCipher cipher = SecretCipher.fromBase64Key("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        DataSourceConfigController controller = new DataSourceConfigController(mapper, cipher);
        DataSourceConfigDO body = new DataSourceConfigDO();
        body.setName("warehouse");
        body.setUrl("jdbc:mysql://localhost:3306/cdp");
        body.setUsername("cdp_app");
        body.setPassword("db-password");

        controller.create(body).block();

        ArgumentCaptor<DataSourceConfigDO> captor = ArgumentCaptor.forClass(DataSourceConfigDO.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getPassword()).startsWith("v1:");
        assertThat(captor.getValue().getPassword()).doesNotContain("db-password");
        assertThat(cipher.decrypt(captor.getValue().getPassword())).isEqualTo("db-password");
    }
}
