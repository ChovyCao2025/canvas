package org.chovy.canvas.web.config;

import org.chovy.canvas.platform.api.AuthFacade;
import org.chovy.canvas.web.platform.AuthController;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = CanvasWebSecurityConfigTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
class CanvasWebSecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AuthFacade authFacade;

    @Test
    void loginDoesNotRequireCsrfToken() {
        when(authFacade.login(ArgumentMatchers.any()))
                .thenReturn(new AuthFacade.LoginView(
                        "auth-token-1-admin", 1L, 0L, "admin", "Administrator", "ADMIN"));

        webTestClient.post()
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
                .jsonPath("$.data.token").isEqualTo("auth-token-1-admin");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            RedisReactiveAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class
    })
    @ImportAutoConfiguration({
            ReactiveSecurityAutoConfiguration.class,
            ReactiveUserDetailsServiceAutoConfiguration.class,
            ReactiveOAuth2ClientAutoConfiguration.class,
            ReactiveOAuth2ResourceServerAutoConfiguration.class
    })
    @ComponentScan(basePackages = "org.chovy.canvas.web.config")
    static class TestApplication {

        @Bean
        AuthController authController(AuthFacade authFacade) {
            return new AuthController(authFacade);
        }
    }
}
