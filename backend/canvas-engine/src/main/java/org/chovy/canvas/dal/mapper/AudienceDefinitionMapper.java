package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人群定义 Mapper（表：audience_definition）。
 *
 * <p>存储人群规则、计算策略与运行参数。
 */
@Mapper
public interface AudienceDefinitionMapper extends BaseMapper<AudienceDefinitionDO> {
    // 人群规则 JSON 的解析和执行不在 Mapper 层处理。
    // 计算任务调度和状态更新在 AudienceBatchComputeService。
    // 删除人群时对应 bitmap 清理由业务服务触发。
}
