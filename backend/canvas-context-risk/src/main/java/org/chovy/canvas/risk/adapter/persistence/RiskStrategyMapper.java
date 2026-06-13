package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.risk.adapter.persistence.RiskStrategyDO;

/**
 * 风控策略 MyBatis Mapper。
 */
@Mapper
public interface RiskStrategyMapper extends BaseMapper<RiskStrategyDO> {
}
