package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.BiMetricDO;

/**
 * BiMetricMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface BiMetricMapper extends BaseMapper<BiMetricDO> {

    @Delete("""
            DELETE FROM bi_metric
            WHERE tenant_id = #{tenantId}
              AND dataset_id = #{datasetId}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @return 返回 delete by dataset 计算得到的数量、金额或指标值。
     */
    int deleteByDataset(@Param("tenantId") Long tenantId, @Param("datasetId") Long datasetId);

    @Update("""
            UPDATE bi_metric
            SET expression = #{expression},
                allowed_dimensions_json = #{allowedDimensionsJson},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND dataset_id = #{datasetId}
              AND metric_key = #{metricKey}
              AND (status IS NULL OR status <> 'ARCHIVED')
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @param metricKey 业务键，用于在同一租户下定位资源。
     * @param expression expression 参数，用于 updateMetricContract 流程中的校验、计算或对象转换。
     * @param allowedDimensionsJson JSON 字符串，承载结构化配置或明细。
     * @return 返回流程执行后的业务结果。
     */
    int updateMetricContract(@Param("tenantId") Long tenantId,
                             @Param("datasetId") Long datasetId,
                             @Param("metricKey") String metricKey,
                             @Param("expression") String expression,
                             @Param("allowedDimensionsJson") String allowedDimensionsJson);
}
