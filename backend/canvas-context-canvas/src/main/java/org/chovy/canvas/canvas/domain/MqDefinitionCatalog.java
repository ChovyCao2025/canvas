package org.chovy.canvas.canvas.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;

import org.chovy.canvas.canvas.api.MqDefinitionFacade.MqDefinitionCommand;
import org.chovy.canvas.canvas.api.MqDefinitionFacade.MqDefinitionListQuery;
import org.chovy.canvas.canvas.api.MqDefinitionFacade.MqDefinitionView;
import org.chovy.canvas.canvas.api.MqDefinitionFacade.PageView;

public class MqDefinitionCatalog {

    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, MqRow> rows = new LinkedHashMap<>();
    private final AtomicInteger rebuildCount = new AtomicInteger();
    private final IntConsumer routeRebuilder;

    public MqDefinitionCatalog() {
        this(null);
    }

    public MqDefinitionCatalog(IntConsumer routeRebuilder) {
        this.routeRebuilder = routeRebuilder;
    }

    public synchronized PageView<MqDefinitionView> list(MqDefinitionListQuery query) {
        int page = query.page() <= 0 ? 1 : query.page();
        int size = query.size() <= 0 ? 20 : query.size();
        List<MqDefinitionView> filtered = rows.values().stream()
                .filter(row -> query.enabled() == null || Objects.equals(row.enabled, query.enabled()))
                .sorted(Comparator.comparing(row -> row.id))
                .map(MqDefinitionCatalog::view)
                .toList();
        int from = Math.min((page - 1) * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return new PageView<>(filtered.size(), filtered.subList(from, to));
    }

    public synchronized MqDefinitionView create(MqDefinitionCommand command) {
        MqDefinitionCommand safe = requireCommand(command);
        requireText(safe.messageCode(), "messageCode is required");
        requireText(safe.topic(), "topic is required");
        MqRow row = new MqRow();
        LocalDateTime now = LocalDateTime.now();
        row.id = ids.incrementAndGet();
        row.messageCode = safe.messageCode().trim();
        row.topic = safe.topic().trim();
        row.tags = safe.tags();
        row.consumerGroup = safe.consumerGroup();
        row.payloadSchema = safe.payloadSchema();
        row.description = safe.description();
        row.enabled = safe.enabled() == null ? 1 : safe.enabled();
        row.createdBy = safe.createdBy();
        row.createdAt = now;
        row.updatedAt = now;
        rows.put(row.id, row);
        rebuildRoutes();
        return view(row);
    }

    public synchronized MqDefinitionView update(Long id, MqDefinitionCommand command) {
        if (id == null) {
            throw new IllegalArgumentException("mq definition not found: " + id);
        }
        MqRow existing = rows.get(id);
        if (existing == null) {
            throw new IllegalArgumentException("mq definition not found: " + id);
        }
        MqDefinitionCommand safe = requireCommand(command);
        if (safe.messageCode() != null) {
            existing.messageCode = requireText(safe.messageCode(), "messageCode is required");
        }
        if (safe.topic() != null) {
            existing.topic = requireText(safe.topic(), "topic is required");
        }
        if (safe.tags() != null) {
            existing.tags = safe.tags();
        }
        if (safe.consumerGroup() != null) {
            existing.consumerGroup = safe.consumerGroup();
        }
        if (safe.payloadSchema() != null) {
            existing.payloadSchema = safe.payloadSchema();
        }
        if (safe.description() != null) {
            existing.description = safe.description();
        }
        if (safe.enabled() != null) {
            existing.enabled = safe.enabled();
        }
        if (safe.createdBy() != null) {
            existing.createdBy = safe.createdBy();
        }
        existing.updatedAt = LocalDateTime.now();
        rebuildRoutes();
        return view(existing);
    }

    public synchronized void delete(Long id) {
        if (rows.remove(id) == null) {
            throw new IllegalArgumentException("mq definition not found: " + id);
        }
        rebuildRoutes();
    }

    public int rebuildCount() {
        return rebuildCount.get();
    }

    private void rebuildRoutes() {
        int count = rebuildCount.incrementAndGet();
        if (routeRebuilder != null) {
            routeRebuilder.accept(count);
        }
    }

    private static MqDefinitionCommand requireCommand(MqDefinitionCommand command) {
        if (command == null) {
            return new MqDefinitionCommand(null, null, null, null, null, null, null, null);
        }
        return command;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static MqDefinitionView view(MqRow row) {
        return new MqDefinitionView(row.id, row.messageCode, row.topic, row.tags, row.consumerGroup,
                row.payloadSchema, row.description, row.enabled, row.createdBy, row.createdAt, row.updatedAt);
    }

    private static final class MqRow {
        private Long id;
        private String messageCode;
        private String topic;
        private String tags;
        private String consumerGroup;
        private String payloadSchema;
        private String description;
        private Integer enabled;
        private String createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
