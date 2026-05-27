package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.CustomerTaskRecordDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户任务记录 MyBatis-Plus Mapper。
 *
 * <p>继承 BaseMapper 为 {@code CustomerTaskRecordDO} 提供基础 CRUD 能力，复杂查询可在同名 XML 中扩展。
 * <p>该接口只定义数据访问边界，不承载业务编排或跨表事务逻辑。
 */
@Mapper
public interface CustomerTaskRecordMapper extends BaseMapper<CustomerTaskRecordDO> {
}
