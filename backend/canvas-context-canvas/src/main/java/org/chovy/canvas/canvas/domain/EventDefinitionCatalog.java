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

/**
 * 封装EventDefinitionCatalog相关的业务逻辑。
 */
public class EventDefinitionCatalog {

    /**
     * 保存DEFAULT_ATTRIBUTES。
     */
    private static final String DEFAULT_ATTRIBUTES = "[]";

    /**
     * 保存DEFAULT_DISCOVERY_MODE。
     */
    private static final String DEFAULT_DISCOVERY_MODE = "REJECT_UNKNOWN";

    /**
     * 保存内存场景下生成标识或统计次数的原子计数器。
     */
    private final AtomicLong ids = new AtomicLong(1);

    /**
     * 保存内存实现使用的rows映射数据。
     */
    private final Map<Long, EventRow> rows = new LinkedHashMap<>();

    /**
     * 保存eventCodeInvalidator。
     */
    private final Consumer<String> eventCodeInvalidator;

    /**
     * 创建当前对象实例。
     */
    public EventDefinitionCatalog() {
        this(eventCode -> {
        });
    }

    /**
     * 创建当前对象实例。
     */
    public EventDefinitionCatalog(Consumer<String> eventCodeInvalidator) {
        this.eventCodeInvalidator = eventCodeInvalidator == null ? eventCode -> {
        } : eventCodeInvalidator;
    }

    /**
     * 列出。
     */
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

    /**
     * 创建。
     */
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

    /**
     * 更新。
     */
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

    /**
     * 删除。
     */
    public synchronized void delete(Long id) {
        EventRow existing = requireExisting(id);
        rows.remove(existing.id);
        invalidateEventCode(existing.eventCode);
    }

    /**
     * 校验并返回Existing。
     */
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

    /**
     * 校验并返回命令。
     */
    private static EventDefinitionCommand requireCommand(EventDefinitionCommand command) {
        if (command == null) {
            return new EventDefinitionCommand(null, null, null, null, null, null, null, null);
        }
        return command;
    }

    /**
     * 校验并返回EventCode。
     */
    private static void requireEventCode(String eventCode) {
        if (eventCode == null || eventCode.isBlank()) {
            throw new IllegalArgumentException("eventCode is required");
        }
    }

    /**
     * 处理invalidateEventCode。
     */
    private void invalidateEventCode(String eventCode) {
        if (eventCode != null && !eventCode.isBlank()) {
            eventCodeInvalidator.accept(eventCode);
        }
    }

    /**
     * 处理defaultString。
     */
    private static String defaultString(String value, String fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 处理view。
     */
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

    /**
     * 封装EventRow相关的业务逻辑。
     */
    private static final class EventRow {

        /**
         * 保存标识。
         */
        private Long id;

        /**
         * 保存名称。
         */
        private String name;

        /**
         * 保存eventCode。
         */
        private String eventCode;

        /**
         * 保存attributes。
         */
        private String attributes;

        /**
         * 保存描述。
         */
        private String description;

        /**
         * 保存autoDiscover。
         */
        private Integer autoDiscover;

        /**
         * 保存discoveryMode。
         */
        private String discoveryMode;

        /**
         * 保存启用状态。
         */
        private Integer enabled;

        /**
         * 保存创建人。
         */
        private String createdBy;

        /**
         * 保存创建时间。
         */
        private LocalDateTime createdAt;

        /**
         * 保存更新时间。
         */
        private LocalDateTime updatedAt;
    }
}
