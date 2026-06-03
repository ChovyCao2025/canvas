package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.springframework.stereotype.Component;

/**
 * Single state machine for canvas lifecycle transitions.
 */
@Component
public class CanvasStateTransitionPolicy {

    public void assertTransition(CanvasDO canvas, CanvasStatusEnum target) {
        CanvasStatusEnum source = statusOf(canvas);
        if (source == target) {
            return;
        }
        if (source == CanvasStatusEnum.KILLED || source == CanvasStatusEnum.ARCHIVED) {
            reject(source, target);
        }

        boolean allowed = switch (target) {
            case PUBLISHED -> source == CanvasStatusEnum.DRAFT
                    || source == CanvasStatusEnum.PUBLISHED
                    || source == CanvasStatusEnum.OFFLINE;
            case OFFLINE -> source == CanvasStatusEnum.PUBLISHED;
            case KILLED -> source == CanvasStatusEnum.PUBLISHED;
            case ARCHIVED -> source == CanvasStatusEnum.DRAFT
                    || source == CanvasStatusEnum.PUBLISHED
                    || source == CanvasStatusEnum.OFFLINE;
            case DRAFT -> false;
        };
        if (!allowed) {
            reject(source, target);
        }
    }

    private static CanvasStatusEnum statusOf(CanvasDO canvas) {
        if (canvas == null) {
            throw new IllegalArgumentException("canvas must not be null");
        }
        Integer status = canvas.getStatus();
        for (CanvasStatusEnum candidate : CanvasStatusEnum.values()) {
            if (candidate.getCode().equals(status)) {
                return candidate;
            }
        }
        throw new IllegalStateException("UNKNOWN canvas state: " + status);
    }

    private static void reject(CanvasStatusEnum source, CanvasStatusEnum target) {
        throw new IllegalStateException("Illegal canvas state transition: "
                + source.name() + " -> " + target.name());
    }
}
