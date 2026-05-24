package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 执行死信队列 Mapper（表：canvas_execution_dlq）。
 *
 * <p>保存失败可重放的执行消息，供 DLQ 回放任务消费。
 */
@Mapper
public interface CanvasExecutionDlqMapper extends BaseMapper<CanvasExecutionDlqDO> {
    // 回放扫描与重试节奏由 DLQ 任务控制，不在 Mapper 层实现。
    // 死信保留时长与清理策略通常由运维任务配置。
    // 业务侧可通过 executionId/msgId 反查死信原因。
}
