package org.chovy.canvas.domain.bi.embed;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                300
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
        assertThat(payload.nonce()).isNotBlank();
    }

    @Test
    void capsTtlAndRejectsTamperedTicket() {
        BiEmbedTicketService service = new BiEmbedTicketService(SECRET, CLOCK);
        BiEmbedTicket ticket = service.createTicket(7L, "alice", new BiEmbedTicketRequest(
                "DASHBOARD",
                "canvas-effect",
                "EXTERNAL_TICKET",
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
}
