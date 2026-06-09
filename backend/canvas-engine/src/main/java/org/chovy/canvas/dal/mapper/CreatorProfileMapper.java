package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.CreatorProfileDO;

/**
 * CreatorProfileMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CreatorProfileMapper extends BaseMapper<CreatorProfileDO> {
}
