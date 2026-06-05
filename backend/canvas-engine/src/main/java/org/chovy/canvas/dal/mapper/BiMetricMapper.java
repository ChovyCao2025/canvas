package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.BiMetricDO;

@Mapper
public interface BiMetricMapper extends BaseMapper<BiMetricDO> {

    @Delete("""
            DELETE FROM bi_metric
            WHERE tenant_id = #{tenantId}
              AND dataset_id = #{datasetId}
            """)
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
    int updateMetricContract(@Param("tenantId") Long tenantId,
                             @Param("datasetId") Long datasetId,
                             @Param("metricKey") String metricKey,
                             @Param("expression") String expression,
                             @Param("allowedDimensionsJson") String allowedDimensionsJson);
}
