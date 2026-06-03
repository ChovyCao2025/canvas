package org.chovy.canvas.dal.dataobject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceConfigDOTest {

    @Test
    void passwordIsWriteOnlyInApiJson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DataSourceConfigDO config = new DataSourceConfigDO();
        config.setName("warehouse");
        config.setPassword("secret");

        String json = objectMapper.writeValueAsString(config);
        DataSourceConfigDO parsed = objectMapper.readValue("{\"password\":\"from-request\"}", DataSourceConfigDO.class);

        assertThat(json).doesNotContain("secret");
        assertThat(json).doesNotContain("password");
        assertThat(parsed.getPassword()).isEqualTo("from-request");
    }

    @Test
    void passwordIsExcludedFromToStringAndEquality() {
        DataSourceConfigDO first = new DataSourceConfigDO();
        first.setName("warehouse");
        first.setPassword("secret-1");
        DataSourceConfigDO second = new DataSourceConfigDO();
        second.setName("warehouse");
        second.setPassword("secret-2");

        assertThat(first.toString()).doesNotContain("secret-1");
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}
