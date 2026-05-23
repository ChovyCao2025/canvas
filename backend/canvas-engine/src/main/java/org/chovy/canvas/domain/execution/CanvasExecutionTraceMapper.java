package org.chovy.canvas.domain.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 节点执行轨迹 Mapper（表：canvas_execution_trace）。
 *
 * <p>用于执行链路明细查询与漏斗统计。
 */
@Mapper
public interface CanvasExecutionTraceMapper extends BaseMapper<CanvasExecutionTrace> {

    /**
     * 批量插入轨迹记录（TraceWriteBuffer 每 500ms 调用一次）。
     * SQL 在 CanvasExecutionTraceMapper.xml 中定义。
     */
    void insertBatch(@Param("list") List<CanvasExecutionTrace> list);

    /**
     * 节点漏斗统计：按 canvas_id 聚合每个节点的进入/成功/失败/跳过次数（设计文档 21.3节）。
     * SQL 在 CanvasExecutionTraceMapper.xml 中定义。
     */
    List<Map<String, Object>> selectFunnelByCanvasId(@Param("canvasId") Long canvasId);

    // TraceWriteBuffer 聚合写入可显著降低高并发场景下的单条 insert 压力。
    // 轨迹表通常数据量最大，查询时建议带 canvasId/time 范围条件。
}
