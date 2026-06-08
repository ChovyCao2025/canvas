package org.chovy.canvas.domain.bi.query;

import java.util.List;

/**
 * BiDatasetSpecResolver 定义 domain.bi.query 场景中的扩展契约。
 */
public interface BiDatasetSpecResolver {

    /**
     * 按数据集 key 解析租户可见的数据集规格。
     *
     * @param datasetKey 数据集稳定业务 key
     * @param tenantId 当前租户 ID，扩展实现可据此加载租户自定义数据集或隔离数据目录
     * @return 数据集表表达式、租户列、字段、指标和 SQL 参数定义
     */
    BiDatasetSpec dataset(String datasetKey, Long tenantId);

    /**
     * 返回租户可用于查询、AI 规划和看板建模的数据集目录。
     *
     * @param tenantId 当前租户 ID，内置实现忽略该值，自定义实现可按租户过滤
     * @return 数据集规格列表，字段和指标口径应已完成权限范围内过滤
     */
    List<BiDatasetSpec> datasets(Long tenantId);

    /**
     * 执行 builtIn 流程，围绕 built in 完成校验、计算或结果组装。
     *
     * @return 返回 builtIn 流程生成的业务结果。
     */
    static BiDatasetSpecResolver builtIn() {
        return new BiDatasetSpecResolver() {
            /**
             * 从内置营销 BI 注册表读取数据集规格。
             *
             * @param datasetKey 内置数据集 key
             * @param tenantId 租户 ID；内置注册表不按租户分叉，但保留接口语义
             * @return 内置数据集规格
             */
            @Override
            public BiDatasetSpec dataset(String datasetKey, Long tenantId) {
                return MarketingBiDatasetRegistry.dataset(datasetKey);
            }

            /**
             * 返回内置营销 BI 注册表中的全部数据集规格。
             *
             * @param tenantId 租户 ID；内置注册表不按租户过滤
             * @return 内置数据集目录
             */
            @Override
            public List<BiDatasetSpec> datasets(Long tenantId) {
                return MarketingBiDatasetRegistry.datasets();
            }
        };
    }
}
