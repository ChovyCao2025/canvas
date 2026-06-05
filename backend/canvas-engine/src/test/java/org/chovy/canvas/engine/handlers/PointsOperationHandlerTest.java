package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.CustomerPointsLedgerDO;
import org.chovy.canvas.domain.cdp.CustomerPointsLedgerService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PointsOperationHandlerTest {

    private CustomerPointsLedgerService ledgerService;
    private PointsOperationHandler handler;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        ledgerService = mock(CustomerPointsLedgerService.class);
        handler = new PointsOperationHandler(ledgerService);
        context = new ExecutionContext();
        context.setExecutionId("exec-1");
        context.setUserId("user-7");
    }

    @Test
    void executeAsyncReturnsExistingLedgerWhenIdempotencyKeyAlreadyExists() {
        CustomerPointsLedgerDO existing = new CustomerPointsLedgerDO();
        existing.setId(99L);
        when(ledgerService.findByIdempotencyKey("idem-1")).thenReturn(existing);

        NodeResult result = handler.executeAsync(Map.of(
                "idempotencyKey", "idem-1",
                MapFieldKeys.NEXT_NODE_ID, "next"
        ), context).block();

        assertThat(result.success()).isTrue();
        assertThat(result.nextNodeId()).isEqualTo("next");
        assertThat(result.output())
                .containsEntry(MapFieldKeys.POINTS_LEDGER_ID, 99L)
                .containsEntry(MapFieldKeys.DUPLICATE, true);
    }

    @Test
    void executeAsyncInsertsLedgerThroughDomainService() {
        doAnswer(invocation -> {
            CustomerPointsLedgerDO ledger = invocation.getArgument(0);
            ledger.setId(100L);
            return null;
        }).when(ledgerService).insert(any(CustomerPointsLedgerDO.class));

        NodeResult result = handler.executeAsync(Map.of(
                "points", 10,
                "pointsType", "MARKETING",
                "operation", "GRANT",
                MapFieldKeys.NEXT_NODE_ID, "next"
        ), context).block();

        assertThat(result.success()).isTrue();
        assertThat(result.nextNodeId()).isEqualTo("next");
        assertThat(result.output())
                .containsEntry(MapFieldKeys.POINTS_LEDGER_ID, 100L)
                .containsEntry(MapFieldKeys.DUPLICATE, false);
        verify(ledgerService).findByIdempotencyKey("exec-1:points");
        verify(ledgerService).insert(any(CustomerPointsLedgerDO.class));
    }

    @Test
    void executeAsyncTreatsDuplicateInsertAsIdempotentSuccess() {
        doThrow(new DuplicateKeyException("duplicate"))
                .when(ledgerService).insert(any(CustomerPointsLedgerDO.class));

        NodeResult result = handler.executeAsync(Map.of(
                "idempotencyKey", "idem-1",
                MapFieldKeys.NEXT_NODE_ID, "next"
        ), context).block();

        assertThat(result.success()).isTrue();
        assertThat(result.nextNodeId()).isEqualTo("next");
        assertThat(result.output())
                .containsEntry(MapFieldKeys.DUPLICATE, true)
                .containsEntry("idempotent", true);
        verify(ledgerService).findByIdempotencyKey(eq("idem-1"));
    }
}
