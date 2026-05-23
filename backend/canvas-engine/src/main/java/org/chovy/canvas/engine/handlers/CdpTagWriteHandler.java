package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.domain.cdp.CdpTagService;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@NodeHandlerType("CDP_TAG_WRITE")
public class CdpTagWriteHandler implements NodeHandler {

    private final CdpTagService tagService;

    public CdpTagWriteHandler(CdpTagService tagService) {
        this.tagService = tagService;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String userId = ctx.getUserId();
        if (userId == null || userId.isBlank()) {
            return Mono.error(new IllegalArgumentException("CDP_TAG_WRITE 缺少 userId"));
        }

        String tagCode = stringValue(config.get("tagCode"));
        String valueMode = stringValue(config.get("valueMode"));
        String tagValue = resolveTagValue(config, ctx, valueMode);
        String reason = stringValue(config.get("reason"));
        String nextNodeId = stringValue(config.get("nextNodeId"));
        String sourceRefId = ctx.getExecutionId();
        String idempotencyKey = sourceRefId + ":CDP_TAG_WRITE:" + userId + ":" + tagCode;

        tagService.setTag(userId, new CdpTagWriteReq(
                tagCode,
                tagValue,
                reason,
                null,
                "CANVAS",
                sourceRefId,
                null,
                idempotencyKey
        ));

        return Mono.just(NodeResult.ok(nextNodeId, Map.of(
                "tagCode", tagCode,
                "tagValue", tagValue,
                "tagWriteStatus", "SUCCESS"
        )));
    }

    private String resolveTagValue(Map<String, Object> config, ExecutionContext ctx, String valueMode) {
        if ("context".equalsIgnoreCase(valueMode)) {
            String field = stringValue(config.get("tagValueField"));
            Object value = ctx.getContextValue(field);
            return value == null ? null : String.valueOf(value);
        }
        return stringValue(config.get("tagValue"));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
