package org.chovy.canvas.infrastructure.bi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedisBiQueryResultCacheTest {

    @Test
    void storesAndReadsQueryResultsWithConfiguredTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        RedisBiQueryResultCache cache = new RedisBiQueryResultCache(
                redis,
                new ObjectMapper(),
                true,
                Duration.ofSeconds(90),
                "canvas:bi:query-cache:");
        BiQueryResult result = result("abc123");

        cache.put("abc123", result);

        org.mockito.ArgumentCaptor<String> payloadCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(values).set(eq("canvas:bi:query-cache:abc123"), payloadCaptor.capture(), eq(Duration.ofSeconds(90)));
        when(values.get("canvas:bi:query-cache:abc123")).thenReturn(payloadCaptor.getValue());

        assertThat(cache.get("abc123"))
                .hasValueSatisfying(restored -> {
                    assertThat(restored.datasetKey()).isEqualTo("canvas_daily_stats");
                    assertThat(restored.rowCount()).isEqualTo(1);
                    assertThat(restored.sqlHash()).isEqualTo("abc123");
                    assertThat(restored.rows()).containsExactly(Map.of("total_executions", 42));
                });
    }

    @Test
    void disabledCacheDoesNotTouchRedis() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RedisBiQueryResultCache cache = new RedisBiQueryResultCache(
                redis,
                new ObjectMapper(),
                false,
                Duration.ofSeconds(90),
                "canvas:bi:query-cache:");

        cache.put("abc123", result("abc123"));

        assertThat(cache.get("abc123")).isEmpty();
        verifyNoInteractions(redis);
    }

    @Test
    void corruptPayloadIsEvictedAndTreatedAsMiss() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("canvas:bi:query-cache:abc123")).thenReturn("{not-json");
        RedisBiQueryResultCache cache = new RedisBiQueryResultCache(
                redis,
                new ObjectMapper(),
                true,
                Duration.ofSeconds(90),
                "canvas:bi:query-cache:");

        assertThat(cache.get("abc123")).isEmpty();

        verify(redis).delete("canvas:bi:query-cache:abc123");
        verify(values, never()).set(eq("canvas:bi:query-cache:abc123"), eq("{not-json"), eq(Duration.ofSeconds(90)));
    }

    @Test
    void invalidatesHashDatasetAndAllKeys() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.delete("canvas:bi:query-cache:hash-one")).thenReturn(true);
        when(redis.delete("canvas:bi:query-cache:hash-two")).thenReturn(true);
        when(redis.keys("canvas:bi:query-cache:*")).thenReturn(Set.of(
                "canvas:bi:query-cache:hash-two",
                "canvas:bi:query-cache:hash-three"));
        when(redis.delete(Set.of("canvas:bi:query-cache:hash-two", "canvas:bi:query-cache:hash-three")))
                .thenReturn(2L);
        RedisBiQueryResultCache cache = new RedisBiQueryResultCache(
                redis,
                new ObjectMapper(),
                true,
                Duration.ofSeconds(90),
                "canvas:bi:query-cache:");
        BiQueryResult datasetResult = result("hash-two", "canvas_daily_stats");
        BiQueryResult otherResult = result("hash-three", "channel_daily_stats");
        when(values.get("canvas:bi:query-cache:hash-two"))
                .thenReturn(toJson(datasetResult));
        when(values.get("canvas:bi:query-cache:hash-three"))
                .thenReturn(toJson(otherResult));

        assertThat(cache.evict("hash-one")).isTrue();
        assertThat(cache.evictDataset("canvas_daily_stats")).isEqualTo(1);
        assertThat(cache.clear()).isEqualTo(2);

        verify(redis).delete("canvas:bi:query-cache:hash-one");
        verify(redis).delete("canvas:bi:query-cache:hash-two");
        verify(redis).delete(Set.of("canvas:bi:query-cache:hash-two", "canvas:bi:query-cache:hash-three"));
    }

    private BiQueryResult result(String sqlHash) {
        return result(sqlHash, "canvas_daily_stats");
    }

    private BiQueryResult result(String sqlHash, String datasetKey) {
        return new BiQueryResult(
                datasetKey,
                List.of(),
                List.of(Map.of("total_executions", 42)),
                1,
                10L,
                sqlHash);
    }

    private String toJson(BiQueryResult result) {
        try {
            return new ObjectMapper().writeValueAsString(result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
