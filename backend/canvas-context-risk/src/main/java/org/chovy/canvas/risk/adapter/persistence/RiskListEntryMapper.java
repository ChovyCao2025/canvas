package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.risk.adapter.persistence.RiskListEntryDO;

/**
 * 风控名单条目 MyBatis Mapper。
 */
@Mapper
public interface RiskListEntryMapper extends BaseMapper<RiskListEntryDO> {
}
