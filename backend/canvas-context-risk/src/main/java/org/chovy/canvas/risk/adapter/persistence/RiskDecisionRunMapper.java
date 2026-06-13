package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.risk.adapter.persistence.RiskDecisionRunDO;

/**
 * 风控决策运行 MyBatis Mapper。
 */
@Mapper
public interface RiskDecisionRunMapper extends BaseMapper<RiskDecisionRunDO> {
}
