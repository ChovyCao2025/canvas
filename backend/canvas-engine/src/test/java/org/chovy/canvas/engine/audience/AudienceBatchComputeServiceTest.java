package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.audience.AudienceComputeRun;
import org.chovy.canvas.domain.audience.AudienceComputeRunMapper;
import org.chovy.canvas.domain.audience.AudienceDefinitionMapper;
import org.chovy.canvas.domain.audience.AudienceStatMapper;
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
            AudienceComputeRun run = invocation.getArgument(0);
            assertThat(run.getStatus()).isEqualTo("COMPUTING");
            assertThat(run.getPerfRunId()).isEqualTo("perf_20260523_001");
            assertThat(run.getPerfInputId()).isEqualTo("perf_20260523_001:audience:1");
            return 1;
        }).when(computeRunMapper).insert(any(AudienceComputeRun.class));

        AudienceBatchComputeService service = new AudienceBatchComputeService(
                mock(AudienceDefinitionMapper.class),
                mock(AudienceStatMapper.class),
                computeRunMapper,
                mock(RuleEvaluatorRouter.class),
                mock(AudienceBitmapStore.class),
                redis,
                new ObjectMapper(),
                mock(SqlWhereGenerator.class),
                mock(AudienceEvaluationContextFetcher.class)
        );

        AudienceComputeResult result = service.compute(
                1L,
                "perf_20260523_001",
                "perf_20260523_001:audience:1");

        assertThat(result.ready()).isFalse();
        var runCaptor = org.mockito.ArgumentCaptor.forClass(AudienceComputeRun.class);
        verify(computeRunMapper).updateById(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo("SKIPPED_LOCK");
    }
}
