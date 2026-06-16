package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.ChannelConnectorFacade;
import org.chovy.canvas.platform.domain.ChannelConnectorCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 渠道连接器应用服务，负责连接器查询、模式切换和回退校验。
 */
@Service
public class ChannelConnectorApplicationService implements ChannelConnectorFacade {

    /**
     * 渠道连接器接口缺省租户标识。
     */
    private static final Long DEFAULT_TENANT_ID = 7L;

    /**
     * 渠道连接器接口缺省操作者。
     */
    private static final String DEFAULT_ACTOR = "operator-1";

    /**
     * 保存渠道连接器、限流和回退数据的目录。
     */
    private final ChannelConnectorCatalog catalog;

    /**
     * 使用默认内存目录创建渠道连接器应用服务。
     */
    public ChannelConnectorApplicationService() {
        this(new ChannelConnectorCatalog());
    }

    /**
     * 使用指定目录创建渠道连接器应用服务。
     *
     * @param catalog 渠道连接器目录
     */
    public ChannelConnectorApplicationService(ChannelConnectorCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 查询渠道连接器。
     *
     * @param tenantId 租户标识
     * @return 连接器列表
     */
    @Override
    public List<Map<String, Object>> connectors(Long tenantId) {
        return catalog.connectors(safeTenantId(tenantId));
    }

    /**
     * 查询渠道限流配置。
     *
     * @param tenantId 租户标识
     * @return 限流配置列表
     */
    @Override
    public List<Map<String, Object>> limits(Long tenantId) {
        return catalog.limits(safeTenantId(tenantId));
    }

    /**
     * 更新连接器模式。
     *
     * @param tenantId 租户标识
     * @param connectorId 连接器标识
     * @param payload 模式更新参数
     * @param actor 操作者
     * @return 更新后的连接器记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateMode(Long tenantId, Long connectorId, Map<String, Object> payload, String actor) {
        Map<String, Object> safePayload = safePayload(payload);
        return catalog.updateMode(safeTenantId(tenantId), connectorId, stringValue(safePayload, "mode"),
                stringValue(safePayload, "reason"), actorOrDefault(actor));
    }

    /**
     * 执行连接器健康检查。
     *
     * @param tenantId 租户标识
     * @param connectorId 连接器标识
     * @return 健康检查结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> healthTest(Long tenantId, Long connectorId) {
        return catalog.healthTest(safeTenantId(tenantId), connectorId);
    }

    /**
     * 校验渠道回退链路。
     *
     * @param tenantId 租户标识
     * @param payload 回退校验参数
     * @return 校验结果
     */
    @Override
    public Map<String, Object> validateFallback(Long tenantId, Map<String, Object> payload) {
        Map<String, Object> safePayload = safePayload(payload);
        // 回退校验只需要抽取通道和供应方字段，避免把额外请求字段传入目录层。
        return catalog.validateFallback(safeTenantId(tenantId),
                stringValue(safePayload, "channel"),
                stringValue(safePayload, "provider"),
                stringValue(safePayload, "fallbackChannel"),
                stringValue(safePayload, "fallbackProvider"));
    }

    /**
     * 查询回退决策记录。
     *
     * @param tenantId 租户标识
     * @return 回退决策列表
     */
    @Override
    public List<Map<String, Object>> fallbackDecisions(Long tenantId) {
        return catalog.fallbackDecisions(safeTenantId(tenantId));
    }

    /**
     * 查询渠道去重记录。
     *
     * @param tenantId 租户标识
     * @return 去重记录列表
     */
    @Override
    public List<Map<String, Object>> dedupeRecords(Long tenantId) {
        return catalog.dedupeRecords(safeTenantId(tenantId));
    }

    /**
     * 将缺失或非法租户标识归一到渠道演示租户。
     *
     * @param tenantId 原始租户标识
     * @return 可传递给目录层的租户标识
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId <= 0 ? DEFAULT_TENANT_ID : tenantId;
    }

    /**
     * 将缺失操作者归一为默认渠道操作者。
     *
     * @param actor 原始操作者
     * @return 可审计的操作者名称
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
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
     * 从请求体读取字符串字段。
     *
     * @param payload 请求体
     * @param key 字段键
     * @return 字符串字段值；字段缺失时返回 null
     */
    private static String stringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
