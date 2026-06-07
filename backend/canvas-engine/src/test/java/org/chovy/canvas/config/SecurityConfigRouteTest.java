package org.chovy.canvas.config;

import org.junit.jupiter.api.Test;
import org.chovy.canvas.common.tenant.RoleNames;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigRouteTest {

    @Test
    void opsRequiresAdmin() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/ops/canvas/1/cache/invalidate").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void biEmbedTicketVerifyAllowsAnonymousRender() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/canvas/bi/embed-tickets/verify").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void biEmbedQueryExecuteAllowsAnonymousTicketBoundRender() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/canvas/bi/embed/query/execute").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void biEmbedDashboardResourceAllowsAnonymousTicketBoundMetadataLoad() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/canvas/bi/embed/resources/dashboard").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void biEmbedDashboardRuntimeStateAllowsAnonymousTicketBoundRuntimeReuse() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/canvas/bi/embed/resources/dashboard/runtime-state").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void biEmbedTicketCreationRequiresAuthentication() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/canvas/bi/embed-tickets").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void publicMarketingFormSubmitAllowsAnonymousLeadCapture() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/public/marketing-forms/signup/submit").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void publicConversationWebhookAllowsAnonymousProviderCallbackToReachController() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/public/conversation-webhooks/7/whatsapp").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void publicMarketingMonitoringWebhookAllowsAnonymousProviderCallbackToReachController() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST,
                        "/public/marketing-monitoring/webhooks/7/brandwatch").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void publicAssetUploadWebhookAllowsAnonymousProviderCallbackToReachController() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST,
                        "/public/marketing/content/assets/upload-callbacks/7/mux").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void contentOperatorCanCreateDraftsAndUploadIntents() {
        assertAllowedWithRole(HttpMethod.POST, "/marketing/content/templates", RoleNames.OPERATOR);
        assertAllowedWithRole(HttpMethod.POST, "/marketing/content/assets/upload-intents", RoleNames.OPERATOR);
        assertAllowedWithRole(HttpMethod.POST, "/message-templates", RoleNames.OPERATOR);
    }

    @Test
    void contentOperatorCannotPublishOrApproveContent() {
        assertDeniedWithRole(HttpMethod.POST, "/marketing/content/releases/publish", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/marketing/content/templates/welcome/status", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/marketing/content/assets/hero/status", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/marketing/content/assets/upload-intents/expire-stale", RoleNames.OPERATOR);
    }

    @Test
    void tenantAdminCanPublishAndApproveContent() {
        assertAllowedWithRole(HttpMethod.POST, "/marketing/content/releases/publish", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/marketing/content/templates/welcome/status", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/marketing/content/releases/template-welcome/rollback", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/marketing/content/assets/upload-intents/expire-stale", RoleNames.TENANT_ADMIN);
    }

    @Test
    void cdpTrackEndpointAllowsAnonymousSecurityFilterAccess() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/cdp/events/track").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void actuatorHealthAllowsAnonymousHealthChecks() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/actuator/health").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void realtimePipelineCheckpointAllowsInternalFilterToReachController() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/warehouse/realtime/pipelines/checkpoints").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private WebFilterChainProxy securityProxy() {
        JwtAuthFilter jwtAuthFilter = mock(JwtAuthFilter.class);
        when(jwtAuthFilter.filter(any(), any()))
                .thenAnswer(invocation -> {
                    WebFilterChain chain = invocation.getArgument(1);
                    return chain.filter(invocation.getArgument(0));
                });
        SecurityWebFilterChain chain = new SecurityConfig()
                .securityWebFilterChain(ServerHttpSecurity.http(), jwtAuthFilter, new InternalApiAuthFilter(""));
        return new WebFilterChainProxy(chain);
    }

    private void assertAllowedWithRole(HttpMethod method, String path, String role) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(method, path).build());

        StepVerifier.create(securityProxy().filter(exchange, ignored -> Mono.empty())
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication(role))))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private void assertDeniedWithRole(HttpMethod method, String path, String role) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(method, path).build());

        StepVerifier.create(securityProxy().filter(exchange, ignored -> Mono.empty())
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication(role))))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private UsernamePasswordAuthenticationToken authentication(String role) {
        return new UsernamePasswordAuthenticationToken(
                "alice",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
