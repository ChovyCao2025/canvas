package org.chovy.canvas.engine.delivery;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DeliveryOutboxDO 参与 engine.delivery 场景的画布执行引擎处理。
 */
@Data
@Builder(toBuilder = true)
public class DeliveryOutboxDO {
    /** 发件箱记录主键，用于投递任务持久化和状态更新。 */
    private Long id;
    /** 租户 ID，用于隔离调度、查询、投递和治理数据。 */
    private Long tenantId;
    /** 关联的消息发送记录 ID，用于回写投递结果。 */
    private Long messageSendRecordId;
    /** 画布执行实例 ID，用于关联一次营销旅程运行。 */
    private String executionId;
    /** 画布 ID，用于追踪投递任务所属的营销流程。 */
    private Long canvasId;
    /** 目标用户 ID，用于定位本次消息投递对象。 */
    private String userId;
    /** 触发投递的画布节点 ID，用于回溯投递来源。 */
    private String nodeId;
    /** 投递渠道，用于区分短信、邮件、推送等发送路径。 */
    private String channel;
    /** 投递服务商标识，用于匹配回执来源和发件箱记录。 */
    private String provider;
    /** 投递载荷 JSON，用于保存发送服务需要的消息内容和参数。 */
    private String payloadJson;
    /** 幂等键，用于避免重复投递或重复处理同一回执。 */
    private String idempotencyKey;
    /** 业务状态，用于控制配置启用、检查结果或回执处理分支。 */
    private String status;
    /** 已尝试投递次数，用于控制重试策略和失败判定。 */
    private int attemptCount;
    /** 下次重试时间，用于调度发件箱补偿投递。 */
    private LocalDateTime nextRetryAt;
    /** 锁定该投递任务的 worker 标识，用于并发调度互斥。 */
    private String lockedBy;
    /** 投递任务被锁定的时间，用于释放超时锁和排查卡住任务。 */
    private LocalDateTime lockedAt;
    /** 服务商侧消息 ID，用于关联投递请求和异步回执。 */
    private String providerMessageId;
    /** 服务商响应 JSON，用于保存发送结果和排障证据。 */
    private String providerResponseJson;
    /** 最近一次投递错误信息，用于失败重试和人工排查。 */
    private String lastError;
    /** 发件箱记录创建时间，用于调度排序和生命周期管理。 */
    private LocalDateTime createdAt;
    /** 发件箱记录更新时间，用于判断状态是否被最近推进。 */
    private LocalDateTime updatedAt;
    /** 是否被识别为重复投递，用于跳过实际发送并保留审计痕迹。 */
    private boolean duplicate;

    /**
     * markDuplicate 处理 engine.delivery 场景的业务逻辑。
     * @param duplicate duplicate 参数，用于 markDuplicate 流程中的校验、计算或对象转换。
     * @return 返回 markDuplicate 流程生成的业务结果。
     */
    public DeliveryOutboxDO markDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
        return this;
    }
}
