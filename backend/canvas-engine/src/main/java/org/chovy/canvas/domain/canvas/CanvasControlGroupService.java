package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.dal.dataobject.CanvasControlGroupHoldoutDO;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasControlGroupHoldoutMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CanvasControlGroupService {

    private static final String DEFAULT_SALT = "default";

    private final CanvasControlGroupHoldoutMapper holdoutMapper;

    public CanvasControlGroupService(CanvasControlGroupHoldoutMapper holdoutMapper) {
        this.holdoutMapper = holdoutMapper;
    }

    public boolean isHeldOut(CanvasDO canvas, String userId) {
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
        return bucket < percent * 100;
    }

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
        } catch (DuplicateKeyException ignored) {
            // Replayed events should not create duplicate holdout audit rows.
        }
    }
}
