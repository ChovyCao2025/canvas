package org.chovy.canvas.execution.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 MqTriggerRejectedFacade 的执行上下文数据结构或业务契约。
 */
public interface MqTriggerRejectedFacade {

    /**
     * 执行 list 对应的业务处理。
     * @param query query 参数
     * @return 处理后的结果
     */
    RejectedPageView list(RejectedQuery query);

    /**
     * 执行 detail 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    RejectedView detail(Long id);

    /**
     * 执行 replay 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    ReplayResult replay(Long id);

    /**
     * 执行 register 对应的业务处理。
     * @param command command 参数
     */
    default void register(RejectedCommand command) {
    }

    /**
     * 定义 RejectedQuery 的执行上下文数据结构或业务契约。
     * @param tag tag 对应的数据字段
     * @param reason reason 对应的数据字段
     * @param page page 对应的数据字段
     * @param size size 对应的数据字段
     */
    record RejectedQuery(String tag, String reason, int page, int size) {
    }

    /**
     * 定义 RejectedCommand 的执行上下文数据结构或业务契约。
     * @param id id 对应的数据字段
     * @param tag tag 对应的数据字段
     * @param msgId msgId 对应的数据字段
     * @param reason reason 对应的数据字段
     * @param body body 对应的数据字段
     * @param routes routes 对应的数据字段
     */
    record RejectedCommand(
            Long id,
            String tag,
            String msgId,
            String reason,
            Map<String, Object> body,
            List<String> routes) {
    }

    /**
     * 定义 RejectedPageView 的执行上下文数据结构或业务契约。
     * @param total total 对应的数据字段
     * @param page page 对应的数据字段
     * @param size size 对应的数据字段
     * @param list list 对应的数据字段
     */
    record RejectedPageView(long total, int page, int size, List<RejectedView> list) {
    }

    /**
     * 定义 RejectedView 的执行上下文数据结构或业务契约。
     * @param id id 对应的数据字段
     * @param tag tag 对应的数据字段
     * @param msgId msgId 对应的数据字段
     * @param reason reason 对应的数据字段
     * @param body body 对应的数据字段
     * @param createdAt createdAt 对应的数据字段
     */
    record RejectedView(
            Long id,
            String tag,
            String msgId,
            String reason,
            Map<String, Object> body,
            String createdAt) {
    }

    /**
     * 定义 ReplayResult 的执行上下文数据结构或业务契约。
     * @param count count 对应的数据字段
     * @param requestIds requestIds 对应的数据字段
     * @param dispatchFailureCount dispatchFailureCount 对应的数据字段
     * @param dispatchFailedRequestIds dispatchFailedRequestIds 对应的数据字段
     */
    record ReplayResult(
            int count,
            List<String> requestIds,
            int dispatchFailureCount,
            List<String> dispatchFailedRequestIds) {
    }
}
