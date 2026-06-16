package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

/**
 * 提供渠道连接器配置、限流、健康检查和回退决策能力的应用入口。
 */
public interface ChannelConnectorFacade {

    /**
     * 查询租户下的渠道连接器。
     *
     * @param tenantId 租户标识
     * @return 渠道连接器列表
     */
    List<Map<String, Object>> connectors(Long tenantId);

    /**
     * 查询租户下的渠道限流配置。
     *
     * @param tenantId 租户标识
     * @return 限流配置列表
     */
    List<Map<String, Object>> limits(Long tenantId);

    /**
     * 更新渠道连接器运行模式。
     *
     * @param tenantId 租户标识
     * @param connectorId 连接器标识
     * @param payload 模式更新参数
     * @param actor 操作者
     * @return 更新后的连接器记录
     */
    Map<String, Object> updateMode(Long tenantId, Long connectorId, Map<String, Object> payload, String actor);

    /**
     * 对渠道连接器执行健康检查。
     *
     * @param tenantId 租户标识
     * @param connectorId 连接器标识
     * @return 健康检查结果
     */
    Map<String, Object> healthTest(Long tenantId, Long connectorId);

    /**
     * 校验渠道回退策略。
     *
     * @param tenantId 租户标识
     * @param payload 回退策略参数
     * @return 校验结果
     */
    Map<String, Object> validateFallback(Long tenantId, Map<String, Object> payload);

    /**
     * 查询渠道回退决策记录。
     *
     * @param tenantId 租户标识
     * @return 回退决策记录列表
     */
    List<Map<String, Object>> fallbackDecisions(Long tenantId);

    /**
     * 查询渠道去重记录。
     *
     * @param tenantId 租户标识
     * @return 去重记录列表
     */
    List<Map<String, Object>> dedupeRecords(Long tenantId);
}
