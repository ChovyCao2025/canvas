package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.OpsFacade;
import org.chovy.canvas.platform.domain.OpsCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 运维应用服务，负责运行时状态、审计事件和应急动作。
 */
@Service
public class OpsApplicationService implements OpsFacade {

    /**
     * 运维接口缺省租户标识。
     */
    private static final Long DEFAULT_TENANT_ID = 7L;

    /**
     * 运维接口缺省操作者。
     */
    private static final String DEFAULT_ACTOR = "operator-1";

    /**
     * 保存运维状态和审计数据的目录。
     */
    private final OpsCatalog catalog;

    /**
     * 使用默认内存目录创建运维应用服务。
     */
    public OpsApplicationService() {
        this(new OpsCatalog());
    }

    /**
     * 使用指定目录创建运维应用服务。
     *
     * @param catalog 运维目录
     */
    public OpsApplicationService(OpsCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 失效画布运行时缓存。
     *
     * @param tenantId 租户标识
     * @param canvasId 画布标识
     * @param actor 操作者
     * @return 缓存失效结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> invalidateCache(Long tenantId, Long canvasId, String actor) {
        return catalog.invalidateCache(safeTenantId(tenantId), canvasId, actorOrDefault(actor));
    }

    /**
     * 重建租户运行时状态。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @return 重建结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rebuildRuntimeState(Long tenantId, String actor) {
        return catalog.rebuildRuntimeState(safeTenantId(tenantId), actorOrDefault(actor));
    }

    /**
     * 查询运行时状态。
     *
     * @param tenantId 租户标识
     * @param role 操作者角色
     * @param actor 操作者
     * @return 运行时状态记录
     */
    @Override
    public Map<String, Object> runtimeStatus(Long tenantId, String role, String actor) {
        return catalog.runtimeStatus(safeTenantId(tenantId), roleOrDefault(role), actorOrDefault(actor));
    }

    /**
     * 查询审计事件。
     *
     * @param tenantId 租户标识
     * @param limit 最大返回数量
     * @return 审计事件列表
     */
    @Override
    public List<Map<String, Object>> auditEvents(Long tenantId, Integer limit) {
        return catalog.auditEvents(safeTenantId(tenantId), normalizeLimit(limit));
    }

    /**
     * 执行应急动作。
     *
     * @param tenantId 租户标识
     * @param canvasId 画布标识
     * @param action 应急动作名称
     * @param payload 应急动作参数
     * @param role 操作者角色
     * @param actor 操作者
     * @return 应急动作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> emergencyAction(Long tenantId, Long canvasId, String action, Map<String, Object> payload,
                                               String role, String actor) {
        return catalog.emergencyAction(safeTenantId(tenantId), canvasId, action, safePayload(payload),
                roleOrDefault(role), actorOrDefault(actor));
    }

    /**
     * 将缺失或非法租户标识归一到运维演示租户。
     *
     * @param tenantId 原始租户标识
     * @return 可传递给目录层的租户标识
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? DEFAULT_TENANT_ID : tenantId;
    }

    /**
     * 将缺失操作者归一为默认运维操作者。
     *
     * @param actor 原始操作者
     * @return 可审计的操作者名称
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    /**
     * 将角色归一为大写运维角色。
     *
     * @param role 原始角色
     * @return 标准化角色
     */
    private static String roleOrDefault(String role) {
        return role == null || role.isBlank() ? "OPERATOR" : role.trim().toUpperCase();
    }

    /**
     * 将空请求体归一为空 Map。
     *
     * @param payload 原始请求体
     * @return 非空请求体
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    /**
     * 将审计查询数量限制归一到运维接口允许范围。
     *
     * @param limit 原始限制数量
     * @return 1 到 500 之间的限制数量；缺失时返回 50
     */
    private static int normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 50 : Math.min(limit, 500);
    }
}
