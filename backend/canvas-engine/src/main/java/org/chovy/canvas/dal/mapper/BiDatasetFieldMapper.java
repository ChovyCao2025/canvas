package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.BiDatasetFieldDO;

@Mapper
public interface BiDatasetFieldMapper extends BaseMapper<BiDatasetFieldDO> {

    @Delete("""
            DELETE FROM bi_dataset_field
            WHERE tenant_id = #{tenantId}
              AND dataset_id = #{datasetId}
            """)
    int deleteByDataset(@Param("tenantId") Long tenantId, @Param("datasetId") Long datasetId);
}
