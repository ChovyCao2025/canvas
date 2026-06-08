package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.BiDatasetFieldDO;

/**
 * BiDatasetFieldMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface BiDatasetFieldMapper extends BaseMapper<BiDatasetFieldDO> {

    @Delete("""
            DELETE FROM bi_dataset_field
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
}
