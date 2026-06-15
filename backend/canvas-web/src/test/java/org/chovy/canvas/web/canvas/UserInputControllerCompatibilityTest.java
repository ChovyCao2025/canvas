package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.canvas.api.UserInputFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class UserInputControllerCompatibilityTest {

    @Test
    void submitRouteUsesPathResponseIdBodyResponseMapAndLegacySuccessEnvelope() {
        RecordingUserInputFacade facade = new RecordingUserInputFacade();

        webClient(facade)
                .post()
                .uri("/user-input/responses/12/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "response": {
                            "email": "a@example.com",
                            "profile": {
                              "tier": "gold"
                            }
                          },
                          "operator": "alice"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.responseId").isEqualTo(12)
                .jsonPath("$.data.status").isEqualTo("COMPLETED")
                .jsonPath("$.data.duplicate").isEqualTo(false);

        assertThat(facade.lastResponseId).isEqualTo(12L);
        assertThat(facade.lastCommand.operator()).isEqualTo("alice");
        assertThat(facade.lastCommand.response()).containsEntry("email", "a@example.com");
        assertThat(facade.lastCommand.response()).containsKey("profile");
        assertThat(facade.lastCommand.response().get("profile"))
                .isInstanceOfSatisfying(Map.class, profile -> assertThat(profile).containsEntry("tier", "gold"));
    }

    @Test
    void submitRouteMapsIllegalArgumentExceptionToApi001BadRequestEnvelope() {
        RecordingUserInputFacade facade = new RecordingUserInputFacade();
        facade.failSubmit = true;

        webClient(facade)
                .post()
                .uri("/user-input/responses/99/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "response": {
                            "email": "bad"
                          },
                          "operator": "alice"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("User input response not found: 99")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        assertThat(facade.lastResponseId).isEqualTo(99L);
        assertThat(facade.lastCommand.response()).containsEntry("email", "bad");
    }

    private static WebTestClient webClient(UserInputFacade facade) {
        return WebTestClient.bindToController(new UserInputController(facade)).build();
    }

    private static final class RecordingUserInputFacade implements UserInputFacade {
        private Long lastResponseId;
        private SubmitCommand lastCommand;
        private boolean failSubmit;

        @Override
        public SubmitResult submit(Long responseId, SubmitCommand command) {
            lastResponseId = responseId;
            lastCommand = new SubmitCommand(new LinkedHashMap<>(command.response()), command.operator());
            if (failSubmit) {
                throw new IllegalArgumentException("User input response not found: " + responseId);
            }
            return new SubmitResult(responseId, "COMPLETED", false);
        }
    }
}
