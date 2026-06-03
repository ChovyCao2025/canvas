package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.domain.cdp.CdpTagService;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * CDP 标签写入节点处理器（节点类型 {@code CDP_TAG_WRITE}）。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType("CDP_TAG_WRITE")
public class CdpTagWriteHandler implements NodeHandler {

    /** CDP 标签服务，用于执行用户标签写入。 */
    private final CdpTagService tagService;

    /**
     * 构造 CdpTagWriteHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param tagService tagService 方法执行所需的业务参数
     */
    public CdpTagWriteHandler(CdpTagService tagService) {
        this.tagService = tagService;
    }

    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
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

        return Mono.fromCallable(() -> {
                    // setTag 是外部 CDP 写入副作用，幂等键确保同一执行重试不会重复写标签。
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

                    return NodeResult.ok(nextNodeId, Map.of(
                            "tagCode", tagCode,
                            "tagValue", tagValue,
                            "tagWriteStatus", "SUCCESS"
                    ));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Mono.just(NodeResult.fail("CDP_TAG_WRITE: " + e.getMessage())));
    }

    /**
     * 构建、解析或转换 resolve Tag Value 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @param valueMode valueMode 待写入、比较或转换的业务值
     * @return 转换或查询得到的字符串结果
     */
    private String resolveTagValue(Map<String, Object> config, ExecutionContext ctx, String valueMode) {
        if ("context".equalsIgnoreCase(valueMode)) {
            // context 模式从运行上下文取标签值，支持上游节点动态产出标签。
            String field = stringValue(config.get("tagValueField"));
            Object value = ctx.getContextValue(field);
            return value == null ? null : String.valueOf(value);
        }
        return stringValue(config.get("tagValue"));
    }

    /**
     * 执行 string Value 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 转换或查询得到的字符串结果
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
