package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AudienceComputeRunDO;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.AudienceComputeRunMapper;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.AudienceStatMapper;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.chovy.canvas.engine.rule.AudienceDefinitionRuleValidator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class AudienceBatchComputeServiceTest {

    @Test
    void computeRecordsPerfRunWhenLockSkipsDuplicateRequest() {
        AudienceComputeRunMapper computeRunMapper = mock(AudienceComputeRunMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("audience:compute:lock:1"), eq("1"), any(Duration.class)))
                .thenReturn(false);
        doAnswer(invocation -> {
            AudienceComputeRunDO run = invocation.getArgument(0);
            assertThat(run.getStatus()).isEqualTo("COMPUTING");
            assertThat(run.getPerfRunId()).isEqualTo("perf_20260523_001");
            assertThat(run.getPerfInputId()).isEqualTo("perf_20260523_001:audience:1");
            return 1;
        }).when(computeRunMapper).insert(any(AudienceComputeRunDO.class));

        AudienceBatchComputeService service = new AudienceBatchComputeService(
                mock(AudienceDefinitionMapper.class),
                mock(AudienceStatMapper.class),
                computeRunMapper,
                mock(RuleEvaluatorRouter.class),
                mock(AudienceBitmapStore.class),
                redis,
                new ObjectMapper(),
                mock(SqlWhereGenerator.class),
                mock(AudienceEvaluationContextFetcher.class),
                mock(JdbcConfigResolver.class),
                mock(AudienceDefinitionRuleValidator.class)
        );

        AudienceComputeResult result = service.compute(
                1L,
                "perf_20260523_001",
                "perf_20260523_001:audience:1");

        assertThat(result.success()).isFalse();
        assertThat(result.status()).isEqualTo("FAILED");
        var runCaptor = org.mockito.ArgumentCaptor.forClass(AudienceComputeRunDO.class);
        verify(computeRunMapper).updateById(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo("SKIPPED_LOCK");
    }

    @Test
    void createValidatesAudienceRuleBeforeInsert() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceDefinitionRuleValidator validator = mock(AudienceDefinitionRuleValidator.class);
        AudienceBatchComputeService service = new AudienceBatchComputeService(
                definitionMapper,
                mock(AudienceStatMapper.class),
                mock(AudienceComputeRunMapper.class),
                mock(RuleEvaluatorRouter.class),
                mock(AudienceBitmapStore.class),
                mock(StringRedisTemplate.class),
                new ObjectMapper(),
                mock(SqlWhereGenerator.class),
                mock(AudienceEvaluationContextFetcher.class),
                mock(JdbcConfigResolver.class),
                validator
        );
        org.chovy.canvas.dal.dataobject.AudienceDefinitionDO definition =
                new org.chovy.canvas.dal.dataobject.AudienceDefinitionDO();

        service.create(definition);

        verify(validator).validateForSave(definition);
        verify(definitionMapper).insert(definition);
    }

    @Test
    void computeViaJdbcBindsMaxRowsAndStreamsUsersIntoBitmap() throws Exception {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceStatMapper statMapper = mock(AudienceStatMapper.class);
        AudienceBitmapStore bitmapStore = mock(AudienceBitmapStore.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("audience:compute:lock:42"), eq("1"), any(Duration.class)))
                .thenReturn(true);

        String ruleJson = """
                {"logic":"AND","conditions":[{"field":"age","op":">=","value":18}]}
                """;
        String dataSourceConfig = """
                {"dataSourceId":7,"baseTable":"users","userIdColumn":"external_user_id","maxRows":5}
                """;
        AudienceDefinitionDO definition = new AudienceDefinitionDO();
        definition.setId(42L);
        definition.setName("JDBC 人群");
        definition.setEnabled(1);
        definition.setDataSourceType("JDBC");
        definition.setRuleJson(ruleJson);
        definition.setDataSourceConfig(dataSourceConfig);
        when(definitionMapper.selectById(42L)).thenReturn(definition);

        SqlWhereGenerator sqlWhereGenerator = mock(SqlWhereGenerator.class);
        MapSqlParameterSource whereParams = new MapSqlParameterSource("age_0", 18);
        when(sqlWhereGenerator.generate(ruleJson))
                .thenReturn(new SqlWhereGenerator.SqlWhere("age >= :age_0", whereParams));

        JdbcConfigResolver jdbcConfigResolver = mock(JdbcConfigResolver.class);
        when(jdbcConfigResolver.resolve(dataSourceConfig))
                .thenReturn(new JdbcConfig(
                        7L,
                        "users",
                        "jdbc:test",
                        "user",
                        "password",
                        "external_user_id",
                        "org.example.Driver",
                        5));

        AudienceBatchComputeService service = new AudienceBatchComputeService(
                definitionMapper,
                statMapper,
                mock(AudienceComputeRunMapper.class),
                mock(RuleEvaluatorRouter.class),
                bitmapStore,
                redis,
                new ObjectMapper(),
                sqlWhereGenerator,
                mock(AudienceEvaluationContextFetcher.class),
                jdbcConfigResolver,
                mock(AudienceDefinitionRuleValidator.class)
        );

        DataSource dataSource = mock(DataSource.class, withSettings().extraInterfaces(AutoCloseable.class));
        @SuppressWarnings("rawtypes")
        DataSourceBuilder builder = mock(DataSourceBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(dataSource);
        AtomicReference<String> executedSql = new AtomicReference<>();
        AtomicReference<SqlParameterSource> executedParams = new AtomicReference<>();

        try (MockedStatic<DataSourceBuilder> dataSourceBuilder = mockStatic(DataSourceBuilder.class);
             MockedConstruction<NamedParameterJdbcTemplate> ignored = mockConstruction(
                     NamedParameterJdbcTemplate.class,
                     (template, context) -> {
                         JdbcTemplate delegate = mock(JdbcTemplate.class);
                         when(template.getJdbcTemplate()).thenReturn(delegate);
                         doAnswer(invocation -> {
                             executedSql.set(invocation.getArgument(0));
                             executedParams.set(invocation.getArgument(1));
                             RowCallbackHandler handler = invocation.getArgument(2);
                             handler.processRow(resultSetReturning("user-001"));
                             handler.processRow(resultSetReturning(" "));
                             handler.processRow(resultSetReturning("user-002"));
                             return null;
                         }).when(template).query(anyString(), any(SqlParameterSource.class), any(RowCallbackHandler.class));
                     })) {
            dataSourceBuilder.when(DataSourceBuilder::create).thenReturn(builder);

            AudienceComputeResult result = service.compute(42L);

            assertThat(result.status()).isEqualTo("READY");
            assertThat(result.estimatedSize()).isEqualTo(2);
        }

        assertThat(executedSql.get())
                .contains("LIMIT :__maxRows")
                .doesNotContain("LIMIT 5");
        assertThat(executedParams.get()).isInstanceOf(MapSqlParameterSource.class);
        MapSqlParameterSource params = (MapSqlParameterSource) executedParams.get();
        assertThat(params.getValue("age_0")).isEqualTo(18);
        assertThat(params.getValue("__maxRows")).isEqualTo(5);

        ArgumentCaptor<RoaringBitmap> bitmapCaptor = ArgumentCaptor.forClass(RoaringBitmap.class);
        verify(bitmapStore).save(eq(42L), bitmapCaptor.capture());
        RoaringBitmap bitmap = bitmapCaptor.getValue();
        assertThat(bitmap.getCardinality()).isEqualTo(2);
        assertThat(bitmap.contains(AudienceBitmapStore.toUid("user-001"))).isTrue();
        assertThat(bitmap.contains(AudienceBitmapStore.toUid("user-002"))).isTrue();
        verify((AutoCloseable) dataSource).close();
    }

    @Test
    void computeViaJdbcClosesDataSourceWhenQueryFails() throws Exception {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceStatMapper statMapper = mock(AudienceStatMapper.class);
        AudienceBitmapStore bitmapStore = mock(AudienceBitmapStore.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("audience:compute:lock:43"), eq("1"), any(Duration.class)))
                .thenReturn(true);

        String ruleJson = """
                {"logic":"AND","conditions":[{"field":"age","op":">=","value":18}]}
                """;
        String dataSourceConfig = """
                {"dataSourceId":7,"baseTable":"users","userIdColumn":"external_user_id","maxRows":5}
                """;
        AudienceDefinitionDO definition = new AudienceDefinitionDO();
        definition.setId(43L);
        definition.setName("JDBC 失败人群");
        definition.setEnabled(1);
        definition.setDataSourceType("JDBC");
        definition.setRuleJson(ruleJson);
        definition.setDataSourceConfig(dataSourceConfig);
        when(definitionMapper.selectById(43L)).thenReturn(definition);

        SqlWhereGenerator sqlWhereGenerator = mock(SqlWhereGenerator.class);
        when(sqlWhereGenerator.generate(ruleJson))
                .thenReturn(new SqlWhereGenerator.SqlWhere(
                        "age >= :age_0",
                        new MapSqlParameterSource("age_0", 18)));

        JdbcConfigResolver jdbcConfigResolver = mock(JdbcConfigResolver.class);
        when(jdbcConfigResolver.resolve(dataSourceConfig))
                .thenReturn(new JdbcConfig(
                        7L,
                        "users",
                        "jdbc:test",
                        "user",
                        "password",
                        "external_user_id",
                        "org.example.Driver",
                        5));

        AudienceBatchComputeService service = new AudienceBatchComputeService(
                definitionMapper,
                statMapper,
                mock(AudienceComputeRunMapper.class),
                mock(RuleEvaluatorRouter.class),
                bitmapStore,
                redis,
                new ObjectMapper(),
                sqlWhereGenerator,
                mock(AudienceEvaluationContextFetcher.class),
                jdbcConfigResolver,
                mock(AudienceDefinitionRuleValidator.class)
        );

        DataSource dataSource = mock(DataSource.class, withSettings().extraInterfaces(AutoCloseable.class));
        @SuppressWarnings("rawtypes")
        DataSourceBuilder builder = mock(DataSourceBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(dataSource);

        try (MockedStatic<DataSourceBuilder> dataSourceBuilder = mockStatic(DataSourceBuilder.class);
             MockedConstruction<NamedParameterJdbcTemplate> ignored = mockConstruction(
                     NamedParameterJdbcTemplate.class,
                     (template, context) -> {
                         JdbcTemplate delegate = mock(JdbcTemplate.class);
                         when(template.getJdbcTemplate()).thenReturn(delegate);
                         doAnswer(invocation -> {
                             throw new IllegalStateException("query failed");
                         }).when(template).query(anyString(), any(SqlParameterSource.class), any(RowCallbackHandler.class));
                     })) {
            dataSourceBuilder.when(DataSourceBuilder::create).thenReturn(builder);

            AudienceComputeResult result = service.compute(43L);

            assertThat(result.status()).isEqualTo("FAILED");
            assertThat(result.errorMsg()).isEqualTo("query failed");
        }

        verify((AutoCloseable) dataSource).close();
    }

    @Test
    void jdbcConfigRejectsNonPositiveMaxRows() {
        DataSourceConfigMapper dataSourceConfigMapper = mock(DataSourceConfigMapper.class);
        DataSourceConfigDO dataSource = new DataSourceConfigDO();
        dataSource.setType("JDBC");
        dataSource.setEnabled(1);
        dataSource.setUrl("jdbc:mysql://localhost:3306/app");
        dataSource.setUsername("user");
        dataSource.setPassword("password");
        when(dataSourceConfigMapper.selectById(7L)).thenReturn(dataSource);

        JdbcConfigResolver resolver = new JdbcConfigResolver(new ObjectMapper(), dataSourceConfigMapper);

        assertThatThrownBy(() -> resolver.resolve("""
                {"dataSourceId":7,"baseTable":"users","maxRows":0}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxRows must be positive");
        assertThatThrownBy(() -> resolver.resolve("""
                {"dataSourceId":7,"baseTable":"users","maxRows":-1}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxRows must be positive");
    }

    private static ResultSet resultSetReturning(String userId) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString(1)).thenReturn(userId);
        return resultSet;
    }
}
