package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.domain.cdp.CdpTagService;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class CdpTagWriteHandlerTest {

    @Test
    void writesFixedTagValueAndRoutesToNextNode() {
        CdpTagService tagService = Mockito.mock(CdpTagService.class);
        CdpTagWriteHandler handler = new CdpTagWriteHandler(tagService);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setUserId("u1");

        NodeResult result = handler.executeAsync(Map.of(
                "tagCode", "vip",
                "valueMode", "fixed",
                "tagValue", "true",
                "reason", "hit branch",
                "nextNodeId", "next"
        ), ctx).block();

        ArgumentCaptor<CdpTagWriteReq> reqCaptor = ArgumentCaptor.forClass(CdpTagWriteReq.class);
        verify(tagService).setTag(Mockito.eq("u1"), reqCaptor.capture());
        assertThat(reqCaptor.getValue().tagCode()).isEqualTo("vip");
        assertThat(reqCaptor.getValue().tagValue()).isEqualTo("true");
        assertThat(reqCaptor.getValue().sourceType()).isEqualTo("CANVAS");
        assertThat(reqCaptor.getValue().idempotencyKey()).contains("exec-1");
        assertThat(result.nextNodeId()).isEqualTo("next");
        assertThat(result.output()).containsEntry("tagWriteStatus", "SUCCESS");
    }

    @Test
    void writesContextTagValue() {
        CdpTagService tagService = Mockito.mock(CdpTagService.class);
        CdpTagWriteHandler handler = new CdpTagWriteHandler(tagService);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setUserId("u1");
        ctx.getTriggerPayload().put("vipFlag", "true");

        handler.executeAsync(Map.of(
                "tagCode", "vip",
                "valueMode", "context",
                "tagValueField", "vipFlag"
        ), ctx).block();

        ArgumentCaptor<CdpTagWriteReq> reqCaptor = ArgumentCaptor.forClass(CdpTagWriteReq.class);
        verify(tagService).setTag(Mockito.eq("u1"), reqCaptor.capture());
        assertThat(reqCaptor.getValue().tagValue()).isEqualTo("true");
    }
}
