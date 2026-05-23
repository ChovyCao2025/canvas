package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.audience.AudienceDataSource;
import org.chovy.canvas.domain.audience.AudienceDefinition;
import org.chovy.canvas.domain.audience.AudienceDataSourceMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class AudienceBatchComputeServiceJdbcConfigTest {

    private final AudienceBatchComputeService service = new AudienceBatchComputeService(
            null,
            null,
            null,
            null,
            null,
            new ObjectMapper(),
            null,
            null,
            dataSourceMapper(null)
    );

    @Test
    void parseJdbcConfigMergesDataSourceRecordAndDefinitionConfig() throws Exception {
        AudienceDataSource dataSource = new AudienceDataSource();
        dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/canvas_demo");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        AudienceDefinition definition = new AudienceDefinition();
        definition.setDataSourceType("JDBC");
        definition.setDataSourceId(11L);
        definition.setDataSourceConfig("{\"baseTable\":\"audience_demo_user\",\"userIdColumn\":\"user_id\",\"maxRows\":100}");

        AudienceBatchComputeService.JdbcConfig config = service.parseJdbcConfig(definition, dataSource);

        assertThat(config.baseTable()).isEqualTo("audience_demo_user");
        assertThat(config.url()).contains("canvas_demo");
        assertThat(config.username()).isEqualTo("root");
        assertThat(config.password()).isEqualTo("root");
        assertThat(config.userIdColumn()).isEqualTo("user_id");
        assertThat(config.driverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(config.maxRows()).isEqualTo(100);
    }

    @Test
    void parseJdbcConfigFallsBackToDefaultDriverWhenDataSourceDriverMissing() throws Exception {
        AudienceDataSource dataSource = new AudienceDataSource();
        dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/canvas_demo");
        dataSource.setUsername("root");
        dataSource.setPassword("root");

        AudienceDefinition definition = new AudienceDefinition();
        definition.setDataSourceType("JDBC");
        definition.setDataSourceId(11L);
        definition.setDataSourceConfig("{\"baseTable\":\"audience_demo_user\"}");

        AudienceBatchComputeService.JdbcConfig config = service.parseJdbcConfig(definition, dataSource);

        assertThat(config.driverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
    }

    @Test
    void validateWritableDataSourceRejectsDisabledSourceForNewBindings() {
        AudienceDataSource disabledDataSource = new AudienceDataSource();
        disabledDataSource.setId(11L);
        disabledDataSource.setEnabled(0);
        AudienceBatchComputeService validatingService = new AudienceBatchComputeService(
                null,
                null,
                null,
                null,
                null,
                new ObjectMapper(),
                null,
                null,
                dataSourceMapper(disabledDataSource)
        );

        AudienceDefinition definition = new AudienceDefinition();
        definition.setDataSourceType("JDBC");
        definition.setDataSourceId(11L);

        assertThatThrownBy(() -> validatingService.validateWritableDataSource(definition))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disabled");
    }

    private static AudienceDataSourceMapper dataSourceMapper(AudienceDataSource result) {
        return (AudienceDataSourceMapper) Proxy.newProxyInstance(
                AudienceDataSourceMapper.class.getClassLoader(),
                new Class[]{AudienceDataSourceMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectById" -> result;
                    case "toString" -> "AudienceDataSourceMapperStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
