package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.risk.adapter.persistence.RiskStrategyVersionDO;

/**
 * 风控策略版本 MyBatis Mapper。
 */
@Mapper
public interface RiskStrategyVersionMapper extends BaseMapper<RiskStrategyVersionDO> {
}
