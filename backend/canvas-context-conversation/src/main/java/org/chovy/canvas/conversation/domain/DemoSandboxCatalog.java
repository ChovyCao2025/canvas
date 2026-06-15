package org.chovy.canvas.conversation.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.conversation.api.DemoSandboxFacade.ConversationReplyCommand;
import org.chovy.canvas.conversation.api.DemoSandboxFacade.ConversationReplyResult;
import org.chovy.canvas.conversation.api.DemoSandboxFacade.InstallCommand;
import org.chovy.canvas.conversation.api.DemoSandboxFacade.ResetResult;
import org.chovy.canvas.conversation.api.DemoSandboxFacade.SandboxView;

public class DemoSandboxCatalog {

    private static final String SANDBOX = "SANDBOX";

    private final Clock clock;
    private final Map<Long, SandboxRow> sandboxes = new LinkedHashMap<>();
    private final Map<Long, ConversationReplyResult> replies = new LinkedHashMap<>();
    private long sandboxIds;
    private long replyIds;

    public DemoSandboxCatalog(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public synchronized SandboxView install(InstallCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("install request is required");
        }
        Long tenantId = requireTenant(command.tenantId());
        LocalDateTime now = now();
        LocalDateTime expiresAt = now.plusDays(command.ttlDays());
        String status = command.ttlDays() <= 0 ? "EXPIRED" : "ACTIVE";
        SandboxRow row = new SandboxRow(++sandboxIds, tenantId, defaultText(command.demoName(), SANDBOX),
                command.ttlDays(), status, actorOrSystem(actor), now, expiresAt);
        sandboxes.put(tenantId, row);
        return view(row);
    }

    public synchronized ResetResult reset(Long tenantId, String actor) {
        Long scopedTenantId = requireTenant(tenantId);
        SandboxRow row = sandboxes.get(scopedTenantId);
        if (row != null) {
            row.status = "RESET";
        }
        return new ResetResult(scopedTenantId, "RESET", actorOrSystem(actor), now());
    }

    public synchronized List<SandboxView> expired() {
        LocalDateTime now = now();
        return sandboxes.values().stream()
                .filter(row -> "EXPIRED".equals(row.status) || !row.expiresAt.isAfter(now))
                .map(DemoSandboxCatalog::view)
                .toList();
    }

    public synchronized ConversationReplyResult reply(Long tenantId, ConversationReplyCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("conversation reply request is required");
        }
        Long scopedTenantId = requireTenant(tenantId);
        String executionId = requireText(command.executionId(), "executionId");
        String userId = requireText(command.userId(), "userId");
        ConversationReplyResult result = new ConversationReplyResult(++replyIds, scopedTenantId, SANDBOX,
                command.canvasId(), command.versionId(), executionId, userId, optional(command.externalMessageId()),
                optional(command.eventId()), optional(command.text()), optional(command.intent()),
                command.attributes() == null ? Map.of() : Map.copyOf(command.attributes()), actorOrSystem(actor),
                now());
        replies.put(result.id(), result);
        return result;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static Long requireTenant(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        return tenantId;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String actorOrSystem(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static SandboxView view(SandboxRow row) {
        return new SandboxView(row.id, row.tenantId, row.demoName, row.ttlDays, row.status, row.installedBy,
                row.installedAt, row.expiresAt);
    }

    private static final class SandboxRow {
        private final Long id;
        private final Long tenantId;
        private final String demoName;
        private final int ttlDays;
        private String status;
        private final String installedBy;
        private final LocalDateTime installedAt;
        private final LocalDateTime expiresAt;

        private SandboxRow(Long id, Long tenantId, String demoName, int ttlDays, String status, String installedBy,
                LocalDateTime installedAt, LocalDateTime expiresAt) {
            this.id = id;
            this.tenantId = tenantId;
            this.demoName = demoName;
            this.ttlDays = ttlDays;
            this.status = status;
            this.installedBy = installedBy;
            this.installedAt = installedAt;
            this.expiresAt = expiresAt;
        }
    }
}
