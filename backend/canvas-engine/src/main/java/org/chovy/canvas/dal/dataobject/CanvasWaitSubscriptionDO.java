package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 画布等待订阅记录（canvas_wait_subscription）。
 *
 * <p>当 DAG 执行到 WAIT 节点的事件等待模式时，引擎暂停执行并在此表写入一条
 * ACTIVE 订阅记录，等待以下两种条件之一满足后恢复：
 * <ol>
 *   <li>事件上报：{@code POST /canvas/events/report} 触发 {@link WaitResumeService#resumeEventWaits}，
 *       按 eventCode + userId 查找 ACTIVE 记录并 CAS 更新为 COMPLETED；</li>
 *   <li>超时扫描：定时任务扫描 expiresAt ≤ now 的 ACTIVE 记录，更新为 EXPIRED。</li>
 * </ol>
 *
 * <p>并发安全：{@code finishWait()} 使用 {@code WHERE status='ACTIVE'} 做乐观 CAS，
 * 多机并发恢复时只有一个实例能成功更新（行级锁），防止同一条等待被恢复两次。
 */
@Data
@TableName("canvas_wait_subscription")
public class CanvasWaitSubscriptionDO {

    @TableId(type = IdType.AUTO)
    /** 等待订阅主键 ID */
    private Long id;

    /** 所属执行实例 ID（对应 canvas_execution.id），用于恢复时关联执行上下文。 */
    private String executionId;

    /** 所属画布 ID。 */
    private Long canvasId;

    /** 执行时锁定的画布版本 ID，恢复时传回引擎以匹配正确的 DAG 图。 */
    private Long versionId;

    /** 触发用户 ID，事件恢复时与 eventCode 联合查询定位订阅记录。 */
    private String userId;

    /** 等待节点的 nodeId，恢复时作为 matchKey 定位触发器节点，直接恢复该节点之后的执行。 */
    private String nodeId;

    /**
     * 等待类型，决定恢复触发方式：
     * <ul>
     *   <li>{@code UNTIL_EVENT}：等待特定事件（可设置超时）；</li>
     *   <li>{@code UNTIL_TIME}：等待到指定时间（定时扫描恢复，无 eventCode）。</li>
     * </ul>
     */
    private String waitType;

    /**
     * 等待的事件编码，UNTIL_EVENT 必填，UNTIL_TIME 为 null。
     * 事件上报时按此字段 + userId 查找订阅记录。
     */
    private String eventCode;

    /**
     * 事件过滤条件，JSON 格式，对上报事件的 attributes 字段进行二次过滤。
     * 当前版本暂不生效（预留字段），恢复时不校验，直接恢复所有匹配的 eventCode + userId 记录。
     */
    private String eventFilters;

    /**
     * 恢复时携带的上下文快照，JSON 格式（来自暂停时的 ExecutionContext 序列化）。
     * 包含：暂停前各节点输出、待传入下游节点的事件属性、WAIT 恢复状态标记等。
     */
    private String resumePayload;

    /**
     * 订阅过期时间（精确到秒）。
     * null 表示永不超时（仅依靠事件触发恢复）。
     * 超时扫描任务查询 {@code status='ACTIVE' AND expires_at <= now} 触发 EXPIRED 恢复。
     */
    private LocalDateTime expiresAt;

    /**
     * 订阅状态：
     * <ul>
     *   <li>{@code ACTIVE}：等待中；</li>
     *   <li>{@code COMPLETED}：事件到达，正常恢复；</li>
     *   <li>{@code EXPIRED}：超时未收到事件，以超时状态恢复（WAIT 节点走超时分支）。</li>
     * </ul>
     */
    private String status;

    /** 订阅创建时间，表示执行进入等待节点的时间。 */
    private LocalDateTime createdAt;

    /** 状态更新时间，CAS 成功后由 {@code finishWait()} 写入。 */
    private LocalDateTime updatedAt;
}
