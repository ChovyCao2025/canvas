package org.chovy.canvas.canvas.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定义CanvasProjectMapper对外提供的能力契约。
 */
@Mapper
public interface CanvasProjectMapper extends BaseMapper<CanvasProjectDO> {
}
