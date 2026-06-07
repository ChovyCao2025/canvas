package org.chovy.canvas.domain.bi.embed;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiEmbedTokenDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiEmbedTokenMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiEmbedTicketServiceTest {

    private static final String SECRET = "bi-embed-test-secret-with-at-least-32-bytes";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-05T01:50:00Z"), ZoneOffset.UTC);

    @Test
    void createsShortLivedTicketBoundToTenantUserResourceAndFilters() {
        BiEmbedTicketService service = new BiEmbedTicketService(SECRET, CLOCK);
        BiEmbedTicketRequest request = new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "INTERNAL_CANVAS",
                Map.of("canvasId", "12", "statDate", "LAST_7_DAYS"),
                300,
                List.of(),
                Map.of("gpCanvas", "Welcome Journey", "gpTriggerType", "TIME,MQ")
        );

        BiEmbedTicket ticket = service.createTicket(7L, "alice", request);
        BiEmbedTicketPayload payload = service.verify(ticket.ticket());

        assertThat(ticket.embedUrl()).isEqualTo("/bi/embed/DASHBOARD/canvas-effect?ticket=" + ticket.ticket());
        assertThat(ticket.expiresAt()).isEqualTo(Instant.parse("2026-06-05T01:55:00Z"));
        assertThat(payload.tenantId()).isEqualTo(7L);
        assertThat(payload.username()).isEqualTo("alice");
        assertThat(payload.resourceType()).isEqualTo("DASHBOARD");
        assertThat(payload.resourceKey()).isEqualTo("canvas-effect");
        assertThat(payload.scope()).isEqualTo("INTERNAL_CANVAS");
        assertThat(payload.filters()).containsEntry("canvasId", "12");
        assertThat(payload.parameters()).containsEntry("gpCanvas", "Welcome Journey");
        assertThat(payload.parameters()).containsEntry("gpTriggerType", "TIME,MQ");
        assertThat(payload.nonce()).isNotBlank();
    }

    @Test
    void capsTtlAndRejectsTamperedTicket() {
        BiEmbedTicketService service = new BiEmbedTicketService(SECRET, CLOCK);
        BiEmbedTicket ticket = service.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "INTERNAL_CANVAS",
                Map.of(),
                999999
        ));

        assertThat(ticket.expiresAt()).isEqualTo(Instant.parse("2026-06-05T02:20:00Z"));
        assertThatThrownBy(() -> service.verify(ticket.ticket() + "x"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("invalid BI embed ticket");
    }

    @Test
    void rejectsWeakSecretAndUnsafeRequest() {
        assertThatThrownBy(() -> new BiEmbedTicketService("weak", CLOCK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");

        BiEmbedTicketService service = new BiEmbedTicketService(SECRET, CLOCK);
        assertThatThrownBy(() -> service.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "../unsafe",
                "INTERNAL_CANVAS",
                Map.of(),
                (int) Duration.ofMinutes(5).toSeconds()
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resourceKey");
    }

    @Test
    void rejectsOversizedAndControlCharacterFiltersBeforeSigning() {
        BiEmbedTicketService service = new BiEmbedTicketService(SECRET, CLOCK);
        Map<String, String> tooManyFilters = new HashMap<>();
        for (int index = 0; index < 17; index++) {
            tooManyFilters.put("filter-" + index, "value-" + index);
        }

        assertThatThrownBy(() -> service.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                tooManyFilters,
                300
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too many filters");

        assertThatThrownBy(() -> service.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of("filter-canvas", "Welcome\nJourney"),
                300
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("filter value contains control characters");

        assertThatThrownBy(() -> service.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of(),
                300,
                List.of("reports.example.com"),
                Map.of("gpCanvas", "Welcome\nJourney")
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parameter value contains control characters");
    }

    @Test
    void externalTicketsRequireAllowedDomainsBeforeSigning() {
        BiEmbedTicketService service = new BiEmbedTicketService(SECRET, CLOCK);

        assertThatThrownBy(() -> service.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of(),
                300
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allowedDomains are required");
    }

    @Test
    void consumingVerificationEnforcesAllowedDomainAndRejectsReplay() {
        BiEmbedTicketService service = new BiEmbedTicketService(SECRET, CLOCK);
        BiEmbedTicket allowedTicket = service.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of("canvasId", "12"),
                300,
                List.of("reports.example.com", "https://ops.example.com:8443")
        ));
        BiEmbedTicket deniedTicket = service.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of(),
                300,
                List.of("reports.example.com")
        ));

        assertThat(service.verifyForUse(allowedTicket.ticket(), "https://reports.example.com").allowedDomains())
                .containsExactly("reports.example.com", "ops.example.com:8443");
        assertThatThrownBy(() -> service.verifyForUse(allowedTicket.ticket(), "https://reports.example.com"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("replayed BI embed ticket");
        assertThatThrownBy(() -> service.verifyForUse(deniedTicket.ticket(), "https://evil.example.com"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("origin is not allowed");
    }

    @Test
    void createsPersistentTokenRowWhenTokenMapperIsConfigured() throws Exception {
        BiEmbedTokenMapper tokenMapper = mock(BiEmbedTokenMapper.class);
        BiEmbedTicketService service = new BiEmbedTicketService(SECRET, CLOCK, tokenMapper, mock(BiAuditLogMapper.class));

        BiEmbedTicket ticket = service.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of("canvasId", "12"),
                300,
                List.of("https://reports.example.com")
        ));

        ArgumentCaptor<BiEmbedTokenDO> stored = ArgumentCaptor.forClass(BiEmbedTokenDO.class);
        verify(tokenMapper).insert(stored.capture());
        assertThat(stored.getValue().getTenantId()).isEqualTo(7L);
        assertThat(stored.getValue().getTokenHash()).isNotBlank();
        assertThat(stored.getValue().getTokenHash()).doesNotContain(ticket.ticket());
        assertThat(stored.getValue().getResourceType()).isEqualTo("DASHBOARD");
        assertThat(stored.getValue().getResourceKey()).isEqualTo("canvas-effect");
        assertThat(stored.getValue().getResourceId()).isEqualTo(0L);
        assertThat(stored.getValue().getUserId()).isEqualTo("alice");
        assertThat(stored.getValue().getNonce()).isEqualTo(service.verify(ticket.ticket()).nonce());
        assertThat(stored.getValue().getRevoked()).isFalse();
        assertThat(stored.getValue().getExpiresAt()).isEqualTo(java.time.LocalDateTime.of(2026, 6, 5, 1, 55));
        JsonNode scope = new ObjectMapper().readTree(stored.getValue().getScopeJson());
        assertThat(scope.path("filters").path("canvasId").asText()).isEqualTo("12");
        assertThat(scope.path("allowedDomains").get(0).asText()).isEqualTo("reports.example.com");
    }

    @Test
    void persistentTokenRowsCarrySignedAccessAndRateLimits() {
        BiEmbedTokenMapper tokenMapper = mock(BiEmbedTokenMapper.class);
        BiEmbedTicketService service = new BiEmbedTicketService(SECRET, CLOCK, tokenMapper, mock(BiAuditLogMapper.class));

        BiEmbedTicket ticket = service.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of(),
                300,
                List.of("reports.example.com"),
                3,
                12
        ));

        ArgumentCaptor<BiEmbedTokenDO> stored = ArgumentCaptor.forClass(BiEmbedTokenDO.class);
        verify(tokenMapper).insert(stored.capture());
        assertThat(stored.getValue().getAccessCount()).isZero();
        assertThat(stored.getValue().getMaxAccessCount()).isEqualTo(3);
        assertThat(stored.getValue().getRateLimitPerMinute()).isEqualTo(12);
        assertThat(stored.getValue().getRateWindowCount()).isZero();
        assertThat(stored.getValue().getRateWindowStartedAt()).isNull();
        assertThat(stored.getValue().getLastAccessedAt()).isNull();
        assertThat(stored.getValue().getLastAccessOrigin()).isNull();
        BiEmbedTicketPayload payload = service.verify(ticket.ticket());
        assertThat(payload.maxAccessCount()).isEqualTo(3);
        assertThat(payload.rateLimitPerMinute()).isEqualTo(12);
    }

    @Test
    void consumingPersistentTicketAtomicallyMarksUsedAndAuditsOrigin() throws Exception {
        BiEmbedTokenMapper tokenMapper = mock(BiEmbedTokenMapper.class);
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        when(tokenMapper.update(isNull(), any())).thenReturn(1);
        BiEmbedTicketService service = new BiEmbedTicketService(SECRET, CLOCK, tokenMapper, auditLogMapper);
        BiEmbedTicket ticket = service.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of(),
                300,
                List.of("reports.example.com")
        ));

        BiEmbedTicketPayload payload = service.verifyForUse(ticket.ticket(), "https://reports.example.com");

        assertThat(payload.resourceKey()).isEqualTo("canvas-effect");
        verify(tokenMapper).update(isNull(), any());
        ArgumentCaptor<BiAuditLogDO> audit = ArgumentCaptor.forClass(BiAuditLogDO.class);
        verify(auditLogMapper).insert(audit.capture());
        assertThat(audit.getValue().getTenantId()).isEqualTo(7L);
        assertThat(audit.getValue().getActorId()).isEqualTo("alice");
        assertThat(audit.getValue().getActionKey()).isEqualTo("BI_EMBED_TICKET_CONSUME");
        assertThat(audit.getValue().getResourceType()).isEqualTo("DASHBOARD");
        JsonNode detail = new ObjectMapper().readTree(audit.getValue().getDetailJson());
        assertThat(detail.path("resourceKey").asText()).isEqualTo("canvas-effect");
        assertThat(detail.path("origin").asText()).isEqualTo("reports.example.com");
        assertThat(detail.path("nonce").asText()).isEqualTo(payload.nonce());
        assertThat(detail.path("status").asText()).isEqualTo("CONSUMED");
    }

    @Test
    @SuppressWarnings("unchecked")
    void consumingPersistentTicketAtomicallyEnforcesAccessCountAndRateWindow() {
        BiEmbedTokenMapper tokenMapper = mock(BiEmbedTokenMapper.class);
        when(tokenMapper.update(isNull(), any())).thenReturn(1);
        BiEmbedTicketService service = new BiEmbedTicketService(SECRET, CLOCK, tokenMapper, mock(BiAuditLogMapper.class));
        BiEmbedTicket ticket = service.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of(),
                300,
                List.of("reports.example.com"),
                3,
                12
        ));

        service.verifyForUse(ticket.ticket(), "https://reports.example.com");

        ArgumentCaptor<UpdateWrapper<BiEmbedTokenDO>> update = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(tokenMapper).update(isNull(), update.capture());
        assertThat(update.getValue().getSqlSegment())
                .contains("access_count < max_access_count")
                .contains("rate_window_count < rate_limit_per_minute");
        assertThat(update.getValue().getSqlSet())
                .contains("access_count = access_count + 1")
                .contains("rate_window_count = CASE")
                .contains("rate_window_started_at = CASE")
                .contains("last_accessed_at")
                .contains("last_access_origin")
                .contains("revoked = CASE");
    }

    @Test
    void persistentReplayRejectionAuditsRejectedUse() {
        BiEmbedTokenMapper tokenMapper = mock(BiEmbedTokenMapper.class);
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        when(tokenMapper.update(isNull(), any())).thenReturn(0);
        BiEmbedTicketService service = new BiEmbedTicketService(SECRET, CLOCK, tokenMapper, auditLogMapper);
        BiEmbedTicket ticket = service.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
                Map.of(),
                300,
                List.of("reports.example.com")
        ));

        assertThatThrownBy(() -> service.verifyForUse(ticket.ticket(), "https://reports.example.com"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("replayed BI embed ticket");

        ArgumentCaptor<BiAuditLogDO> audit = ArgumentCaptor.forClass(BiAuditLogDO.class);
        verify(auditLogMapper).insert(audit.capture());
        assertThat(audit.getValue().getActionKey()).isEqualTo("BI_EMBED_TICKET_REJECTED");
        assertThat(audit.getValue().getDetailJson()).contains("REPLAY_REJECTED");
    }

    @Test
    void cleanupExpiredTokensRevokesTenantRowsAndCapsLimit() {
        BiEmbedTokenMapper tokenMapper = mock(BiEmbedTokenMapper.class);
        BiEmbedTokenDO expired = new BiEmbedTokenDO();
        expired.setId(41L);
        expired.setTenantId(7L);
        expired.setTokenHash("hash-41");
        expired.setResourceType("DASHBOARD");
        expired.setResourceKey("canvas-effect");
        expired.setResourceId(0L);
        expired.setUserId("alice");
        expired.setScopeJson("{}");
        expired.setNonce("nonce-41");
        expired.setExpiresAt(java.time.LocalDateTime.of(2026, 6, 5, 1, 49));
        expired.setRevoked(false);
        when(tokenMapper.selectList(any())).thenReturn(List.of(expired));
        BiEmbedTicketService service = new BiEmbedTicketService(SECRET, CLOCK, tokenMapper, mock(BiAuditLogMapper.class));

        BiEmbedTokenCleanupResult result = service.cleanupExpiredTokens(7L, 9999);

        assertThat(result.checked()).isEqualTo(1);
        assertThat(result.revoked()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        ArgumentCaptor<BiEmbedTokenDO> updated = ArgumentCaptor.forClass(BiEmbedTokenDO.class);
        verify(tokenMapper).updateById(updated.capture());
        assertThat(updated.getValue().getId()).isEqualTo(41L);
        assertThat(updated.getValue().getRevoked()).isTrue();
    }
}
