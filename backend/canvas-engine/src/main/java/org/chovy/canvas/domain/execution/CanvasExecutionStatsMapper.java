package org.chovy.canvas.domain.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 画布每日统计 Mapper（表：canvas_execution_stats）。
 *
 * <p>用于按天聚合成功/失败/触发量等指标。
 */
@Mapper
public interface CanvasExecutionStatsMapper extends BaseMapper<CanvasExecutionStats> {
    // 统计写入通常来自异步聚合作业或定时汇总任务。
    // 仪表盘趋势查询优先读取该聚合表，避免扫明细。
    // 粒度为“天”，更细粒度明细请查询 trace/execution 表。
}
