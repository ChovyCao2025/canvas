package org.chovy.canvas.execution.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定义 CanvasExecutionStatsMapper 的执行上下文数据结构或业务契约。
 */
@Mapper
public interface CanvasExecutionStatsMapper extends BaseMapper<CanvasExecutionStatsDO> {
}
