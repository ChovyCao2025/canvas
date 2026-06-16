package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

/**
 * 提供运行时运维、审计查询和应急动作能力的应用入口。
 */
public interface OpsFacade {

    /**
     * 失效指定画布的运行时缓存。
     *
     * @param tenantId 租户标识
     * @param canvasId 画布标识
     * @param actor 操作者
     * @return 缓存失效结果
     */
    Map<String, Object> invalidateCache(Long tenantId, Long canvasId, String actor);

    /**
     * 重建租户运行时状态。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @return 重建结果
     */
    Map<String, Object> rebuildRuntimeState(Long tenantId, String actor);

    /**
     * 查询租户运行时状态。
     *
     * @param tenantId 租户标识
     * @param role 操作者角色
     * @param actor 操作者
     * @return 运行时状态记录
     */
    Map<String, Object> runtimeStatus(Long tenantId, String role, String actor);

    /**
     * 查询运行时审计事件。
     *
     * @param tenantId 租户标识
     * @param limit 最大返回数量
     * @return 审计事件列表
     */
    List<Map<String, Object>> auditEvents(Long tenantId, Integer limit);

    /**
     * 执行指定画布的应急动作。
     *
     * @param tenantId 租户标识
     * @param canvasId 画布标识
     * @param action 应急动作名称
     * @param payload 应急动作参数
     * @param role 操作者角色
     * @param actor 操作者
     * @return 应急动作结果
     */
    Map<String, Object> emergencyAction(Long tenantId, Long canvasId, String action, Map<String, Object> payload,
                                        String role, String actor);
}
