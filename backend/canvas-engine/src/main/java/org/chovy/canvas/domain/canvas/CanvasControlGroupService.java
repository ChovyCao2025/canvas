package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.dal.dataobject.CanvasControlGroupHoldoutDO;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasControlGroupHoldoutMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * CanvasControlGroupService 编排 domain.canvas 场景的领域业务规则。
 */
@Service
public class CanvasControlGroupService {

    private static final String DEFAULT_SALT = "default";

    private final CanvasControlGroupHoldoutMapper holdoutMapper;

    /**
     * 创建 CanvasControlGroupService 实例并注入 domain.canvas 场景依赖。
     * @param holdoutMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CanvasControlGroupService(CanvasControlGroupHoldoutMapper holdoutMapper) {
        this.holdoutMapper = holdoutMapper;
    }

    /**
     * 判断用户是否落入指定 Canvas 的控制组。
     * 使用画布 ID、控制组盐和 userId 做稳定分桶，同一用户结果可复现；控制组比例超过 50% 会被拒绝以防误配。
     */
    public boolean isHeldOut(CanvasDO canvas, String userId) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (canvas == null || userId == null || userId.isBlank()) {
            return false;
        }
        int percent = canvas.getControlGroupPercent() == null ? 0 : canvas.getControlGroupPercent();
        if (percent <= 0) {
            return false;
        }
        if (percent > 50) {
            throw new IllegalArgumentException("controlGroupPercent cannot exceed 50");
        }
        String salt = canvas.getControlGroupSalt() == null || canvas.getControlGroupSalt().isBlank()
                ? DEFAULT_SALT
                : canvas.getControlGroupSalt().trim();
        int bucket = Math.floorMod((canvas.getId() + ":" + salt + ":" + userId).hashCode(), 10_000);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return bucket < percent * 100;
    }

    /**
     * 记录用户被控制组拦截的审计行。
     * 方法对缺失 canvasId 或 userId 的调用保持空操作，并通过唯一键处理事件重放，避免重复留痕。
     */
    public void recordHoldout(Long canvasId, String userId, String eventId, String reason) {
        if (canvasId == null || userId == null || userId.isBlank()) {
            return;
        }
        CanvasControlGroupHoldoutDO row = new CanvasControlGroupHoldoutDO();
        row.setCanvasId(canvasId);
        row.setUserId(userId);
        row.setEventId(eventId);
        row.setReason(reason == null || reason.isBlank() ? "CONTROL_GROUP" : reason);
        row.setCreatedAt(LocalDateTime.now());
        try {
            holdoutMapper.insert(row);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (DuplicateKeyException ignored) {
            // Replayed events should not create duplicate holdout audit rows.
        }
    }
}
