package org.chovy.canvas.web.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.platform.api.AuthFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class AuthControllerCompatibilityTest {

    @Test
    void mapsLegacyAuthRoutesToFacadeWithCompatibilityEnvelope() {
        RecordingAuthFacade facade = new RecordingAuthFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"admin","password":"secret"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.token").isEqualTo("auth-token-1-admin")
                .jsonPath("$.data.userId").isEqualTo(1)
                .jsonPath("$.data.username").isEqualTo("admin");

        client.post()
                .uri("/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer auth-token-1-admin")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.revoked").isEqualTo(true);

        client.get()
                .uri("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer auth-token-1-admin")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.displayName").isEqualTo("Administrator");

        assertThat(facade.operations).containsExactly("login", "logout", "me");
        assertThat(facade.lastLoginCommand.username()).isEqualTo("admin");
        assertThat(facade.lastLoginCommand.password()).isEqualTo("secret");
        assertThat(facade.lastAuthorizationHeader).isEqualTo("Bearer auth-token-1-admin");
    }

    @Test
    void missingLoginBodyUsesEmptyCommandAndDelegatesValidationToFacade() {
        RecordingAuthFacade facade = new RecordingAuthFacade();
        facade.failLogin = true;

        webClient(facade).post()
                .uri("/auth/login")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("username is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        assertThat(facade.lastLoginCommand.username()).isNull();
        assertThat(facade.lastLoginCommand.password()).isNull();
    }

    @Test
    void meForwardsAuthorizationFailureAsApi001Envelope() {
        RecordingAuthFacade facade = new RecordingAuthFacade();
        facade.failMe = true;

        webClient(facade).get()
                .uri("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer revoked")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("token has been revoked");
    }

    private static WebTestClient webClient(AuthFacade facade) {
        return WebTestClient.bindToController(new AuthController(facade)).build();
    }

    private static final class RecordingAuthFacade implements AuthFacade {
        private final List<String> operations = new ArrayList<>();
        private LoginCommand lastLoginCommand;
        private String lastAuthorizationHeader;
        private boolean failLogin;
        private boolean failMe;

        @Override
        public LoginView login(LoginCommand command) {
            operations.add("login");
            lastLoginCommand = command;
            if (failLogin) {
                throw new IllegalArgumentException("username is required");
            }
            return loginView(command.username());
        }

        @Override
        public LogoutView logout(String authorizationHeader) {
            operations.add("logout");
            lastAuthorizationHeader = authorizationHeader;
            return new LogoutView(true, "1234567890abcdef1234567890abcdef");
        }

        @Override
        public LoginView me(String authorizationHeader) {
            operations.add("me");
            lastAuthorizationHeader = authorizationHeader;
            if (failMe) {
                throw new IllegalArgumentException("token has been revoked");
            }
            return loginView("admin");
        }

        private static LoginView loginView(String username) {
            return new LoginView("auth-token-1-admin", 1L, 0L, username, "Administrator", "ADMIN");
        }
    }
}
