package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

/**
 * 提供审批任务、审批实例和飞书审批同步能力的应用入口。
 */
public interface ApprovalFacade {

    /**
     * 查询当前操作者可处理的审批任务。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @param role 操作者角色
     * @param status 任务状态过滤值
     * @return 审批任务列表
     */
    List<Map<String, Object>> tasks(Long tenantId, String actor, String role, String status);

    /**
     * 查询审批实例。
     *
     * @param tenantId 租户标识
     * @param targetType 审批目标类型
     * @param targetId 审批目标标识
     * @param status 实例状态过滤值
     * @return 审批实例列表
     */
    List<Map<String, Object>> instances(Long tenantId, String targetType, String targetId, String status);

    /**
     * 通过指定审批任务。
     *
     * @param tenantId 租户标识
     * @param taskId 审批任务标识
     * @param payload 审批操作参数
     * @param actor 操作者
     * @param role 操作者角色
     * @return 审批后的任务或实例记录
     */
    Map<String, Object> approve(Long tenantId, Long taskId, Map<String, Object> payload, String actor, String role);

    /**
     * 驳回指定审批任务。
     *
     * @param tenantId 租户标识
     * @param taskId 审批任务标识
     * @param payload 驳回操作参数
     * @param actor 操作者
     * @param role 操作者角色
     * @return 驳回后的任务或实例记录
     */
    Map<String, Object> reject(Long tenantId, Long taskId, Map<String, Object> payload, String actor, String role);

    /**
     * 从飞书同步一批审批实例。
     *
     * @param tenantId 租户标识
     * @param limit 最大同步数量
     * @param actor 操作者
     * @param role 操作者角色
     * @return 同步结果摘要
     */
    Map<String, Object> syncLarkApprovals(Long tenantId, Integer limit, String actor, String role);

    /**
     * 从飞书同步单个审批实例。
     *
     * @param tenantId 租户标识
     * @param instanceId 本地审批实例标识
     * @param actor 操作者
     * @param role 操作者角色
     * @return 同步结果记录
     */
    Map<String, Object> syncLarkApprovalInstance(Long tenantId, Long instanceId, String actor, String role);
}
