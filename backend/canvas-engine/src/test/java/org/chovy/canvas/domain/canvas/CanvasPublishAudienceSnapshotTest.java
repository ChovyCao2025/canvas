package org.chovy.canvas.domain.canvas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.engine.audience.AudienceSnapshotService;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.GroovyHandler;
import org.chovy.canvas.engine.rule.CanvasRuleGraphValidator;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.engine.trigger.CanvasSchedulerService;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasPublishAudienceSnapshotTest {

    @Test
    void publishUsesBoundGraphWithoutWritingDraftGraph() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasTransactionService transactionService = mock(CanvasTransactionService.class);
        AudienceSnapshotService snapshotService = mock(AudienceSnapshotService.class);

        CanvasDO canvas = new CanvasDO();
        canvas.setId(62L);
        canvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
        when(canvasMapper.selectById(62L)).thenReturn(canvas);

        CanvasVersionDO draft = new CanvasVersionDO();
        draft.setId(91L);
        draft.setCanvasId(62L);
        draft.setVersion(3);
        draft.setStatus(VersionStatus.DRAFT.getCode());
        draft.setGraphJson("""
                {"nodes":[{"id":"start","type":"DIRECT_CALL","config":{"nextNodeId":"tag"}},
                {"id":"tag","type":"TAGGER","config":{"mode":"audience","audienceId":101}}]}
                """);
        when(versionMapper.selectOne(any())).thenReturn(draft);

        String boundGraph = """
                {"nodes":[{"id":"start","type":"DIRECT_CALL","config":{"nextNodeId":"tag"}},
                {"id":"tag","type":"TAGGER","config":{"mode":"audience","audienceId":101,"audienceSnapshotMode":"STATIC_LOCKED","audienceSnapshotId":501}}]}
                """;
        when(snapshotService.bindAudienceSnapshotsForPublish(eq(62L), eq(91L), eq(draft.getGraphJson()), eq("alice")))
                .thenReturn(boundGraph);

        ArgumentCaptor<String> publishedGraph = ArgumentCaptor.forClass(String.class);
        CanvasVersionDO published = new CanvasVersionDO();
        published.setId(92L);
        published.setCanvasId(62L);
        published.setGraphJson(boundGraph);
        when(transactionService.publishDb(eq(62L), publishedGraph.capture(), eq("alice")))
                .thenReturn(new CanvasTransactionService.PublishResult(published, null));

        CanvasService canvasService = canvasService(canvasMapper, versionMapper, transactionService);
        canvasService.setAudienceSnapshotService(snapshotService);

        CanvasVersionDO result = canvasService.publish(62L, "alice");

        assertThat(result).isSameAs(published);
        assertThat(publishedGraph.getValue()).contains("\"audienceSnapshotId\":501");
        verify(versionMapper, never()).updateById(any(CanvasVersionDO.class));
    }

    private CanvasService canvasService(CanvasMapper canvasMapper,
                                        CanvasVersionMapper versionMapper,
                                        CanvasTransactionService transactionService) {
        return new CanvasService(
                canvasMapper,
                versionMapper,
                new DagParser(new ObjectMapper()),
                mock(TriggerRouteService.class),
                mock(CanvasSchedulerService.class),
                mock(CanvasConfigCache.class),
                mock(CanvasExecutionService.class),
                mock(TriggerPreCheckService.class),
                mock(GroovyHandler.class),
                mock(org.chovy.canvas.engine.handlers.MqTriggerHandler.class),
                mock(CanvasRuleGraphValidator.class),
                redisWithAcquiredLock(),
                transactionService,
                new CanvasExamplesProperties());
    }

    private StringRedisTemplate redisWithAcquiredLock() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        return redis;
    }
}
