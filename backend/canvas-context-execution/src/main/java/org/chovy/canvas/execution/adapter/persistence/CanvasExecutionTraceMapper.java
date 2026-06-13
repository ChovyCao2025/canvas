package org.chovy.canvas.execution.adapter.persistence;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CanvasExecutionTraceMapper extends BaseMapper<CanvasExecutionTraceDO> {

    void insertBatch(@Param("list") List<CanvasExecutionTraceDO> list);

    @Select("""
            SELECT
                id,
                tenant_id AS tenantId,
                execution_id AS executionId,
                node_id AS nodeId,
                node_type AS nodeType,
                node_name AS nodeName,
                status,
                input_data AS inputData,
                output_data AS outputData,
                error_msg AS errorMsg,
                started_at AS startedAt,
                finished_at AS finishedAt,
                duration_ms AS durationMs
            FROM canvas_execution_trace
            WHERE execution_id = #{executionId}
            ORDER BY started_at ASC, id ASC
            """)
    List<CanvasExecutionTraceDO> selectByExecutionId(@Param("executionId") String executionId);

    List<Map<String, Object>> selectFunnelByCanvasId(@Param("canvasId") Long canvasId);
}
