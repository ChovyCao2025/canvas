package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.CanvasMqTriggerRejectedDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MQ 触发拒绝记录 MyBatis-Plus Mapper。
 *
 * <p>继承 BaseMapper 为 {@code CanvasMqTriggerRejectedDO} 提供基础 CRUD 能力，复杂查询可在同名 XML 中扩展。
 * <p>该接口只定义数据访问边界，不承载业务编排或跨表事务逻辑。
 */
@Mapper
public interface CanvasMqTriggerRejectedMapper extends BaseMapper<CanvasMqTriggerRejectedDO> {
}
