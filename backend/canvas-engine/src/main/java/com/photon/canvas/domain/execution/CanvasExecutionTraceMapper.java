package com.photon.canvas.domain.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CanvasExecutionTraceMapper extends BaseMapper<CanvasExecutionTrace> {

    /**
     * 节点漏斗统计：按 canvas_id 聚合每个节点的进入/成功/失败/跳过次数（设计文档 21.3节）。
     */
    @Select("""
        SELECT
            t.node_id        AS nodeId,
            t.node_type      AS nodeType,
            t.node_name      AS nodeName,
            COUNT(*)         AS totalEntered,
            SUM(CASE WHEN t.status = 1 THEN 1 ELSE 0 END) AS totalSuccess,
            SUM(CASE WHEN t.status = 2 THEN 1 ELSE 0 END) AS totalFailed,
            SUM(CASE WHEN t.status = 3 THEN 1 ELSE 0 END) AS totalSkipped,
            AVG(TIMESTAMPDIFF(SECOND, t.started_at, t.finished_at)) AS avgDurationSec
        FROM canvas_execution_trace t
        JOIN canvas_execution e ON e.id = t.execution_id
        WHERE e.canvas_id = #{canvasId}
          AND t.started_at IS NOT NULL
        GROUP BY t.node_id, t.node_type, t.node_name
        ORDER BY totalEntered DESC
        """)
    List<Map<String, Object>> selectFunnelByCanvasId(@Param("canvasId") Long canvasId);
}
