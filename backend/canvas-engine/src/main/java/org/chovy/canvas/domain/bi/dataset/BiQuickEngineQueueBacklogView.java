package org.chovy.canvas.domain.bi.dataset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * BiQuickEngineQueueBacklogView 编排 domain.bi.dataset 场景的领域业务规则。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BiQuickEngineQueueBacklogView {

    /** 租户 ID，用于隔离调度、查询、投递和治理数据。 */
    private Long tenantId;
    /** 快引擎资源池唯一键，用于聚合队列积压情况。 */
    private String poolKey;
    /** 已就绪但尚未执行的任务数量，用于衡量调度压力。 */
    private Long readyCount;
    /** 最早入队时间，用于判断快引擎队列积压时长。 */
    private LocalDateTime oldestQueuedAt;
}
