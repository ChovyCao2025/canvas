package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.RiskRuleHitDO;

/**
 * 风控规则命中 MyBatis Mapper。
 */
@Mapper
public interface RiskRuleHitMapper extends BaseMapper<RiskRuleHitDO> {
}
