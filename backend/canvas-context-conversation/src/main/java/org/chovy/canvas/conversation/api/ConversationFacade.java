package org.chovy.canvas.conversation.api;

import java.util.List;
import java.util.Map;

/**
 * 会话上下文模块面向控制器和其他业务模块的统一门面。
 */
public interface ConversationFacade {

    /**
     * 记录一条外部渠道入站消息，并恢复匹配的等待执行。
     *
     * @param command 入站消息记录命令
     * @return 消息记录结果
     */
    ConversationRecordResult recordInbound(ConversationInboundCommand command);

    /**
     * 确保指定会话存在对应工单。
     *
     * @param tenantId 租户标识
     * @param sessionId 会话标识
     * @param actor 操作者
     * @return 已存在或新建的工单视图
     */
    ConversationWorkItemView ensureWorkItemForSession(Long tenantId, Long sessionId, String actor);

    /**
     * 将工单分配给指定处理人或团队。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param command 分配请求
     * @param actor 操作者
     * @return 分配后的工单视图
     */
    ConversationWorkItemView assignWorkItem(Long tenantId, Long workItemId, ConversationAssignmentCommand command, String actor);

    /**
     * 更新工单状态、优先级和下一次跟进时间。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param command 状态更新请求
     * @param actor 操作者
     * @return 更新后的工单视图
     */
    ConversationWorkItemView updateWorkItemStatus(Long tenantId, Long workItemId, ConversationWorkItemStatusCommand command, String actor);

    /**
     * 新建或更新租户内的路由坐席配置。
     *
     * @param tenantId 租户标识
     * @param command 坐席配置请求
     * @param actor 操作者
     * @return 保存后的坐席视图
     */
    ConversationRoutingAgentView upsertRoutingAgent(Long tenantId, ConversationRoutingAgentCommand command, String actor);

    /**
     * 新建或更新租户内的路由规则配置。
     *
     * @param tenantId 租户标识
     * @param command 路由规则请求
     * @param actor 操作者
     * @return 保存后的路由规则视图
     */
    ConversationRoutingRuleView upsertRoutingRule(Long tenantId, ConversationRoutingRuleCommand command, String actor);

    /**
     * 对指定工单执行路由决策。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param command 路由请求
     * @param actor 操作者
     * @return 路由执行结果
     */
    ConversationRouteResultView routeWorkItem(Long tenantId, Long workItemId, ConversationRouteCommand command, String actor);

    /**
     * 记录适配器提交的原始入站载荷。
     *
     * @param tenantId 租户标识
     * @param adapterKey 适配器键
     * @param payload 原始载荷
     * @param actor 操作者
     * @return 记录后的事件摘要列表
     */
    List<Map<String, Object>> recordAdapterInbound(Long tenantId, String adapterKey, Map<String, Object> payload, String actor);

    /**
     * 查询会话列表。
     *
     * @param tenantId 租户标识
     * @param userId 用户标识过滤条件
     * @param channel 渠道过滤条件
     * @param limit 返回数量上限
     * @return 会话摘要列表
     */
    List<Map<String, Object>> listSessions(Long tenantId, String userId, String channel, int limit);

    /**
     * 查询指定会话下的消息列表。
     *
     * @param tenantId 租户标识
     * @param sessionId 会话标识
     * @param limit 返回数量上限
     * @return 消息摘要列表
     */
    List<Map<String, Object>> listMessages(Long tenantId, Long sessionId, int limit);

    /**
     * 查询客服工单收件箱。
     *
     * @param tenantId 租户标识
     * @param status 状态过滤条件
     * @param assignedTo 处理人过滤条件
     * @param channel 渠道过滤条件
     * @param limit 返回数量上限
     * @return 工单视图列表
     */
    List<ConversationWorkItemView> inbox(Long tenantId, String status, String assignedTo, String channel, int limit);

    /**
     * 为工单创建外部待办任务。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param command 待办创建参数
     * @param actor 操作者
     * @return 创建后的待办摘要
     */
    Map<String, Object> createTask(Long tenantId, Long workItemId, Map<String, Object> command, String actor);

    /**
     * 完成外部待办任务。
     *
     * @param tenantId 租户标识
     * @param taskId 待办任务标识
     * @param command 完成参数
     * @param actor 操作者
     * @return 完成后的待办摘要
     */
    Map<String, Object> completeTask(Long tenantId, Long taskId, Map<String, Object> command, String actor);

    /**
     * 汇总工单时间线。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param messageLimit 消息数量上限
     * @param auditLimit 审计数量上限
     * @return 工单时间线数据
     */
    Map<String, Object> timeline(Long tenantId, Long workItemId, int messageLimit, int auditLimit);

    /**
     * 执行 SLA 违约检测。
     *
     * @param tenantId 租户标识
     * @param limit 检测数量上限
     * @param actor 操作者
     * @return SLA 检测结果摘要
     */
    Map<String, Object> evaluateSlaBreaches(Long tenantId, int limit, String actor);

    /**
     * 查询 SLA 违约记录。
     *
     * @param tenantId 租户标识
     * @param status 状态过滤条件
     * @param limit 返回数量上限
     * @return SLA 违约摘要列表
     */
    List<Map<String, Object>> slaBreaches(Long tenantId, String status, int limit);

    /**
     * 为工单生成 AI 回复建议。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param command 生成参数
     * @param actor 操作者
     * @return 回复建议摘要
     */
    Map<String, Object> generateAiReplySuggestion(Long tenantId, Long workItemId, Map<String, Object> command, String actor);

    /**
     * 审核 AI 回复建议。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param suggestionId 建议标识
     * @param command 审核参数
     * @param actor 操作者
     * @return 审核后的建议摘要
     */
    Map<String, Object> reviewAiReplySuggestion(
            Long tenantId,
            Long workItemId,
            Long suggestionId,
            Map<String, Object> command,
            String actor);

    /**
     * 查询工单下的 AI 回复建议。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @param status 状态过滤条件
     * @param limit 返回数量上限
     * @return 回复建议摘要列表
     */
    List<Map<String, Object>> listAiReplySuggestions(Long tenantId, Long workItemId, String status, int limit);

    /**
     * 摄入私域同步任务结果。
     *
     * @param tenantId 租户标识
     * @param command 同步载荷
     * @param actor 操作者
     * @return 同步处理摘要
     */
    Map<String, Object> ingestPrivateDomainSync(Long tenantId, Map<String, Object> command, String actor);

    /**
     * 查询私域联系人。
     *
     * @param tenantId 租户标识
     * @param provider 私域服务商
     * @param ownerUserId 归属用户标识
     * @param keyword 搜索关键词
     * @param limit 返回数量上限
     * @return 私域联系人摘要列表
     */
    List<Map<String, Object>> privateDomainContacts(
            Long tenantId,
            String provider,
            String ownerUserId,
            String keyword,
            int limit);

    /**
     * 查询私域社群。
     *
     * @param tenantId 租户标识
     * @param provider 私域服务商
     * @param ownerUserId 归属用户标识
     * @param limit 返回数量上限
     * @return 私域社群摘要列表
     */
    List<Map<String, Object>> privateDomainGroups(Long tenantId, String provider, String ownerUserId, int limit);

    /**
     * 查询私域同步任务运行记录。
     *
     * @param tenantId 租户标识
     * @param provider 私域服务商
     * @param limit 返回数量上限
     * @return 私域同步运行摘要列表
     */
    List<Map<String, Object>> privateDomainSyncRuns(Long tenantId, String provider, int limit);
}
