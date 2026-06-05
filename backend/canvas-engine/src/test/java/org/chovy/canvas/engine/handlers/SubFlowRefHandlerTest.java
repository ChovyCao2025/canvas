package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.domain.canvas.SubFlowLookupService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubFlowRefHandlerTest {

    private SubFlowLookupService lookupService;
    private SubFlowRefHandler handler;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        lookupService = mock(SubFlowLookupService.class);
        handler = new SubFlowRefHandler(
                lookupService,
                mock(CanvasConfigCache.class),
                mock(DagEngine.class),
                new ObjectMapper());
        context = new ExecutionContext();
        context.setExecutionId("exec-1");
        context.setCanvasId(10L);
        context.setUserId("user-7");
    }

    @Test
    void executeAsyncFailsWhenSubFlowIdIsMissingWithoutLookup() {
        NodeResult result = handler.executeAsync(Map.of(), context).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("SUB_FLOW_REF 缺少 subFlowId");
        verify(lookupService, never()).findCanvas(20L);
    }

    @Test
    void executeAsyncFailsWhenSubFlowCanvasIsNotPublished() {
        when(lookupService.findCanvas(20L)).thenReturn(null);

        NodeResult result = handler.executeAsync(Map.of("subFlowId", 20L), context).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("子流程画布未发布: 20");
    }

    @Test
    void executeAsyncRunsStrategyTableFromLookupServiceVersion() {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(20L);
        canvas.setStatus(1);
        canvas.setPublishedVersionId(30L);
        CanvasVersionDO version = new CanvasVersionDO();
        version.setId(30L);
        version.setGraphJson("""
                {
                  "type": "STRATEGY_TABLE",
                  "strategies": [
                    {
                      "id": "vip",
                      "order": 1,
                      "conditions": {"tier": "VIP"},
                      "result": {"coupon": "VIP-10"}
                    }
                  ]
                }
                """);
        context.setTriggerPayload(Map.of("userTier", "VIP"));
        when(lookupService.findCanvas(20L)).thenReturn(canvas);
        when(lookupService.findVersion(30L)).thenReturn(version);

        NodeResult result = handler.executeAsync(Map.of(
                "subFlowId", 20L,
                "nextNodeId", "next",
                "inputMapping", Map.of("tier", "ctx.userTier")
        ), context).block();

        assertThat(result.success()).isTrue();
        assertThat(result.nextNodeId()).isEqualTo("next");
        assertThat(result.output()).containsEntry("coupon", "VIP-10");
    }
}
