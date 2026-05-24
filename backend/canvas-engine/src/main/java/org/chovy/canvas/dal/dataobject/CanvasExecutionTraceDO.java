package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 节点执行轨迹记录（canvas_execution_trace）。
 *
 * <p>画布执行过程中，每个节点的执行均记录一条轨迹，用于排查问题和执行回放。
 * 执行结束时，所有未实际执行的节点以 SKIPPED 状态批量写入。
 *
 * <p>注意：trace.status 的含义与 {@link CanvasExecutionDO#status} 不同，
 * 这里反映的是单个节点的执行状态，而非整体执行流的状态。
 */
@Data
@Builder
@TableName("canvas_execution_trace")
public class CanvasExecutionTraceDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属执行记录 ID */
    private String executionId;

    /** 节点 ID（对应 DAG 中的 node.id） */
    private String nodeId;

    /** 节点类型（对应 DAG 中的 node.type，见 NodeType 常量） */
    private String nodeType;

    /** 节点显示名称 */
    private String nodeName;

    /**
     * 节点执行状态。
     * 0 = 执行中（RUNNING），1 = 成功（SUCCESS），2 = 失败（FAILED），3 = 跳过（SKIPPED）
     * 对应 {@link org.chovy.canvas.engine.context.NodeStatus} 枚举的序数值。
     */
    private Integer status;

    /** 节点输入数据 JSON（来自上游输出或触发 payload） */
    private String inputData;

    /** 节点输出数据 JSON */
    private String outputData;

    /** 失败时的错误信息 */
    private String errorMsg;

    /** 节点开始执行时间 */
    private LocalDateTime startedAt;

    /** 节点执行结束时间 */
    private LocalDateTime finishedAt;

    /** 节点执行耗时（毫秒） */
    private Long durationMs;
}
