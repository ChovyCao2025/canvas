package org.chovy.canvas.risk.adapter.external;

import org.chovy.canvas.risk.domain.runtime.RiskFeatureStore;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisRiskFeatureStoreTest {

    @Test
    void setStoresFeatureValueWithTenantPrefixAndTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        RiskFeatureStore store = new RedisRiskFeatureStore(redis, new JacksonRiskRuleJsonCodec());

        store.set(7L, "user.fail_count_1d", "hash-user-1", 3, Duration.ofMinutes(30));

        verify(values).set(
                eq("risk:feature:7:user.fail_count_1d:hash-user-1"),
                eq("{\"type\":\"NUMBER\",\"value\":3}"),
                eq(Duration.ofMinutes(30)));
    }

    @Test
    void getParsesNumberBooleanAndStringValues() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("risk:feature:7:user.fail_count_1d:hash-user-1"))
                .thenReturn("{\"type\":\"NUMBER\",\"value\":3}");
        when(values.get("risk:feature:7:user.has_chargeback:hash-user-1"))
                .thenReturn("{\"type\":\"BOOLEAN\",\"value\":true}");
        when(values.get("risk:feature:7:user.segment:hash-user-1"))
                .thenReturn("{\"type\":\"STRING\",\"value\":\"vip\"}");
        RiskFeatureStore store = new RedisRiskFeatureStore(redis, new JacksonRiskRuleJsonCodec());

        assertThat(store.get(7L, "user.fail_count_1d", "hash-user-1")).contains(3);
        assertThat(store.get(7L, "user.has_chargeback", "hash-user-1")).contains(true);
        assertThat(store.get(7L, "user.segment", "hash-user-1")).contains("vip");
    }

    @Test
    void missingValueReturnsEmpty() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        RiskFeatureStore store = new RedisRiskFeatureStore(redis, new JacksonRiskRuleJsonCodec());

        Optional<Object> value = store.get(7L, "user.fail_count_1d", "hash-user-1");

        assertThat(value).isEmpty();
    }

    @Test
    void corruptValueIsDeletedAndTreatedAsMissing() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("risk:feature:7:user.fail_count_1d:hash-user-1")).thenReturn("{bad-json");
        RiskFeatureStore store = new RedisRiskFeatureStore(redis, new JacksonRiskRuleJsonCodec());

        assertThat(store.get(7L, "user.fail_count_1d", "hash-user-1")).isEmpty();

        verify(redis).delete("risk:feature:7:user.fail_count_1d:hash-user-1");
    }
}
