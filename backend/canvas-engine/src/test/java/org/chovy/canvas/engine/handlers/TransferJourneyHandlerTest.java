package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferJourneyHandlerTest {

    @Test
    void carryContextKeepsLatestNodeOutputAheadOfStaleTriggerPayloadForBareKeys() {
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        when(executionService.trigger(anyLong(), anyString(), anyString(), anyString(), nullable(String.class),
                anyMap(), anyString(), anyBoolean()))
                .thenReturn(Mono.just(Map.of()));
        TransferJourneyHandler handler = new TransferJourneyHandler(executionService);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-transfer");
        ctx.setUserId("user-1");
        ctx.setTriggerPayload(Map.of("status", "stale-trigger"));
        ctx.putNodeOutput("node-A", Map.of("status", "latest-output"));

        NodeResult result = handler.executeAsync(Map.of(
                "targetJourneyId", 42,
                "carryContext", true
        ), ctx).block();

        assertThat(result.success()).isTrue();
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(executionService).trigger(eq(42L), eq("user-1"), eq(TriggerType.TRANSFER_JOURNEY),
                eq(NodeType.DIRECT_CALL), eq(null), payload.capture(), eq("exec-transfer:transfer:42"),
                eq(false));
        assertThat(payload.getValue()).containsEntry("status", "latest-output");
        assertThat(payload.getValue()).containsEntry("node-A.status", "latest-output");
        assertThat(payload.getValue()).containsEntry(MapFieldKeys.SOURCE_EXECUTION_ID, "exec-transfer");
    }
}
