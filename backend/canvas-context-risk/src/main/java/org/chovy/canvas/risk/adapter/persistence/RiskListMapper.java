package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.risk.adapter.persistence.RiskListDO;

/**
 * 风控名单 MyBatis Mapper。
 */
@Mapper
public interface RiskListMapper extends BaseMapper<RiskListDO> {
}
