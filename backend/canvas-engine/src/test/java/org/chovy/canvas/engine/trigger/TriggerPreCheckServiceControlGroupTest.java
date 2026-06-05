package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasUserQuotaMapper;
import org.chovy.canvas.domain.canvas.CanvasControlGroupService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriggerPreCheckServiceControlGroupTest {

    @Test
    void heldOutUserIsRejectedBeforeQuotaAccounting() {
        CanvasControlGroupService controlGroupService = mock(CanvasControlGroupService.class);
        TriggerPreCheckService service = new TriggerPreCheckService(
                mock(CanvasMapper.class),
                mock(CanvasUserQuotaMapper.class),
                mock(StringRedisTemplate.class));
        service.setControlGroupService(controlGroupService);
        CanvasDO canvas = new CanvasDO();
        canvas.setId(10L);
        canvas.setStatus(1);
        when(controlGroupService.isHeldOut(canvas, "user-1")).thenReturn(true);

        assertThatThrownBy(() -> service.checkWithoutQuotaAccounting(canvas, "user-1"))
                .isInstanceOf(TriggerPreCheckService.TriggerRejectedException.class)
                .extracting("code")
                .isEqualTo("CONTROL_001");

        verify(controlGroupService).recordHoldout(10L, "user-1", null, "CONTROL_GROUP");
    }
}
