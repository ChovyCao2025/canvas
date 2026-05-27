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

/**
 * Cdp Tag Write 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
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
