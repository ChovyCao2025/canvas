package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.chovy.canvas.platform.api.ApprovalFacade;
import org.chovy.canvas.platform.domain.ApprovalCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审批应用服务，负责审批任务查询、决策和飞书审批同步。
 */
@Service
public class ApprovalApplicationService implements ApprovalFacade {

    /**
     * 审批接口缺省租户标识。
     */
    private static final Long DEFAULT_TENANT_ID = 7L;

    /**
     * 审批接口缺省操作者。
     */
    private static final String DEFAULT_ACTOR = "operator-1";

    /**
     * 审批接口缺省角色。
     */
    private static final String DEFAULT_ROLE = "OPERATOR";

    /**
     * 保存审批任务和实例数据的目录。
     */
    private final ApprovalCatalog catalog;

    /**
     * 使用默认内存目录创建审批应用服务。
     */
    public ApprovalApplicationService() {
        this(new ApprovalCatalog());
    }

    /**
     * 使用指定目录创建审批应用服务。
     *
     * @param catalog 审批目录
     */
    public ApprovalApplicationService(ApprovalCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 查询审批任务。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @param role 操作者角色
     * @param status 任务状态过滤值
     * @return 审批任务列表
     */
    @Override
    public List<Map<String, Object>> tasks(Long tenantId, String actor, String role, String status) {
        return catalog.tasks(safeTenantId(tenantId), actorOrDefault(actor), roleOrDefault(role), statusOrPending(status));
    }

    /**
     * 查询审批实例。
     *
     * @param tenantId 租户标识
     * @param targetType 审批目标类型
     * @param targetId 审批目标标识
     * @param status 实例状态过滤值
     * @return 审批实例列表
     */
    @Override
    public List<Map<String, Object>> instances(Long tenantId, String targetType, String targetId, String status) {
        return catalog.instances(safeTenantId(tenantId), targetType, targetId, normalizedStatus(status));
    }

    /**
     * 通过审批任务。
     *
     * @param tenantId 租户标识
     * @param taskId 审批任务标识
     * @param payload 审批参数
     * @param actor 操作者
     * @param role 操作者角色
     * @return 审批结果记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approve(Long tenantId, Long taskId, Map<String, Object> payload, String actor,
                                       String role) {
        return catalog.decide(safeTenantId(tenantId), taskId, actorOrDefault(actor), roleOrDefault(role),
                stringValue(payload, "comment"),
                "APPROVED");
    }

    /**
     * 驳回审批任务。
     *
     * @param tenantId 租户标识
     * @param taskId 审批任务标识
     * @param payload 驳回参数
     * @param actor 操作者
     * @param role 操作者角色
     * @return 审批结果记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reject(Long tenantId, Long taskId, Map<String, Object> payload, String actor,
                                      String role) {
        return catalog.decide(safeTenantId(tenantId), taskId, actorOrDefault(actor), roleOrDefault(role),
                stringValue(payload, "comment"),
                "REJECTED");
    }

    /**
     * 批量同步飞书审批实例。
     *
     * @param tenantId 租户标识
     * @param limit 最大同步数量
     * @param actor 操作者
     * @param role 操作者角色
     * @return 同步结果摘要
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncLarkApprovals(Long tenantId, Integer limit, String actor, String role) {
        // 飞书审批同步会写入外部审批状态，只允许管理员角色触发。
        requireAdmin(roleOrDefault(role));
        return catalog.syncLarkApprovals(safeTenantId(tenantId), normalizedLimit(limit), actorOrDefault(actor));
    }

    /**
     * 同步单个飞书审批实例。
     *
     * @param tenantId 租户标识
     * @param instanceId 本地审批实例标识
     * @param actor 操作者
     * @param role 操作者角色
     * @return 同步结果记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncLarkApprovalInstance(Long tenantId, Long instanceId, String actor, String role) {
        // 单实例同步同样需要管理员权限，避免普通审批人改写集成状态。
        requireAdmin(roleOrDefault(role));
        return catalog.syncLarkApprovalInstance(safeTenantId(tenantId), instanceId, actorOrDefault(actor));
    }

    /**
     * 将缺失或非法租户标识归一到审批演示租户。
     *
     * @param tenantId 原始租户标识
     * @return 可传递给目录层的租户标识
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId <= 0 ? DEFAULT_TENANT_ID : tenantId;
    }

    /**
     * 将缺失操作者归一为默认审批操作者。
     *
     * @param actor 原始操作者
     * @return 可审计的操作者名称
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    /**
     * 将角色归一为大写审批角色。
     *
     * @param role 原始角色
     * @return 标准化角色
     */
    private static String roleOrDefault(String role) {
        return role == null || role.isBlank() ? DEFAULT_ROLE : role.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 将空任务状态归一为待处理。
     *
     * @param status 原始状态
     * @return 任务状态
     */
    private static String statusOrPending(String status) {
        String normalized = normalizedStatus(status);
        return normalized == null ? "PENDING" : normalized;
    }

    /**
     * 将状态过滤值标准化为大写。
     *
     * @param status 原始状态
     * @return 标准化状态；空值返回 null
     */
    private static String normalizedStatus(String status) {
        return status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 将同步数量限制归一到审批同步允许范围。
     *
     * @param limit 原始限制数量
     * @return 1 到 500 之间的同步数量
     */
    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 100;
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("approval sync limit must be positive");
        }
        return Math.min(limit, 500);
    }

    /**
     * 校验角色是否具备审批同步管理员权限。
     *
     * @param role 标准化角色
     * @throws SecurityException 当角色不具备同步权限时抛出
     */
    private static void requireAdmin(String role) {
        if ("ADMIN".equals(role) || "TENANT_ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
            return;
        }
        throw new SecurityException("Lark approval sync requires admin role");
    }

    /**
     * 从请求体读取字符串字段。
     *
     * @param payload 请求体
     * @param key 字段键
     * @return 字符串字段值；字段缺失时返回 null
     */
    private static String stringValue(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
