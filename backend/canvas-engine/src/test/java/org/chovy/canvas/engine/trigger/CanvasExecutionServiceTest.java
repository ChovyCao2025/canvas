package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.canvas.CanvasVersionMapper;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.execution.CanvasExecutionMapper;
import org.chovy.canvas.domain.execution.CanvasExecutionStatsMapper;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.infra.cache.CanvasConfigCache;
import org.chovy.canvas.infra.redis.ContextPersistenceService;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CanvasExecutionService}.
 */
@ExtendWith(MockitoExtension.class)
class CanvasExecutionServiceTest {

    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;
    @Mock CanvasExecutionMapper executionMapper;
    @Mock CanvasConfigCache configCache;
    @Mock DagParser dagParser;
    @Mock ContextPersistenceService ctxStore;
    @Mock DagEngine dagEngine;
    @Mock TriggerPreCheckService preCheckService;
    @Mock InFlightExecutionRegistry executionRegistry;
    @Mock CanvasExecutionStatsMapper statsMapper;

    /** System under test — constructed manually so we can inject mocks via ReflectionTestUtils. */
    CanvasExecutionService sut;

    @BeforeEach
    void setUp() {
        sut = new CanvasExecutionService(
                canvasMapper,
                canvasVersionMapper,
                executionMapper,
                configCache,
                dagParser,
                ctxStore,
                dagEngine,
                preCheckService,
                executionRegistry,
                statsMapper
        );
        // Inject @Value fields with sensible defaults
        ReflectionTestUtils.setField(sut, "ctxTtlSec", 86400L);
        ReflectionTestUtils.setField(sut, "globalTimeoutSec", 600L);
        ReflectionTestUtils.setField(sut, "globalMaxConcurrency", 1000);
    }

    // ─── canvasCache caching behaviour ───────────────────────────────────────

    /**
     * Calling trigger() twice with the same canvasId must hit canvasMapper.selectById()
     * only once — the second call should be served from the Caffeine cache.
     *
     * <p>To keep the test focused solely on the cache, we call the cache directly
     * via ReflectionTestUtils rather than driving the full trigger() pipeline
     * (which would require stubbing 8+ collaborators end-to-end).
     */
    @Test
    @SuppressWarnings("unchecked")
    void canvasCache_deduplicatesDbCallsForSameId() {
        final long canvasId = 10L;

        Canvas published = new Canvas();
        published.setId(canvasId);
        published.setStatus(CanvasStatusEnum.PUBLISHED.getCode());

        when(canvasMapper.selectById(canvasId)).thenReturn(published);

        // Obtain the internal Caffeine cache through reflection
        Cache<Long, Canvas> cache =
                (Cache<Long, Canvas>) ReflectionTestUtils.getField(sut, "canvasCache");

        // First access → loads from DB
        Canvas first = cache.get(canvasId, id -> canvasMapper.selectById(id));
        // Second access → should come from cache, NOT from DB
        Canvas second = cache.get(canvasId, id -> canvasMapper.selectById(id));

        // DB called exactly once despite two cache.get() calls
        verify(canvasMapper, times(1)).selectById(eq(canvasId));
        assert first == second : "cache should return the same object instance";
    }

    /**
     * invalidateCanvas() must remove the entry so the next cache.get() re-queries the DB.
     */
    @Test
    @SuppressWarnings("unchecked")
    void invalidateCanvas_causesNextAccessToReloadFromDb() {
        final long canvasId = 20L;

        Canvas v1 = new Canvas();
        v1.setId(canvasId);
        v1.setStatus(CanvasStatusEnum.PUBLISHED.getCode());

        Canvas v2 = new Canvas();
        v2.setId(canvasId);
        v2.setStatus(0); // simulates post-offline status

        when(canvasMapper.selectById(canvasId))
                .thenReturn(v1)   // first load
                .thenReturn(v2);  // reload after invalidation

        Cache<Long, Canvas> cache =
                (Cache<Long, Canvas>) ReflectionTestUtils.getField(sut, "canvasCache");

        // Warm the cache
        cache.get(canvasId, id -> canvasMapper.selectById(id));

        // Simulate publish/offline event
        sut.invalidateCanvas(canvasId);

        // Next access must go back to DB
        Canvas afterInvalidate = cache.get(canvasId, id -> canvasMapper.selectById(id));

        verify(canvasMapper, times(2)).selectById(eq(canvasId));
        assert afterInvalidate == v2 : "should return the freshly loaded canvas after invalidation";
    }
}
