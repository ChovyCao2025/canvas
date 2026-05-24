package org.chovy.canvas.web;

import org.chovy.canvas.domain.cdp.CanvasUserQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class CanvasUserControllerTest {

    @Test
    void listReturnsCanvasUsers() {
        CanvasUserQueryService service = Mockito.mock(CanvasUserQueryService.class);
        CanvasUserController controller = new CanvasUserController(service);
        when(service.listUsers(7L)).thenReturn(List.of());

        assertThat(controller.list(7L).block().getData()).isEmpty();
    }
}
