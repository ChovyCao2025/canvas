package org.chovy.canvas.execution.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 DlqFacade 的执行上下文数据结构或业务契约。
 */
public interface DlqFacade {

    /**
     * 执行 list 对应的业务处理。
     * @param query query 参数
     * @return 处理后的结果
     */
    DlqPageView list(DlqQuery query);

    /**
     * 执行 replay 对应的业务处理。
     * @param id id 参数
     * @param skipSuccessNodes skipSuccessNodes 参数
     * @return 处理后的结果
     */
    DlqReplayResult replay(Long id, boolean skipSuccessNodes);

    /**
     * 执行 delete 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    DeleteResult delete(Long id);

    /**
     * 执行 register 对应的业务处理。
     * @param command command 参数
     */
    default void register(DlqEntryCommand command) {
    }

    /**
     * 定义 DlqQuery 的执行上下文数据结构或业务契约。
     * @param canvasId canvasId 对应的数据字段
     * @param page page 对应的数据字段
     * @param size size 对应的数据字段
     */
    record DlqQuery(Long canvasId, int page, int size) {
    }

    /**
     * 定义 DlqEntryCommand 的执行上下文数据结构或业务契约。
     * @param id id 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param triggerType triggerType 对应的数据字段
     * @param triggerNodeType triggerNodeType 对应的数据字段
     * @param matchKey matchKey 对应的数据字段
     * @param payload payload 对应的数据字段
     * @param errorMessage errorMessage 对应的数据字段
     */
    record DlqEntryCommand(
            Long id,
            Long canvasId,
            String userId,
            String triggerType,
            String triggerNodeType,
            String matchKey,
            Map<String, Object> payload,
            String errorMessage) {
    }

    /**
     * 定义 DlqPageView 的执行上下文数据结构或业务契约。
     * @param total total 对应的数据字段
     * @param page page 对应的数据字段
     * @param size size 对应的数据字段
     * @param list list 对应的数据字段
     */
    record DlqPageView(long total, int page, int size, List<DlqEntryView> list) {
    }

    /**
     * 定义 DlqEntryView 的执行上下文数据结构或业务契约。
     * @param id id 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param triggerType triggerType 对应的数据字段
     * @param triggerNodeType triggerNodeType 对应的数据字段
     * @param matchKey matchKey 对应的数据字段
     * @param payload payload 对应的数据字段
     * @param errorMessage errorMessage 对应的数据字段
     * @param failedAt failedAt 对应的数据字段
     */
    record DlqEntryView(
            Long id,
            Long canvasId,
            String userId,
            String triggerType,
            String triggerNodeType,
            String matchKey,
            Map<String, Object> payload,
            String errorMessage,
            String failedAt) {
    }

    /**
     * 定义 DlqReplayResult 的执行上下文数据结构或业务契约。
     * @param dlqId dlqId 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param triggerType triggerType 对应的数据字段
     * @param triggerNodeType triggerNodeType 对应的数据字段
     * @param matchKey matchKey 对应的数据字段
     * @param payload payload 对应的数据字段
     * @param skipSuccessNodes skipSuccessNodes 对应的数据字段
     * @param replayId replayId 对应的数据字段
     */
    record DlqReplayResult(
            Long dlqId,
            Long canvasId,
            String userId,
            String triggerType,
            String triggerNodeType,
            String matchKey,
            Map<String, Object> payload,
            boolean skipSuccessNodes,
            String replayId) {
    }

    /**
     * 定义 DeleteResult 的执行上下文数据结构或业务契约。
     * @param id id 对应的数据字段
     * @param deleted deleted 对应的数据字段
     */
    record DeleteResult(Long id, boolean deleted) {
    }
}
