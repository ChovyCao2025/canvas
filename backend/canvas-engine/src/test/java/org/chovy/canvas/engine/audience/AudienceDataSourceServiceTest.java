package org.chovy.canvas.engine.audience;

import org.chovy.canvas.domain.audience.AudienceDataSource;
import org.chovy.canvas.domain.audience.AudienceDataSourceMapper;
import org.chovy.canvas.domain.audience.AudienceDefinitionMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudienceDataSourceServiceTest {

    @Test
    void deleteRejectsWhenDefinitionReferencesDataSource() {
        AudienceDataSourceMapper dataSourceMapper = dataSourceMapper(List.of(), 0);
        AudienceDefinitionMapper definitionMapper = definitionMapper(1L);
        AudienceDataSourceService service = new AudienceDataSourceService(dataSourceMapper, definitionMapper);

        assertThatThrownBy(() -> service.delete(7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("referenced");
    }

    @Test
    void listWithReferenceCountReturnsUsageCount() {
        AudienceDataSource source = new AudienceDataSource();
        source.setId(9L);
        source.setName("Demo DS");
        AudienceDataSourceMapper dataSourceMapper = dataSourceMapper(List.of(source), 0);
        AudienceDefinitionMapper definitionMapper = definitionMapper(3L);
        AudienceDataSourceService service = new AudienceDataSourceService(dataSourceMapper, definitionMapper);

        assertThat(service.list().getFirst().getReferenceCount()).isEqualTo(3L);
    }

    private AudienceDataSourceMapper dataSourceMapper(List<AudienceDataSource> dataSources, int deleteResult) {
        return (AudienceDataSourceMapper) Proxy.newProxyInstance(
                AudienceDataSourceMapper.class.getClassLoader(),
                new Class[]{AudienceDataSourceMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectList" -> dataSources;
                    case "deleteById" -> deleteResult;
                    case "toString" -> "AudienceDataSourceMapperStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private AudienceDefinitionMapper definitionMapper(Long referenceCount) {
        return (AudienceDefinitionMapper) Proxy.newProxyInstance(
                AudienceDefinitionMapper.class.getClassLoader(),
                new Class[]{AudienceDefinitionMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectCount" -> referenceCount;
                    case "toString" -> "AudienceDefinitionMapperStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
