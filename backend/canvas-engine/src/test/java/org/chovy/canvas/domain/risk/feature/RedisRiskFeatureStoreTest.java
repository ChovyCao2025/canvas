package org.chovy.canvas.domain.risk.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void setStoresFeatureValueWithTenantPrefixAndTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        RedisRiskFeatureStore store = new RedisRiskFeatureStore(redis, objectMapper);

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
        RedisRiskFeatureStore store = new RedisRiskFeatureStore(redis, objectMapper);

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
        RedisRiskFeatureStore store = new RedisRiskFeatureStore(redis, objectMapper);

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
        RedisRiskFeatureStore store = new RedisRiskFeatureStore(redis, objectMapper);

        assertThat(store.get(7L, "user.fail_count_1d", "hash-user-1")).isEmpty();

        verify(redis).delete("risk:feature:7:user.fail_count_1d:hash-user-1");
    }
}
