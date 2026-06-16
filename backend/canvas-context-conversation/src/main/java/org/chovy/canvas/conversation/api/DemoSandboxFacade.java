package org.chovy.canvas.conversation.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理演示沙箱安装、重置和会话回复记录的门面。
 */
public interface DemoSandboxFacade {

    /**
     * 安装或覆盖租户的演示沙箱。
     *
     * @param command 安装请求
     * @param actor 操作者
     * @return 安装后的沙箱视图
     */
    SandboxView install(InstallCommand command, String actor);

    /**
     * 重置租户的演示沙箱状态。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @return 重置结果
     */
    ResetResult reset(Long tenantId, String actor);

    /**
     * 查询已经过期的演示沙箱。
     *
     * @return 过期沙箱视图列表
     */
    List<SandboxView> expired();

    /**
     * 记录一次演示沙箱会话回复。
     *
     * @param tenantId 租户标识
     * @param command 回复请求
     * @param actor 操作者
     * @return 回复记录结果
     */
    ConversationReplyResult reply(Long tenantId, ConversationReplyCommand command, String actor);

    /**
     * 演示沙箱安装请求。
     *
     * @param tenantId 租户标识
     * @param demoName 演示名称
     * @param ttlDays 有效天数
     */
    record InstallCommand(
            /**
             * 租户标识。
             */
            Long tenantId,
            /**
             * 演示名称。
             */
            String demoName,
            /**
             * 有效天数。
             */
            int ttlDays) {
    }

    /**
     * 演示沙箱会话回复请求。
     *
     * @param canvasId 关联画布标识
     * @param versionId 关联画布版本标识
     * @param executionId 外部执行标识
     * @param userId 会话用户标识
     * @param externalMessageId 外部消息标识
     * @param eventId 事件幂等标识
     * @param text 回复文本
     * @param intent 回复意图
     * @param attributes 回复扩展属性
     */
    record ConversationReplyCommand(
            /**
             * 关联画布标识。
             */
            Long canvasId,
            /**
             * 关联画布版本标识。
             */
            Long versionId,
            /**
             * 外部执行标识。
             */
            String executionId,
            /**
             * 会话用户标识。
             */
            String userId,
            /**
             * 外部消息标识。
             */
            String externalMessageId,
            /**
             * 事件幂等标识。
             */
            String eventId,
            /**
             * 回复文本。
             */
            String text,
            /**
             * 回复意图。
             */
            String intent,
            /**
             * 回复扩展属性。
             */
            Map<String, Object> attributes) {
    }

    /**
     * 演示沙箱查询视图。
     *
     * @param id 沙箱记录标识
     * @param tenantId 租户标识
     * @param demoName 演示名称
     * @param ttlDays 有效天数
     * @param status 沙箱状态
     * @param installedBy 安装操作者
     * @param installedAt 安装时间
     * @param expiresAt 过期时间
     */
    record SandboxView(
            /**
             * 沙箱记录标识。
             */
            Long id,
            /**
             * 租户标识。
             */
            Long tenantId,
            /**
             * 演示名称。
             */
            String demoName,
            /**
             * 有效天数。
             */
            int ttlDays,
            /**
             * 沙箱状态。
             */
            String status,
            /**
             * 安装操作者。
             */
            String installedBy,
            /**
             * 安装时间。
             */
            LocalDateTime installedAt,
            /**
             * 过期时间。
             */
            LocalDateTime expiresAt) {
    }

    /**
     * 演示沙箱重置结果。
     *
     * @param tenantId 租户标识
     * @param status 重置后的状态
     * @param resetBy 重置操作者
     * @param resetAt 重置时间
     */
    record ResetResult(
            /**
             * 租户标识。
             */
            Long tenantId,
            /**
             * 重置后的状态。
             */
            String status,
            /**
             * 重置操作者。
             */
            String resetBy,
            /**
             * 重置时间。
             */
            LocalDateTime resetAt) {
    }

    /**
     * 演示沙箱会话回复记录结果。
     *
     * @param id 回复记录标识
     * @param tenantId 租户标识
     * @param channel 固定的沙箱渠道
     * @param canvasId 关联画布标识
     * @param versionId 关联画布版本标识
     * @param executionId 外部执行标识
     * @param userId 会话用户标识
     * @param externalMessageId 外部消息标识
     * @param eventId 事件幂等标识
     * @param text 回复文本
     * @param intent 回复意图
     * @param attributes 回复扩展属性
     * @param createdBy 创建操作者
     * @param createdAt 创建时间
     */
    record ConversationReplyResult(
            /**
             * 回复记录标识。
             */
            Long id,
            /**
             * 租户标识。
             */
            Long tenantId,
            /**
             * 固定的沙箱渠道。
             */
            String channel,
            /**
             * 关联画布标识。
             */
            Long canvasId,
            /**
             * 关联画布版本标识。
             */
            Long versionId,
            /**
             * 外部执行标识。
             */
            String executionId,
            /**
             * 会话用户标识。
             */
            String userId,
            /**
             * 外部消息标识。
             */
            String externalMessageId,
            /**
             * 事件幂等标识。
             */
            String eventId,
            /**
             * 回复文本。
             */
            String text,
            /**
             * 回复意图。
             */
            String intent,
            /**
             * 回复扩展属性。
             */
            Map<String, Object> attributes,
            /**
             * 创建操作者。
             */
            String createdBy,
            /**
             * 创建时间。
             */
            LocalDateTime createdAt) {
    }
}
