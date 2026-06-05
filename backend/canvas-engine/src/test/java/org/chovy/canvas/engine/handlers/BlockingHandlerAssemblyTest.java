package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.CustomerPointsLedgerDO;
import org.chovy.canvas.domain.canvas.SubFlowLookupService;
import org.chovy.canvas.domain.cdp.CustomerPointsLedgerService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.scheduler.DagEngine;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class BlockingHandlerAssemblyTest {

    @Test
    void pointsOperationDoesNotTouchLedgerServiceBeforeSubscription() {
        CustomerPointsLedgerService ledgerService = mock(CustomerPointsLedgerService.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, CustomerPointsLedgerDO.class).setId(1L);
            return 1;
        }).when(ledgerService).insert(any(CustomerPointsLedgerDO.class));
        PointsOperationHandler handler = new PointsOperationHandler(ledgerService);

        Mono<NodeResult> result = handler.executeAsync(Map.of("points", 10), ctx());

        assertThat(result).isNotNull();
        verifyNoInteractions(ledgerService);
    }

    @Test
    void pointsOperationDuplicateInsertIsIdempotent() {
        CustomerPointsLedgerService ledgerService = mock(CustomerPointsLedgerService.class);
        doThrow(new DuplicateKeyException("duplicate"))
                .when(ledgerService).insert(any(CustomerPointsLedgerDO.class));
        PointsOperationHandler handler = new PointsOperationHandler(ledgerService);

        NodeResult result = handler.executeAsync(Map.of("points", 10, MapFieldKeys.NEXT_NODE_ID, "next"), ctx()).block();

        assertThat(result.success()).isTrue();
        assertThat(result.nextNodeId()).isEqualTo("next");
        assertThat(result.output()).containsEntry("idempotent", true);
    }

    @Test
    void subFlowRefDoesNotLoadCanvasBeforeSubscription() {
        SubFlowLookupService lookupService = mock(SubFlowLookupService.class);
        SubFlowRefHandler handler = new SubFlowRefHandler(
                lookupService,
                mock(CanvasConfigCache.class),
                mock(DagEngine.class),
                new ObjectMapper());

        Mono<NodeResult> result = handler.executeAsync(Map.of("subFlowId", 20L), ctx());

        assertThat(result).isNotNull();
        verifyNoInteractions(lookupService);
    }

    private static ExecutionContext ctx() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(10L);
        ctx.setVersionId(100L);
        ctx.setUserId("user-1");
        return ctx;
    }
}
