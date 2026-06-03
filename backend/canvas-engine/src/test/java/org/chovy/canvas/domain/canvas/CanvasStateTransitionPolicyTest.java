package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanvasStateTransitionPolicyTest {

    private final CanvasStateTransitionPolicy policy = new CanvasStateTransitionPolicy();

    @Test
    void transitionMatrixAllowsExplicitLifecycleTransitions() {
        assertAllowed(CanvasStatusEnum.DRAFT, CanvasStatusEnum.PUBLISHED);
        assertAllowed(CanvasStatusEnum.DRAFT, CanvasStatusEnum.ARCHIVED);
        assertAllowed(CanvasStatusEnum.PUBLISHED, CanvasStatusEnum.PUBLISHED);
        assertAllowed(CanvasStatusEnum.PUBLISHED, CanvasStatusEnum.OFFLINE);
        assertAllowed(CanvasStatusEnum.PUBLISHED, CanvasStatusEnum.KILLED);
        assertAllowed(CanvasStatusEnum.PUBLISHED, CanvasStatusEnum.ARCHIVED);
        assertAllowed(CanvasStatusEnum.OFFLINE, CanvasStatusEnum.PUBLISHED);
        assertAllowed(CanvasStatusEnum.OFFLINE, CanvasStatusEnum.ARCHIVED);
        assertAllowed(CanvasStatusEnum.KILLED, CanvasStatusEnum.KILLED);
        assertAllowed(CanvasStatusEnum.ARCHIVED, CanvasStatusEnum.ARCHIVED);
    }

    @Test
    void transitionMatrixRejectsImplicitRecoveryOrInvalidTargets() {
        assertRejected(CanvasStatusEnum.DRAFT, CanvasStatusEnum.OFFLINE);
        assertRejected(CanvasStatusEnum.DRAFT, CanvasStatusEnum.KILLED);
        assertRejected(CanvasStatusEnum.PUBLISHED, CanvasStatusEnum.DRAFT);
        assertRejected(CanvasStatusEnum.OFFLINE, CanvasStatusEnum.KILLED);
        assertRejected(CanvasStatusEnum.KILLED, CanvasStatusEnum.PUBLISHED);
        assertRejected(CanvasStatusEnum.KILLED, CanvasStatusEnum.ARCHIVED);
        assertRejected(CanvasStatusEnum.ARCHIVED, CanvasStatusEnum.PUBLISHED);
        assertRejected(CanvasStatusEnum.ARCHIVED, CanvasStatusEnum.OFFLINE);
    }

    private void assertAllowed(CanvasStatusEnum source, CanvasStatusEnum target) {
        assertThatCode(() -> policy.assertTransition(canvas(source), target))
                .as(source + " -> " + target)
                .doesNotThrowAnyException();
    }

    private void assertRejected(CanvasStatusEnum source, CanvasStatusEnum target) {
        assertThatThrownBy(() -> policy.assertTransition(canvas(source), target))
                .as(source + " -> " + target)
                .isInstanceOf(IllegalStateException.class);
    }

    private static CanvasDO canvas(CanvasStatusEnum status) {
        CanvasDO canvas = new CanvasDO();
        canvas.setStatus(status.getCode());
        return canvas;
    }
}
