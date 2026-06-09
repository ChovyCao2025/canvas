package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.CanvasAuditLogDO;

/**
 * CanvasAuditLogMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CanvasAuditLogMapper extends BaseMapper<CanvasAuditLogDO> {
}
