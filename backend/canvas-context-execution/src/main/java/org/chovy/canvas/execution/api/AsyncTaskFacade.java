package org.chovy.canvas.execution.api;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定义 AsyncTaskFacade 的执行上下文数据结构或业务契约。
 */
public interface AsyncTaskFacade {

    /**
     * 执行 listTasks 对应的业务处理。
     * @param query query 参数
     * @return 处理后的结果
     */
    List<AsyncTaskView> listTasks(AsyncTaskQuery query);

    /**
     * 执行 getTask 对应的业务处理。
     * @param taskId taskId 参数
     * @param username username 参数
     * @param admin admin 参数
     */
    AsyncTaskView getTask(String taskId, String username, boolean admin);

    /**
     * 定义 AsyncTaskQuery 的执行上下文数据结构或业务契约。
     * @param taskType taskType 对应的数据字段
     * @param bizType bizType 对应的数据字段
     * @param bizIds bizIds 对应的数据字段
     * @param statuses statuses 对应的数据字段
     * @param username username 对应的数据字段
     * @param admin admin 对应的数据字段
     * @param page page 对应的数据字段
     * @param size size 对应的数据字段
     */
    record AsyncTaskQuery(
            String taskType,
            String bizType,
            List<String> bizIds,
            List<String> statuses,
            String username,
            boolean admin,
            int page,
            int size) {
    }

    /**
     * 定义 AsyncTaskView 的执行上下文数据结构或业务契约。
     * @param taskId taskId 对应的数据字段
     * @param taskType taskType 对应的数据字段
     * @param bizType bizType 对应的数据字段
     * @param bizId bizId 对应的数据字段
     * @param title title 对应的数据字段
     * @param status status 对应的数据字段
     * @param progress progress 对应的数据字段
     * @param resultSummary resultSummary 对应的数据字段
     * @param errorMsg errorMsg 对应的数据字段
     * @param startedAt startedAt 对应的数据字段
     * @param finishedAt finishedAt 对应的数据字段
     * @param createdAt createdAt 对应的数据字段
     */
    record AsyncTaskView(
            String taskId,
            String taskType,
            String bizType,
            String bizId,
            String title,
            String status,
            Integer progress,
            String resultSummary,
            String errorMsg,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String createdBy) {
    }
}
