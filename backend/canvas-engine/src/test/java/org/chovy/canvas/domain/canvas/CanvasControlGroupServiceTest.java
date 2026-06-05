package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.dal.dataobject.CanvasControlGroupHoldoutDO;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasControlGroupHoldoutMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CanvasControlGroupServiceTest {

    private final CanvasControlGroupHoldoutMapper holdoutMapper = mock(CanvasControlGroupHoldoutMapper.class);
    private final CanvasControlGroupService service = new CanvasControlGroupService(holdoutMapper);

    @Test
    void sameCanvasUserAndSaltProduceStableDecision() {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(9L);
        canvas.setControlGroupPercent(10);
        canvas.setControlGroupSalt("salt-a");

        boolean first = service.isHeldOut(canvas, "user-1");
        boolean second = service.isHeldOut(canvas, "user-1");

        assertThat(second).isEqualTo(first);
    }

    @Test
    void zeroPercentNeverHoldsOut() {
        CanvasDO canvas = new CanvasDO();
        canvas.setControlGroupPercent(0);

        assertThat(service.isHeldOut(canvas, "user-1")).isFalse();
    }

    @Test
    void percentAboveFiftyIsRejected() {
        CanvasDO canvas = new CanvasDO();
        canvas.setControlGroupPercent(51);

        assertThatThrownBy(() -> service.isHeldOut(canvas, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("controlGroupPercent");
    }

    @Test
    void recordHoldoutPersistsAuditRowAndIgnoresDuplicates() {
        service.recordHoldout(10L, "user-1", "evt-1", "CONTROL_GROUP");

        ArgumentCaptor<CanvasControlGroupHoldoutDO> captor =
                ArgumentCaptor.forClass(CanvasControlGroupHoldoutDO.class);
        verify(holdoutMapper).insert(captor.capture());
        assertThat(captor.getValue().getCanvasId()).isEqualTo(10L);
        assertThat(captor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(captor.getValue().getEventId()).isEqualTo("evt-1");
        assertThat(captor.getValue().getReason()).isEqualTo("CONTROL_GROUP");

        doThrow(new DuplicateKeyException("duplicate")).when(holdoutMapper)
                .insert((CanvasControlGroupHoldoutDO) any(CanvasControlGroupHoldoutDO.class));
        assertThatCode(() -> service.recordHoldout(10L, "user-1", "evt-1", "CONTROL_GROUP"))
                .doesNotThrowAnyException();
    }
}
