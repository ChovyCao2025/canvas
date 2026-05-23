package org.chovy.canvas.domain.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 画布执行记录 Mapper（表：canvas_execution）。
 *
 * <p>记录一次 execution 的主状态、触发类型与耗时信息。
 */
@Mapper
public interface CanvasExecutionMapper extends BaseMapper<CanvasExecution> {
    // 节点级轨迹不在该表，详见 CanvasExecutionTraceMapper。
    // 该表更偏“一次执行汇总”，用于列表和概览指标。
    // 超时/失败后的补偿状态更新也会落在该表。
}
