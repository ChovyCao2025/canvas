package org.chovy.canvas.canvas.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DataSourceConfigCatalog {

    public static final String JDBC = "JDBC";
    public static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String MASKED_PASSWORD = "******";

    private long nextId = 1L;
    private final Map<Long, DataSourceConfig> rows = new LinkedHashMap<>();

    public synchronized Page<DataSourceConfig> list(TenantScope scope, Query query) {
        int page = query == null || query.page() < 1 ? 1 : query.page();
        int size = query == null || query.size() < 1 ? 20 : query.size();
        String type = query == null ? null : query.type();
        Integer enabled = query == null ? null : query.enabled();
        Long requestedTenantId = query == null ? null : query.tenantId();
        Long visibleTenantId = scope.visibleTenantId(requestedTenantId);

        List<DataSourceConfig> filtered = rows.values().stream()
                .filter(row -> visibleTenantId == null || Objects.equals(row.tenantId(), visibleTenantId))
                .filter(row -> type == null || type.isBlank() || Objects.equals(row.type(), type))
                .filter(row -> enabled == null || Objects.equals(row.enabled(), enabled))
                .sorted(Comparator.comparing(DataSourceConfig::id).reversed())
                .toList();
        int from = Math.min((page - 1) * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return new Page<>(filtered.size(), filtered.subList(from, to));
    }

    public synchronized DataSourceConfig create(TenantScope scope, Command command) {
        DataSourceConfig normalized = normalize(command, scope.writeTenantId(command == null ? null : command.tenantId()), null);
        DataSourceConfig created = normalized.withId(nextId++);
        rows.put(created.id(), created);
        return created;
    }

    public synchronized DataSourceConfig update(TenantScope scope, Long id, Command command) {
        DataSourceConfig existing = requireVisible(scope, id);
        DataSourceConfig normalized = normalize(command, scope.writeTenantId(existing.tenantId()), existing)
                .withId(existing.id());
        rows.put(existing.id(), normalized);
        return normalized;
    }

    public synchronized void delete(TenantScope scope, Long id) {
        DataSourceConfig existing = requireVisible(scope, id);
        rows.remove(existing.id());
    }

    public synchronized List<TableMeta> tables(TenantScope scope, Long id) {
        DataSourceConfig config = requireVisible(scope, id);
        if (config.enabled() == null || config.enabled() == 0) {
            throw new IllegalArgumentException("Data source disabled: " + id);
        }
        if (!JDBC.equals(config.type())) {
            throw new IllegalArgumentException("Unsupported data source type: " + config.type());
        }
        return List.of(
                new TableMeta("orders", List.of("id", "user_id", "amount")),
                new TableMeta("users", List.of("id", "external_id", "updated_at")));
    }

    private DataSourceConfig requireVisible(TenantScope scope, Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Data source not found: " + id);
        }
        DataSourceConfig config = rows.get(id);
        if (config == null || !scope.canAccess(config.tenantId())) {
            throw new IllegalArgumentException("Data source not found: " + id);
        }
        return config;
    }

    private DataSourceConfig normalize(Command command, Long tenantId, DataSourceConfig existing) {
        if (command == null) {
            throw new IllegalArgumentException("data source command is required");
        }
        String type = textOrDefault(command.type(), JDBC);
        if (!JDBC.equals(type)) {
            throw new IllegalArgumentException("Unsupported data source type: " + type);
        }
        String password = command.password();
        requireText(command.name(), "name");
        requireText(command.url(), "url");
        requireText(command.username(), "username");
        requireText(password, "password");
        return new DataSourceConfig(
                existing == null ? null : existing.id(),
                requireTenantId(tenantId),
                command.name(),
                type,
                command.url(),
                command.username(),
                encrypt(password),
                textOrDefault(command.driverClassName(), DEFAULT_DRIVER),
                command.description(),
                command.enabled() == null ? 1 : command.enabled(),
                command.createdBy());
    }

    private static String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Long requireTenantId(Long tenantId) {
        if (tenantId == null || tenantId < 0) {
            throw new IllegalStateException("tenantId is required for data source operation");
        }
        return tenantId;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing data source field: " + field);
        }
    }

    private static String encrypt(String raw) {
        return "enc:" + new StringBuilder(raw).reverse();
    }

    public record Query(
            int page,
            int size,
            String type,
            Integer enabled,
            Long tenantId
    ) {
    }

    public record Command(
            Long tenantId,
            String name,
            String type,
            String url,
            String username,
            String password,
            String driverClassName,
            String description,
            Integer enabled,
            String createdBy
    ) {
    }

    public record TenantScope(
            Long tenantId,
            boolean superAdmin
    ) {

        public Long visibleTenantId(Long requestedTenantId) {
            if (superAdmin) {
                return requestedTenantId;
            }
            return tenantId;
        }

        public Long writeTenantId(Long requestedTenantId) {
            if (superAdmin && requestedTenantId != null) {
                return requestedTenantId;
            }
            return tenantId;
        }

        public boolean canAccess(Long rowTenantId) {
            return superAdmin || Objects.equals(tenantId, rowTenantId);
        }
    }

    public record DataSourceConfig(
            Long id,
            Long tenantId,
            String name,
            String type,
            String url,
            String username,
            String storedPassword,
            String driverClassName,
            String description,
            Integer enabled,
            String createdBy
    ) {

        DataSourceConfig withId(Long id) {
            return new DataSourceConfig(
                    id,
                    tenantId,
                    name,
                    type,
                    url,
                    username,
                    storedPassword,
                    driverClassName,
                    description,
                    enabled,
                    createdBy);
        }
    }

    public record Page<T>(
            long total,
            List<T> records
    ) {
        public Page {
            records = List.copyOf(new ArrayList<>(records));
        }
    }

    public record TableMeta(
            String name,
            List<String> columns
    ) {
    }
}
