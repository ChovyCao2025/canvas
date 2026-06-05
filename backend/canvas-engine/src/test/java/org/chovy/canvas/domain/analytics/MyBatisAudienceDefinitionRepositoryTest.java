package org.chovy.canvas.domain.analytics;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyBatisAudienceDefinitionRepositoryTest {

    @Test
    void requireEnabledLoadsTenantScopedEnabledAudienceDefinition() {
        AudienceDefinitionDO definition = new AudienceDefinitionDO();
        definition.setId(10L);
        definition.setTenantId(7L);
        definition.setEnabled(1);
        AudienceDefinitionMapper mapper = mock(AudienceDefinitionMapper.class);
        when(mapper.selectOne(any())).thenReturn(definition);
        MyBatisAudienceDefinitionRepository repository = new MyBatisAudienceDefinitionRepository(mapper);

        AudienceDefinitionDO result = repository.requireEnabled(7L, 10L);

        assertThat(result).isSameAs(definition);
    }
}
