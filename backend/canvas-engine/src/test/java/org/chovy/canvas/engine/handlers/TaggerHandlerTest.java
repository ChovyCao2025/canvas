package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.audience.AudienceBitmapStore;
import org.chovy.canvas.engine.audience.AudienceSnapshotService;
import org.chovy.canvas.engine.audience.AudienceUserResolver;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class TaggerHandlerTest {

    @Test
    void nonAudienceModeContinuesWithTagOutput() {
        TaggerHandler handler = handler(Mockito.mock(AudienceBitmapStore.class));
        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("u1");

        NodeResult result = handler.executeAsync(Map.of(
                "mode", "offline",
                "tagCodeKey", "high_value",
                MapFieldKeys.NEXT_NODE_ID, "next"
        ), ctx).block();

        assertThat(result.nextNodeId()).isEqualTo("next");
        assertThat(result.output())
                .containsEntry(MapFieldKeys.MODE, "offline")
                .containsEntry("tagCodeKey", "high_value");
    }

    @Test
    void routesToHitBranchWhenUserIsInAudience() {
        AudienceBitmapStore bitmapStore = Mockito.mock(AudienceBitmapStore.class);
        TaggerHandler handler = handler(bitmapStore);
        when(bitmapStore.isMember(101L, "u1")).thenReturn(true);

        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("u1");

        NodeResult result = handler.executeAsync(Map.of(
                "mode", "audience",
                "audienceId", 101,
                "hitNextNodeId", "send_coupon",
                "missNextNodeId", "end"
        ), ctx).block();

        assertThat(result.nextNodeId()).isEqualTo("send_coupon");
        assertThat(result.output()).containsEntry("audienceHit", true);
    }

    @Test
    void routesToMissBranchWhenUserIsNotInAudience() {
        AudienceBitmapStore bitmapStore = Mockito.mock(AudienceBitmapStore.class);
        TaggerHandler handler = handler(bitmapStore);
        when(bitmapStore.isMember(101L, "u1")).thenReturn(false);

        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("u1");

        NodeResult result = handler.executeAsync(Map.of(
                "mode", "audience",
                "audienceId", 101,
                "hitNextNodeId", "send_coupon",
                "missNextNodeId", "end"
        ), ctx).block();

        assertThat(result.nextNodeId()).isEqualTo("end");
        assertThat(result.output()).containsEntry("audienceHit", false);
    }

    @Test
    void scheduledBatchAudienceTaggerFansOutToResolvedUsers() {
        AudienceBitmapStore bitmapStore = Mockito.mock(AudienceBitmapStore.class);
        AudienceUserResolver audienceUserResolver = Mockito.mock(AudienceUserResolver.class);
        CanvasExecutionService executionService = Mockito.mock(CanvasExecutionService.class);
        TaggerHandler handler = new TaggerHandler(
                bitmapStore,
                audienceUserResolver,
                executionService,
                Mockito.mock(AudienceSnapshotService.class));

        when(audienceUserResolver.resolve(101L)).thenReturn(List.of("u1", "u2"));
        when(executionService.trigger(anyLong(), anyString(), anyString(), anyString(), anyString(), anyMap(), anyString(), anyBoolean()))
                .thenReturn(Mono.just(Map.of()));

        ExecutionContext ctx = new ExecutionContext();
        ctx.setCanvasId(62L);
        ctx.setExecutionId("batch-exec");
        ctx.setUserId("__scheduled_batch__:62:schedule");
        ctx.setTriggerPayload(Map.of(MapFieldKeys.SCHEDULED_BATCH, true));

        NodeResult result = handler.executeAsync(Map.of(
                MapFieldKeys.NODE_ID_INTERNAL, "audience",
                "mode", "audience",
                "audienceId", 101,
                "hitNextNodeId", "send_coupon",
                "missNextNodeId", "end"
        ), ctx).block();

        assertThat(result.nextNodeId()).isNull();
        assertThat(result.output()).containsEntry("fanoutCount", 2);
        Mockito.verify(executionService).trigger(eq(62L), eq("u1"), eq(TriggerType.SCHEDULED),
                eq(NodeType.TAGGER), eq("audience"), anyMap(), anyString(), eq(false));
        Mockito.verify(executionService).trigger(eq(62L), eq("u2"), eq(TriggerType.SCHEDULED),
                eq(NodeType.TAGGER), eq("audience"), anyMap(), anyString(), eq(false));
        Mockito.verifyNoInteractions(bitmapStore);
    }

    @Test
    void scheduledBatchStaticAudienceFansOutFromSnapshot() {
        AudienceSnapshotService snapshotService = Mockito.mock(AudienceSnapshotService.class);
        AudienceUserResolver audienceUserResolver = Mockito.mock(AudienceUserResolver.class);
        CanvasExecutionService executionService = Mockito.mock(CanvasExecutionService.class);
        TaggerHandler handler = new TaggerHandler(
                Mockito.mock(AudienceBitmapStore.class),
                audienceUserResolver,
                executionService,
                snapshotService);

        when(snapshotService.users(501L)).thenReturn(List.of("locked-u1", "locked-u2"));
        when(executionService.trigger(anyLong(), anyString(), anyString(), anyString(), anyString(), anyMap(), anyString(), anyBoolean()))
                .thenReturn(Mono.just(Map.of()));

        NodeResult result = handler.executeAsync(Map.of(
                MapFieldKeys.NODE_ID_INTERNAL, "audience",
                "mode", "audience",
                "audienceId", 101,
                "audienceSnapshotId", 501,
                "hitNextNodeId", "send_coupon"
        ), scheduledBatchContext()).block();

        assertThat(result.output())
                .containsEntry("fanoutCount", 2)
                .containsEntry("audienceSnapshotId", 501L);
        Mockito.verify(snapshotService).users(501L);
        Mockito.verifyNoInteractions(audienceUserResolver);
        Mockito.verify(executionService, Mockito.times(2)).trigger(
                eq(62L),
                anyString(),
                eq(TriggerType.SCHEDULED),
                eq(NodeType.TAGGER),
                eq("audience"),
                argThat(payload -> payload != null && Long.valueOf(501L).equals(payload.get("audienceSnapshotId"))),
                anyString(),
                eq(false));
    }

    @Test
    void scheduledBatchDynamicAudienceUsesCurrentResolver() {
        AudienceSnapshotService snapshotService = Mockito.mock(AudienceSnapshotService.class);
        AudienceUserResolver audienceUserResolver = Mockito.mock(AudienceUserResolver.class);
        CanvasExecutionService executionService = Mockito.mock(CanvasExecutionService.class);
        TaggerHandler handler = new TaggerHandler(
                Mockito.mock(AudienceBitmapStore.class),
                audienceUserResolver,
                executionService,
                snapshotService);

        when(audienceUserResolver.resolve(101L)).thenReturn(List.of("fresh-u1"));
        when(executionService.trigger(anyLong(), anyString(), anyString(), anyString(), anyString(), anyMap(), anyString(), anyBoolean()))
                .thenReturn(Mono.just(Map.of()));

        NodeResult result = handler.executeAsync(Map.of(
                MapFieldKeys.NODE_ID_INTERNAL, "audience",
                "mode", "audience",
                "audienceId", 101
        ), scheduledBatchContext()).block();

        assertThat(result.output()).containsEntry("fanoutCount", 1);
        Mockito.verify(audienceUserResolver).resolve(101L);
        Mockito.verifyNoInteractions(snapshotService);
    }

    @Test
    void realtimeStaticAudienceChecksSnapshotMembership() {
        AudienceBitmapStore bitmapStore = Mockito.mock(AudienceBitmapStore.class);
        AudienceSnapshotService snapshotService = Mockito.mock(AudienceSnapshotService.class);
        TaggerHandler handler = new TaggerHandler(
                bitmapStore,
                Mockito.mock(AudienceUserResolver.class),
                Mockito.mock(CanvasExecutionService.class),
                snapshotService);
        when(snapshotService.contains(501L, "u1")).thenReturn(true);

        ExecutionContext ctx = new ExecutionContext();
        ctx.setUserId("u1");

        NodeResult result = handler.executeAsync(Map.of(
                "mode", "audience",
                "audienceId", 101,
                "audienceSnapshotId", 501,
                "hitNextNodeId", "hit",
                "missNextNodeId", "miss"
        ), ctx).block();

        assertThat(result.nextNodeId()).isEqualTo("hit");
        assertThat(result.output())
                .containsEntry("audienceHit", true)
                .containsEntry("audienceSnapshotId", 501L);
        Mockito.verifyNoInteractions(bitmapStore);
    }

    private static TaggerHandler handler(AudienceBitmapStore bitmapStore) {
        return new TaggerHandler(
                bitmapStore,
                Mockito.mock(AudienceUserResolver.class),
                Mockito.mock(CanvasExecutionService.class),
                Mockito.mock(AudienceSnapshotService.class)
        );
    }

    private ExecutionContext scheduledBatchContext() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setCanvasId(62L);
        ctx.setExecutionId("batch-exec");
        ctx.setUserId("__scheduled_batch__:62:schedule");
        ctx.setTriggerPayload(Map.of(MapFieldKeys.SCHEDULED_BATCH, true));
        return ctx;
    }
}
