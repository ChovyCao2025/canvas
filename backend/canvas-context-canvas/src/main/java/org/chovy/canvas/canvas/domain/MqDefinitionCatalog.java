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

/**
 * 封装MqDefinitionCatalog相关的业务逻辑。
 */
public class MqDefinitionCatalog {

    /**
     * 保存内存场景下生成标识或统计次数的原子计数器。
     */
    private final AtomicLong ids = new AtomicLong();

    /**
     * 保存内存实现使用的rows映射数据。
     */
    private final Map<Long, MqRow> rows = new LinkedHashMap<>();

    /**
     * 保存内存场景下生成标识或统计次数的原子计数器。
     */
    private final AtomicInteger rebuildCount = new AtomicInteger();

    /**
     * 保存routeRebuilder。
     */
    private final IntConsumer routeRebuilder;

    /**
     * 创建当前对象实例。
     */
    public MqDefinitionCatalog() {
        this(null);
    }

    /**
     * 创建当前对象实例。
     */
    public MqDefinitionCatalog(IntConsumer routeRebuilder) {
        this.routeRebuilder = routeRebuilder;
    }

    /**
     * 列出。
     */
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

    /**
     * 创建。
     */
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

    /**
     * 更新。
     */
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

    /**
     * 删除。
     */
    public synchronized void delete(Long id) {
        if (rows.remove(id) == null) {
            throw new IllegalArgumentException("mq definition not found: " + id);
        }
        rebuildRoutes();
    }

    /**
     * 处理rebuildCount。
     */
    public int rebuildCount() {
        return rebuildCount.get();
    }

    /**
     * 处理rebuildRoutes。
     */
    private void rebuildRoutes() {
        int count = rebuildCount.incrementAndGet();
        if (routeRebuilder != null) {
            routeRebuilder.accept(count);
        }
    }

    /**
     * 校验并返回命令。
     */
    private static MqDefinitionCommand requireCommand(MqDefinitionCommand command) {
        if (command == null) {
            return new MqDefinitionCommand(null, null, null, null, null, null, null, null);
        }
        return command;
    }

    /**
     * 校验并返回Text。
     */
    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 处理view。
     */
    private static MqDefinitionView view(MqRow row) {
        return new MqDefinitionView(row.id, row.messageCode, row.topic, row.tags, row.consumerGroup,
                row.payloadSchema, row.description, row.enabled, row.createdBy, row.createdAt, row.updatedAt);
    }

    /**
     * 封装MqRow相关的业务逻辑。
     */
    private static final class MqRow {

        /**
         * 保存标识。
         */
        private Long id;

        /**
         * 保存messageCode。
         */
        private String messageCode;

        /**
         * 保存topic。
         */
        private String topic;

        /**
         * 保存tags。
         */
        private String tags;

        /**
         * 保存consumerGroup。
         */
        private String consumerGroup;

        /**
         * 保存payloadSchema。
         */
        private String payloadSchema;

        /**
         * 保存描述。
         */
        private String description;

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
