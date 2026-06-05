package org.chovy.canvas.engine.policy;

import org.chovy.canvas.dal.mapper.CustomerChannelMapper;
import org.chovy.canvas.dal.mapper.CustomerProfileMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.MarketingSuppressionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingPolicyServiceFrequencyPreviewTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private MarketingPolicyService service;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        service = new MarketingPolicyService(
                mock(CustomerProfileMapper.class),
                mock(CustomerChannelMapper.class),
                mock(MarketingConsentMapper.class),
                mock(MarketingSuppressionMapper.class),
                redis);
    }

    @Test
    void previewFrequencyBlocksWhenCurrentBucketIsAtLimitWithoutMutatingRedis() {
        when(values.get(anyString())).thenReturn("3");

        MarketingPolicyService.PolicyDecision decision = service.previewFrequency(
                "user-1",
                20L,
                "node-1",
                "JOURNEY",
                "sms",
                3,
                Duration.ofDays(1));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reasonCode()).isEqualTo("FREQUENCY_CAP_EXCEEDED");
        verify(values).get(anyString());
        verify(values, never()).increment(anyString());
        verify(values, never()).decrement(anyString());
        verify(redis, never()).expire(anyString(), org.mockito.ArgumentMatchers.any(Duration.class));
    }

    @Test
    void previewFrequencyAllowsWhenCounterIsMissingWithoutMutatingRedis() {
        when(values.get(anyString())).thenReturn(null);

        MarketingPolicyService.PolicyDecision decision = service.previewFrequency(
                "user-1",
                20L,
                "node-1",
                "CHANNEL",
                "email",
                1,
                Duration.ofHours(6));

        assertThat(decision.allowed()).isTrue();
        verify(values).get(anyString());
        verify(values, never()).increment(anyString());
        verify(values, never()).decrement(anyString());
        verify(redis, never()).expire(anyString(), org.mockito.ArgumentMatchers.any(Duration.class));
    }
}
