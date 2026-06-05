package org.chovy.canvas.web;

import org.chovy.canvas.domain.canvas.UserInputService;
import org.chovy.canvas.dto.canvas.UserInputSubmitReq;
import org.chovy.canvas.dto.canvas.UserInputSubmitResp;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserInputControllerTest {

    @Test
    void submitDelegatesToService() {
        UserInputService service = mock(UserInputService.class);
        UserInputSubmitReq req = new UserInputSubmitReq(Map.of("email", "a@example.com"), "alice");
        when(service.submit(12L, req)).thenReturn(
                new UserInputSubmitResp(12L, UserInputService.STATUS_COMPLETED, false));
        UserInputController controller = new UserInputController(service);

        StepVerifier.create(controller.submit(12L, req))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().status()).isEqualTo(UserInputService.STATUS_COMPLETED);
                })
                .verifyComplete();

        verify(service).submit(12L, req);
    }
}
