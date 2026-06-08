package org.chovy.canvas.domain.bi.dataset;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;

/**
 * BiDatasetExtractMaterializer 定义 domain.bi.dataset 场景中的扩展契约。
 */
public interface BiDatasetExtractMaterializer {

    /**
     * 将数据集按照加速策略物化为可被 Quick Engine 直接读取的抽取表。
     *
     * <p>实现方需要使用租户维度隔离物化表名和刷新结果，并以 {@link BiDatasetExtractMaterializationResult}
     * 返回本次刷新后的物化表、行数和校验信息；调用方负责调度状态、缓存失效和旧表清理。</p>
     *
     * @param tenantId 租户 ID，用于隔离物化表和刷新状态
     * @param datasetSpec 数据集 SQL、租户列、字段和指标定义
     * @param policy 数据集加速策略，决定抽取模式、刷新方式和保留规则
     * @return 物化刷新结果，包含表名、行数、耗时和异常状态
     */
    BiDatasetExtractMaterializationResult materialize(Long tenantId,
                                                      BiDatasetSpec datasetSpec,
                                                      BiDatasetAccelerationPolicyView policy);

    /**
     * 执行 dropMaterializedTable 流程，围绕 drop materialized table 完成校验、计算或结果组装。
     *
     * @param materializedTable materialized table 参数，用于 dropMaterializedTable 流程中的校验、计算或对象转换。
     * @return 返回 drop materialized table 的布尔判断结果。
     */
    default boolean dropMaterializedTable(String materializedTable) {
        return false;
    }

    /**
     * 执行 unavailable 流程，围绕 unavailable 完成校验、计算或结果组装。
     *
     * @return 返回 unavailable 流程生成的业务结果。
     */
    static BiDatasetExtractMaterializer unavailable() {
        return new BiDatasetExtractMaterializer() {
            /**
             * 明确表示当前环境未配置抽取物化器。
             *
             * @param tenantId 租户 ID
             * @param datasetSpec 数据集规格
             * @param policy 加速策略
             * @return 不会返回；始终抛出异常提醒调用方缺少生产级物化实现
             */
            @Override
            public BiDatasetExtractMaterializationResult materialize(Long tenantId,
                                                                     BiDatasetSpec datasetSpec,
                                                                     BiDatasetAccelerationPolicyView policy) {
                throw new IllegalStateException("BI dataset extract materializer is unavailable");
            }
        };
    }
}
