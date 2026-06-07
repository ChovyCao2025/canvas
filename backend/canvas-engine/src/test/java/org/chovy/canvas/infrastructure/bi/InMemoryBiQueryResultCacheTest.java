package org.chovy.canvas.infrastructure.bi;

import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryBiQueryResultCacheTest {

    @Test
    void expiresEntriesAfterTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-05T03:00:00Z"));
        InMemoryBiQueryResultCache cache = new InMemoryBiQueryResultCache(true, Duration.ofSeconds(60), 10, clock);
        BiQueryResult result = result("hash-1");

        cache.put("hash-1", result);
        assertThat(cache.get("hash-1")).contains(result);

        clock.advance(Duration.ofSeconds(61));
        assertThat(cache.get("hash-1")).isEmpty();
        assertThat(cache.size()).isZero();
    }

    @Test
    void disabledCacheDoesNotStoreResults() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-05T03:00:00Z"));
        InMemoryBiQueryResultCache cache = new InMemoryBiQueryResultCache(false, Duration.ofSeconds(60), 10, clock);

        cache.put("hash-1", result("hash-1"));

        assertThat(cache.get("hash-1")).isEmpty();
        assertThat(cache.size()).isZero();
    }

    @Test
    void exposesCapacityHitAndEvictionStats() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-05T03:00:00Z"));
        InMemoryBiQueryResultCache cache = new InMemoryBiQueryResultCache(true, Duration.ofSeconds(60), 2, clock);

        cache.put("hash-1", result("hash-1"));
        clock.advance(Duration.ofSeconds(1));
        cache.put("hash-2", result("hash-2"));
        assertThat(cache.get("hash-1")).isPresent();
        assertThat(cache.get("missing")).isEmpty();
        clock.advance(Duration.ofSeconds(1));
        cache.put("hash-3", result("hash-3"));

        var stats = cache.stats();
        assertThat(stats.provider()).isEqualTo("memory");
        assertThat(stats.enabled()).isTrue();
        assertThat(stats.entryCount()).isEqualTo(2);
        assertThat(stats.maxEntries()).isEqualTo(2);
        assertThat(stats.ttlSeconds()).isEqualTo(60);
        assertThat(stats.hitCount()).isEqualTo(1);
        assertThat(stats.missCount()).isEqualTo(1);
        assertThat(stats.putCount()).isEqualTo(3);
        assertThat(stats.evictionCount()).isEqualTo(1);
    }

    private BiQueryResult result(String sqlHash) {
        return new BiQueryResult(
                "canvas_daily_stats",
                List.of(),
                List.of(Map.of("total_executions", 42L)),
                1,
                10L,
                sqlHash);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
