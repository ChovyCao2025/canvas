package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.RiskListEntryDO;

/**
 * 风控名单条目 MyBatis Mapper。
 */
@Mapper
public interface RiskListEntryMapper extends BaseMapper<RiskListEntryDO> {
}
