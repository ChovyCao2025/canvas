package org.chovy.canvas.config;

import org.junit.jupiter.api.Test;
import org.chovy.canvas.common.tenant.RoleNames;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.core.env.Environment;
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
    void biEmbedPortalResourceAllowsAnonymousTicketBoundMetadataLoad() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/canvas/bi/embed/resources/portal").build());
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
    void highImpactControlPlaneWritesDenyOperator() {
        assertDeniedWithRole(HttpMethod.POST, "/canvas/bi/permissions/resources", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/cdp/write-keys", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/cdp/webhooks/sub-1/test", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/warehouse/backfill", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/execution-requests/17/replay", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/ai/providers", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/channels/connectors/2/mode", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/search-marketing/mutations/3/approve", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/search-marketing/mutations/3/execute", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST,
                "/canvas/creator-collaboration/mutations/3/approve", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST,
                "/canvas/creator-collaboration/mutations/3/execute", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/programmatic-dsp/mutations/3/approve", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/programmatic-dsp/mutations/3/execute", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST,
                "/canvas/marketing-monitoring/provider-credentials", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST,
                "/canvas/marketing-monitoring/provider-credentials/brandwatch/refresh", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/paid-media/audience-sync/runs", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/loyalty/users/user-1/redeem", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/growth-activities", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/growth-activities/11/publish", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/ab-experiments", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.PUT, "/canvas/ab-experiments/17", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.DELETE, "/canvas/ab-experiments/17", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/ab-experiments/17/groups", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.PUT, "/canvas/ab-experiments/17/groups/3", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.DELETE, "/canvas/ab-experiments/17/groups/3", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST,
                "/canvas/ab-experiments/17/governance/evaluate", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/policies/consent", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/policies/suppression", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/policies/channel", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST, "/canvas/marketing-integrations/contracts", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.DELETE, "/canvas/marketing-integrations/contracts/42", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST,
                "/canvas/marketing-integrations/contracts/42/probe-runs", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST,
                "/canvas/marketing-integrations/contracts/42/probes", RoleNames.OPERATOR);
        assertDeniedWithRole(HttpMethod.POST,
                "/canvas/marketing-integrations/contract-probe-runs/scan", RoleNames.OPERATOR);
    }

    @Test
    void highImpactControlPlaneWritesAllowTenantAdmin() {
        assertAllowedWithRole(HttpMethod.POST, "/canvas/bi/permissions/resources", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/cdp/write-keys", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/cdp/webhooks/sub-1/test", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/warehouse/backfill", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/execution-requests/17/replay", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/ai/providers", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/channels/connectors/2/mode", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/search-marketing/mutations/3/approve", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/search-marketing/mutations/3/execute", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST,
                "/canvas/creator-collaboration/mutations/3/approve", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST,
                "/canvas/creator-collaboration/mutations/3/execute", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/programmatic-dsp/mutations/3/approve", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/programmatic-dsp/mutations/3/execute", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST,
                "/canvas/marketing-monitoring/provider-credentials", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST,
                "/canvas/marketing-monitoring/provider-credentials/brandwatch/refresh", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/paid-media/audience-sync/runs", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/loyalty/users/user-1/redeem", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/growth-activities", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/growth-activities/11/publish", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/ab-experiments", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.PUT, "/canvas/ab-experiments/17", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.DELETE, "/canvas/ab-experiments/17", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/ab-experiments/17/groups", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.PUT, "/canvas/ab-experiments/17/groups/3", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.DELETE, "/canvas/ab-experiments/17/groups/3", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST,
                "/canvas/ab-experiments/17/governance/evaluate", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/policies/consent", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/policies/suppression", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/policies/channel", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/canvas/marketing-integrations/contracts", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.DELETE, "/canvas/marketing-integrations/contracts/42", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST,
                "/canvas/marketing-integrations/contracts/42/probe-runs", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST,
                "/canvas/marketing-integrations/contracts/42/probes", RoleNames.TENANT_ADMIN);
        assertAllowedWithRole(HttpMethod.POST,
                "/canvas/marketing-integrations/contract-probe-runs/scan", RoleNames.TENANT_ADMIN);
    }

    @Test
    void selfServiceBiPermissionRequestStillUsesAuthenticatedFallback() {
        assertAllowedWithRole(HttpMethod.POST, "/canvas/bi/permissions/requests", RoleNames.OPERATOR);
    }

    @Test
    void controlPlaneReadRoutesStillUseAuthenticatedFallback() {
        assertAllowedWithRole(HttpMethod.GET, "/cdp/write-keys", RoleNames.OPERATOR);
        assertAllowedWithRole(HttpMethod.GET, "/canvas/growth-activities", RoleNames.OPERATOR);
        assertAllowedWithRole(HttpMethod.GET, "/warehouse/status", RoleNames.OPERATOR);
        assertAllowedWithRole(HttpMethod.GET, "/canvas/ab-experiments", RoleNames.OPERATOR);
        assertAllowedWithRole(HttpMethod.GET, "/canvas/ab-experiments/17/groups", RoleNames.OPERATOR);
        assertAllowedWithRole(HttpMethod.GET, "/canvas/policies/state", RoleNames.OPERATOR);
        assertAllowedWithRole(HttpMethod.GET, "/canvas/marketing-integrations/contracts", RoleNames.OPERATOR);
        assertAllowedWithRole(HttpMethod.GET,
                "/canvas/marketing-integrations/contracts/42/audit-events", RoleNames.OPERATOR);
        assertAllowedWithRole(HttpMethod.GET,
                "/canvas/marketing-integrations/contracts/42/probes", RoleNames.OPERATOR);
        assertAllowedWithRole(HttpMethod.GET,
                "/canvas/marketing-integrations/contract-probe-runs", RoleNames.OPERATOR);
    }

    @Test
    void tenantAdminCannotAccessTenantAdministrationRoutes() {
        assertDeniedWithRole(HttpMethod.GET, "/admin/tenants", RoleNames.TENANT_ADMIN);
        assertDeniedWithRole(HttpMethod.POST, "/admin/tenants", RoleNames.TENANT_ADMIN);
        assertDeniedWithRole(HttpMethod.GET, "/admin/tenants/17", RoleNames.TENANT_ADMIN);
        assertDeniedWithRole(HttpMethod.POST, "/admin/tenants/17/suspend", RoleNames.TENANT_ADMIN);

        assertAllowedWithRole(HttpMethod.GET, "/admin/tenants", RoleNames.SUPER_ADMIN);
        assertAllowedWithRole(HttpMethod.POST, "/admin/tenants/17/suspend", RoleNames.SUPER_ADMIN);
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
    void openApiJsonAllowsAnonymousDocumentationLoads() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/v3/api-docs").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void openApiJsonRequiresAuthenticationInProductionLikeProfiles() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/v3/api-docs").build());
        WebFilterChainProxy security = securityProxy(new MockEnvironment()
                .withProperty("spring.profiles.active", "prod"));

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
        return securityProxy(new MockEnvironment()
                .withProperty("spring.profiles.active", "local"));
    }

    private WebFilterChainProxy securityProxy(Environment environment) {
        JwtAuthFilter jwtAuthFilter = mock(JwtAuthFilter.class);
        when(jwtAuthFilter.filter(any(), any()))
                .thenAnswer(invocation -> {
                    WebFilterChain chain = invocation.getArgument(1);
                    return chain.filter(invocation.getArgument(0));
        });
        SecurityWebFilterChain chain = new SecurityConfig()
                .securityWebFilterChain(ServerHttpSecurity.http(), jwtAuthFilter, new InternalApiAuthFilter(""),
                        environment);
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
