package org.chovy.canvas.canvas.domain;

import org.springframework.stereotype.Component;

/**
 * 封装CanvasStateTransitionPolicy相关的业务逻辑。
 */
@Component
public class CanvasStateTransitionPolicy {

    /**
     * 处理assertTransition。
     */
    public void assertTransition(Canvas canvas, CanvasStatus target) {
        CanvasStatus source = statusOf(canvas);
        if (source == target) {
            return;
        }
        if (source == CanvasStatus.KILLED || source == CanvasStatus.ARCHIVED) {
            reject(source, target);
        }
        boolean allowed = switch (target) {
            case PUBLISHED -> source == CanvasStatus.DRAFT
                    || source == CanvasStatus.PUBLISHED
                    || source == CanvasStatus.OFFLINE;
            case OFFLINE, KILLED -> source == CanvasStatus.PUBLISHED;
            case ARCHIVED -> source == CanvasStatus.DRAFT
                    || source == CanvasStatus.PUBLISHED
                    || source == CanvasStatus.OFFLINE;
            case DRAFT -> false;
        };
        if (!allowed) {
            reject(source, target);
        }
    }

    /**
     * 处理assertDraftUpdateAllowed。
     */
    public void assertDraftUpdateAllowed(Canvas canvas) {
        CanvasStatus source = statusOf(canvas);
        if (source == CanvasStatus.KILLED || source == CanvasStatus.ARCHIVED) {
            reject(source, CanvasStatus.DRAFT);
        }
    }

    /**
     * 判断Published。
     */
    public boolean isPublished(Canvas canvas) {
        return statusOf(canvas) == CanvasStatus.PUBLISHED;
    }

    /**
     * 处理statusOf。
     */
    private static CanvasStatus statusOf(Canvas canvas) {
        if (canvas == null) {
            throw new IllegalArgumentException("canvas must not be null");
        }
        return canvas.status();
    }

    /**
     * 处理reject。
     */
    private static void reject(CanvasStatus source, CanvasStatus target) {
        throw new IllegalStateException("Illegal canvas state transition: "
                + source.name() + " -> " + target.name());
    }
}
