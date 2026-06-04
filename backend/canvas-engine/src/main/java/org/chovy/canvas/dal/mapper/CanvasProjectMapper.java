package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.CanvasProjectDO;

@Mapper
public interface CanvasProjectMapper extends BaseMapper<CanvasProjectDO> {
}
