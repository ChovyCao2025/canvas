package org.chovy.canvas.canvas.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.chovy.canvas.canvas.api.EventDefinitionFacade.EventDefinitionCommand;
import org.chovy.canvas.canvas.api.EventDefinitionFacade.EventDefinitionListQuery;
import org.chovy.canvas.canvas.api.EventDefinitionFacade.EventDefinitionView;
import org.chovy.canvas.canvas.api.EventDefinitionFacade.PageView;

public class EventDefinitionCatalog {

    private static final String DEFAULT_ATTRIBUTES = "[]";
    private static final String DEFAULT_DISCOVERY_MODE = "REJECT_UNKNOWN";

    private final AtomicLong ids = new AtomicLong(1);
    private final Map<Long, EventRow> rows = new LinkedHashMap<>();
    private final Consumer<String> eventCodeInvalidator;

    public EventDefinitionCatalog() {
        this(eventCode -> {
        });
    }

    public EventDefinitionCatalog(Consumer<String> eventCodeInvalidator) {
        this.eventCodeInvalidator = eventCodeInvalidator == null ? eventCode -> {
        } : eventCodeInvalidator;
    }

    public synchronized PageView<EventDefinitionView> list(EventDefinitionListQuery query) {
        int page = query.page() <= 0 ? 1 : query.page();
        int size = query.size() <= 0 ? 20 : query.size();
        List<EventDefinitionView> filtered = rows.values().stream()
                .filter(row -> query.enabled() == null || Objects.equals(row.enabled, query.enabled()))
                .sorted(Comparator.comparing(row -> row.id))
                .map(EventDefinitionCatalog::view)
                .toList();
        int from = Math.min((page - 1) * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return new PageView<>(filtered.size(), filtered.subList(from, to));
    }

    public synchronized EventDefinitionView create(EventDefinitionCommand command) {
        EventDefinitionCommand safe = requireCommand(command);
        requireEventCode(safe.eventCode());
        EventRow row = new EventRow();
        LocalDateTime now = LocalDateTime.now();
        row.id = ids.getAndIncrement();
        row.name = safe.name();
        row.eventCode = safe.eventCode();
        row.attributes = defaultString(safe.attributes(), DEFAULT_ATTRIBUTES);
        row.description = safe.description();
        row.autoDiscover = safe.autoDiscover() == null ? 0 : safe.autoDiscover();
        row.discoveryMode = defaultString(safe.discoveryMode(), DEFAULT_DISCOVERY_MODE);
        row.enabled = safe.enabled() == null ? 1 : safe.enabled();
        row.createdBy = safe.createdBy();
        row.createdAt = now;
        row.updatedAt = now;
        rows.put(row.id, row);
        invalidateEventCode(row.eventCode);
        return view(row);
    }

    public synchronized EventDefinitionView update(Long id, EventDefinitionCommand command) {
        EventRow row = requireExisting(id);
        EventDefinitionCommand safe = requireCommand(command);
        String oldEventCode = row.eventCode;
        if (safe.name() != null) {
            row.name = safe.name();
        }
        if (safe.eventCode() != null) {
            requireEventCode(safe.eventCode());
            row.eventCode = safe.eventCode();
        }
        if (safe.attributes() != null) {
            row.attributes = safe.attributes();
        }
        if (safe.description() != null) {
            row.description = safe.description();
        }
        if (safe.autoDiscover() != null) {
            row.autoDiscover = safe.autoDiscover();
        }
        if (safe.discoveryMode() != null) {
            row.discoveryMode = safe.discoveryMode();
        }
        if (safe.enabled() != null) {
            row.enabled = safe.enabled();
        }
        if (safe.createdBy() != null) {
            row.createdBy = safe.createdBy();
        }
        row.updatedAt = LocalDateTime.now();
        invalidateEventCode(oldEventCode);
        invalidateEventCode(row.eventCode);
        return view(row);
    }

    public synchronized void delete(Long id) {
        EventRow existing = requireExisting(id);
        rows.remove(existing.id);
        invalidateEventCode(existing.eventCode);
    }

    private EventRow requireExisting(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("event definition not found: " + id);
        }
        EventRow row = rows.get(id);
        if (row == null) {
            throw new IllegalArgumentException("event definition not found: " + id);
        }
        return row;
    }

    private static EventDefinitionCommand requireCommand(EventDefinitionCommand command) {
        if (command == null) {
            return new EventDefinitionCommand(null, null, null, null, null, null, null, null);
        }
        return command;
    }

    private static void requireEventCode(String eventCode) {
        if (eventCode == null || eventCode.isBlank()) {
            throw new IllegalArgumentException("eventCode is required");
        }
    }

    private void invalidateEventCode(String eventCode) {
        if (eventCode != null && !eventCode.isBlank()) {
            eventCodeInvalidator.accept(eventCode);
        }
    }

    private static String defaultString(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static EventDefinitionView view(EventRow row) {
        return new EventDefinitionView(
                row.id,
                row.name,
                row.eventCode,
                row.attributes,
                row.description,
                row.autoDiscover,
                row.discoveryMode,
                row.enabled,
                row.createdBy,
                row.createdAt,
                row.updatedAt);
    }

    private static final class EventRow {
        private Long id;
        private String name;
        private String eventCode;
        private String attributes;
        private String description;
        private Integer autoDiscover;
        private String discoveryMode;
        private Integer enabled;
        private String createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
