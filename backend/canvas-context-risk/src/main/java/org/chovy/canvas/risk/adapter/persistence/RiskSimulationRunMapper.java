package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.risk.adapter.persistence.RiskSimulationRunDO;

/**
 * 风控仿真运行 MyBatis Mapper。
 */
@Mapper
public interface RiskSimulationRunMapper extends BaseMapper<RiskSimulationRunDO> {
}
