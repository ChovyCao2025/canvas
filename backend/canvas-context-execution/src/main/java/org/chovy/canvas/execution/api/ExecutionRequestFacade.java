package org.chovy.canvas.execution.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 ExecutionRequestFacade 的执行上下文数据结构或业务契约。
 */
public interface ExecutionRequestFacade {

    /**
     * 执行 list 对应的业务处理。
     * @param query query 参数
     * @return 处理后的结果
     */
    RequestPageView list(RequestQuery query);

    /**
     * 执行 replay 对应的业务处理。
     * @param id id 参数
     * @param command command 参数
     * @return 处理后的结果
     */
    ReplayResult replay(String id, ReplayCommand command);

    /**
     * 执行 replayBatch 对应的业务处理。
     * @param command command 参数
     */
    BatchReplayResult replayBatch(BatchReplayCommand command);

    /**
     * 执行 register 对应的业务处理。
     * @param command command 参数
     */
    default void register(RequestCommand command) {
    }

    /**
     * 定义 RequestQuery 的执行上下文数据结构或业务契约。
     * @param tenantId tenantId 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param status status 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param sourceMsgId sourceMsgId 对应的数据字段
     * @param page page 对应的数据字段
     * @param size size 对应的数据字段
     */
    record RequestQuery(
            Long tenantId,
            Long canvasId,
            String status,
            String userId,
            String sourceMsgId,
            int page,
            int size) {
    }

    /**
     * 定义 ReplayCommand 的执行上下文数据结构或业务契约。
     * @param tenantId tenantId 对应的数据字段
     * @param operator operator 对应的数据字段
     * @param reason reason 对应的数据字段
     * @param force force 对应的数据字段
     */
    record ReplayCommand(Long tenantId, String operator, String reason, boolean force) {
    }

    /**
     * 定义 BatchReplayCommand 的执行上下文数据结构或业务契约。
     * @param tenantId tenantId 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param status status 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param sourceMsgId sourceMsgId 对应的数据字段
     * @param limit limit 对应的数据字段
     * @param reason reason 对应的数据字段
     * @param force force 对应的数据字段
     */
    record BatchReplayCommand(
            Long tenantId,
            Long canvasId,
            String status,
            String userId,
            String sourceMsgId,
            int limit,
            String reason,
            boolean force) {
    }

    /**
     * 定义 RequestCommand 的执行上下文数据结构或业务契约。
     * @param id id 对应的数据字段
     * @param tenantId tenantId 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param status status 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param sourceMsgId sourceMsgId 对应的数据字段
     * @param payload payload 对应的数据字段
     */
    record RequestCommand(
            String id,
            Long tenantId,
            Long canvasId,
            String status,
            String userId,
            String sourceMsgId,
            Map<String, Object> payload) {
    }

    /**
     * 定义 RequestPageView 的执行上下文数据结构或业务契约。
     * @param total total 对应的数据字段
     * @param page page 对应的数据字段
     * @param size size 对应的数据字段
     * @param list list 对应的数据字段
     */
    record RequestPageView(long total, int page, int size, List<RequestView> list) {
    }

    /**
     * 定义 RequestView 的执行上下文数据结构或业务契约。
     * @param id id 对应的数据字段
     * @param tenantId tenantId 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param status status 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param sourceMsgId sourceMsgId 对应的数据字段
     * @param payload payload 对应的数据字段
     * @param createdAt createdAt 对应的数据字段
     * @param updatedAt updatedAt 对应的数据字段
     */
    record RequestView(
            String id,
            Long tenantId,
            Long canvasId,
            String status,
            String userId,
            String sourceMsgId,
            Map<String, Object> payload,
            String createdAt,
            String updatedAt) {
    }

    /**
     * 定义 ReplayResult 的执行上下文数据结构或业务契约。
     * @param requestId requestId 对应的数据字段
     * @param status status 对应的数据字段
     * @param immediateDispatch immediateDispatch 对应的数据字段
     */
    record ReplayResult(String requestId, String status, boolean immediateDispatch) {
    }

    /**
     * 定义 BatchReplayResult 的执行上下文数据结构或业务契约。
     * @param count count 对应的数据字段
     * @param limit limit 对应的数据字段
     * @param requestIds requestIds 对应的数据字段
     * @param dispatchFailureCount dispatchFailureCount 对应的数据字段
     * @param dispatchFailedRequestIds dispatchFailedRequestIds 对应的数据字段
     */
    record BatchReplayResult(
            int count,
            int limit,
            List<String> requestIds,
            int dispatchFailureCount,
            List<String> dispatchFailedRequestIds) {
    }
}
