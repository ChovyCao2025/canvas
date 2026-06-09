package org.chovy.canvas.domain.ops;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@Service
/**
 * OpsAuditEventService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class OpsAuditEventService {

    private static final int MAX_EVENTS = 500;

    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentLinkedDeque<OpsAuditEvent> events = new ConcurrentLinkedDeque<>();

    public OpsAuditEvent record(Long tenantId,
                                String action,
                                Long canvasId,
                                String operator,
                                String role,
                                String reason) {
        OpsAuditEvent event = new OpsAuditEvent(
                "ops-" + sequence.incrementAndGet(),
                tenantId,
                action,
                canvasId,
                operator,
                role,
                reason,
                Instant.now());
        events.addFirst(event);
        while (events.size() > MAX_EVENTS) {
            events.pollLast();
        }
        return event;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<OpsAuditEvent> recent(Long tenantId, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        List<OpsAuditEvent> result = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (OpsAuditEvent event : events) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (tenantId == null || tenantId.equals(event.tenantId())) {
                result.add(event);
            }
            if (result.size() >= normalizedLimit) {
                break;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * OpsAuditEvent 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record OpsAuditEvent(
            String id,
            Long tenantId,
            String action,
            Long canvasId,
            String operator,
            String role,
            String reason,
            Instant createdAt) {
    }
}
