package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提供 CdpEventLog 的 MyBatis-Plus 数据访问入口。
 */
@Mapper
public interface CdpEventLogMapper extends BaseMapper<CdpEventLogDO> {
}
