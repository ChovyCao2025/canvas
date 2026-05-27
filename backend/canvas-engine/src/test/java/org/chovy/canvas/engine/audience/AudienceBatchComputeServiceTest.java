package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AudienceComputeRunDO;
import org.chovy.canvas.dal.mapper.AudienceComputeRunMapper;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.AudienceStatMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Audience Batch Compute 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
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
                mock(CdpAudienceSourceService.class)
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
    void computeViaCdpTagSavesBitmap() throws Exception {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceStatMapper statMapper = mock(AudienceStatMapper.class);
        AudienceComputeRunMapper computeRunMapper = mock(AudienceComputeRunMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        AudienceBitmapStore bitmapStore = mock(AudienceBitmapStore.class);
        CdpAudienceSourceService cdpAudienceSourceService = mock(CdpAudienceSourceService.class);

        org.chovy.canvas.dal.dataobject.AudienceDefinitionDO definition = new org.chovy.canvas.dal.dataobject.AudienceDefinitionDO();
        definition.setId(9L);
        definition.setName("CDP VIP");
        definition.setEnabled(1);
        definition.setDataSourceType("CDP_TAG");
        definition.setRuleJson("{\"logic\":\"AND\",\"conditions\":[{\"field\":\"high_value\",\"op\":\"=\",\"value\":\"VIP\"}]}");

        when(definitionMapper.selectById(9L)).thenReturn(definition);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("audience:compute:lock:9"), eq("1"), any(Duration.class))).thenReturn(true);
        when(cdpAudienceSourceService.resolveUserIds("CDP_TAG", definition.getRuleJson()))
                .thenReturn(java.util.List.of("u1", "u2"));

        AudienceBatchComputeService service = new AudienceBatchComputeService(
                definitionMapper,
                statMapper,
                computeRunMapper,
                mock(RuleEvaluatorRouter.class),
                bitmapStore,
                redis,
                new ObjectMapper(),
                mock(SqlWhereGenerator.class),
                mock(AudienceEvaluationContextFetcher.class),
                mock(JdbcConfigResolver.class),
                cdpAudienceSourceService
        );

        AudienceComputeResult result = service.compute(9L);

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo("READY");
        var bitmapCaptor = org.mockito.ArgumentCaptor.forClass(org.roaringbitmap.RoaringBitmap.class);
        verify(bitmapStore).save(eq(9L), bitmapCaptor.capture());
        assertThat(bitmapCaptor.getValue().getCardinality()).isEqualTo(2);
    }
}
