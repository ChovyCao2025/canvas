package org.chovy.canvas.engine.audience;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.chovy.canvas.domain.audience.AudienceDataSource;
import org.chovy.canvas.domain.audience.AudienceDefinition;
import org.chovy.canvas.domain.audience.AudienceDataSourceMapper;
import org.chovy.canvas.domain.audience.AudienceDefinitionMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudienceDataSourceServiceTest {

    private static final String JDBC_DATA_SOURCE_TYPE = "JDBC";

    @Test
    void deleteRejectsWhenDefinitionReferencesDataSource() {
        AudienceDataSourceMapper dataSourceMapper = dataSourceMapper(List.of(), 0, 0, null);
        DefinitionMapperStub definitionMapper = definitionMapper(1L, List.of());
        AudienceDataSourceService service = new AudienceDataSourceService(dataSourceMapper, definitionMapper.mapper());

        assertThatThrownBy(() -> service.delete(7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("referenced");

        assertThat(definitionMapper.countWrappers).hasSize(1);
        assertJdbcDataSourceQuery(definitionMapper.countWrappers.getFirst(), List.of(7L));
    }

    @Test
    void listWithReferenceCountReturnsUsageCountUsingSingleJdbcScopedQuery() {
        AudienceDataSource sourceOne = new AudienceDataSource();
        sourceOne.setId(9L);
        sourceOne.setName("Demo DS");
        AudienceDataSource sourceTwo = new AudienceDataSource();
        sourceTwo.setId(12L);
        sourceTwo.setName("Other DS");
        AudienceDataSourceMapper dataSourceMapper = dataSourceMapper(List.of(sourceOne, sourceTwo), 0, 0, null);
        DefinitionMapperStub definitionMapper = definitionMapper(
                0L,
                List.of(definitionWithDataSourceId(9L), definitionWithDataSourceId(9L), definitionWithDataSourceId(12L))
        );
        AudienceDataSourceService service = new AudienceDataSourceService(dataSourceMapper, definitionMapper.mapper());

        List<AudienceDataSource> result = service.list();

        assertThat(result).extracting(AudienceDataSource::getReferenceCount).containsExactly(2L, 1L);
        assertThat(definitionMapper.listWrappers).hasSize(1);
        assertJdbcDataSourceQuery(definitionMapper.listWrappers.getFirst(), List.of(9L, 12L));
    }

    @Test
    void updateReturnsNullWhenDataSourceDoesNotExist() {
        AudienceDataSource source = new AudienceDataSource();
        source.setId(99L);
        AudienceDataSourceMapper dataSourceMapper = dataSourceMapper(List.of(), 0, 0, null);
        DefinitionMapperStub definitionMapper = definitionMapper(0L, List.of());
        AudienceDataSourceService service = new AudienceDataSourceService(dataSourceMapper, definitionMapper.mapper());

        assertThat(service.update(source)).isNull();
    }

    private AudienceDataSourceMapper dataSourceMapper(List<AudienceDataSource> dataSources, int deleteResult, int updateResult, AudienceDataSource selectedDataSource) {
        return (AudienceDataSourceMapper) Proxy.newProxyInstance(
                AudienceDataSourceMapper.class.getClassLoader(),
                new Class[]{AudienceDataSourceMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectList" -> dataSources;
                    case "deleteById" -> deleteResult;
                    case "updateById" -> updateResult;
                    case "selectById" -> selectedDataSource;
                    case "toString" -> "AudienceDataSourceMapperStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private DefinitionMapperStub definitionMapper(Long referenceCount, List<AudienceDefinition> definitions) {
        List<QueryWrapper<AudienceDefinition>> countWrappers = new ArrayList<>();
        List<QueryWrapper<AudienceDefinition>> listWrappers = new ArrayList<>();

        AudienceDefinitionMapper mapper = (AudienceDefinitionMapper) Proxy.newProxyInstance(
                AudienceDefinitionMapper.class.getClassLoader(),
                new Class[]{AudienceDefinitionMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectCount" -> {
                        countWrappers.add(castWrapper(args[0]));
                        yield referenceCount;
                    }
                    case "selectList" -> {
                        listWrappers.add(castWrapper(args[0]));
                        yield definitions;
                    }
                    case "toString" -> "AudienceDefinitionMapperStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        return new DefinitionMapperStub(mapper, countWrappers, listWrappers);
    }

    private AudienceDefinition definitionWithDataSourceId(Long id) {
        AudienceDefinition definition = new AudienceDefinition();
        definition.setDataSourceId(id);
        return definition;
    }

    private QueryWrapper<AudienceDefinition> castWrapper(Object value) {
        return (QueryWrapper<AudienceDefinition>) value;
    }

    private void assertJdbcDataSourceQuery(QueryWrapper<AudienceDefinition> wrapper, List<Long> expectedIds) {
        String sqlSegment = wrapper.getSqlSegment();
        Map<String, Object> params = wrapper.getParamNameValuePairs();

        assertThat(sqlSegment).contains("data_source_id");
        assertThat(sqlSegment).contains("data_source_type");
        assertThat(params.values()).contains(JDBC_DATA_SOURCE_TYPE);
        assertThat(params.values().stream().filter(Long.class::isInstance).toList())
                .containsExactlyInAnyOrderElementsOf(expectedIds);
    }

    private record DefinitionMapperStub(
            AudienceDefinitionMapper mapper,
            List<QueryWrapper<AudienceDefinition>> countWrappers,
            List<QueryWrapper<AudienceDefinition>> listWrappers
    ) {
    }
}
