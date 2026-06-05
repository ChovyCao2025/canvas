package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.CanvasAuditLogDO;

@Mapper
public interface CanvasAuditLogMapper extends BaseMapper<CanvasAuditLogDO> {
}
