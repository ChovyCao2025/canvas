package org.chovy.canvas.canvas.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 封装DataSourceConfigCatalog相关的业务逻辑。
 */
public class DataSourceConfigCatalog {

    /**
     * 保存JDBC。
     */
    public static final String JDBC = "JDBC";

    /**
     * 保存DEFAULT_DRIVER。
     */
    public static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";

    /**
     * 保存MASKED_PASSWORD。
     */
    public static final String MASKED_PASSWORD = "******";

    /**
     * 保存next标识。
     */
    private long nextId = 1L;

    /**
     * 保存内存实现使用的rows映射数据。
     */
    private final Map<Long, DataSourceConfig> rows = new LinkedHashMap<>();

    /**
     * 列出。
     */
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

    /**
     * 创建。
     */
    public synchronized DataSourceConfig create(TenantScope scope, Command command) {
        DataSourceConfig normalized = normalize(command, scope.writeTenantId(command == null ? null : command.tenantId()), null);
        DataSourceConfig created = normalized.withId(nextId++);
        rows.put(created.id(), created);
        return created;
    }

    /**
     * 更新。
     */
    public synchronized DataSourceConfig update(TenantScope scope, Long id, Command command) {
        DataSourceConfig existing = requireVisible(scope, id);
        DataSourceConfig normalized = normalize(command, scope.writeTenantId(existing.tenantId()), existing)
                .withId(existing.id());
        rows.put(existing.id(), normalized);
        return normalized;
    }

    /**
     * 删除。
     */
    public synchronized void delete(TenantScope scope, Long id) {
        DataSourceConfig existing = requireVisible(scope, id);
        rows.remove(existing.id());
    }

    /**
     * 处理tables。
     */
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

    /**
     * 校验并返回Visible。
     */
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

    /**
     * 规范化。
     */
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

    /**
     * 处理textOrDefault。
     */
    private static String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 校验并返回租户标识。
     */
    private static Long requireTenantId(Long tenantId) {
        if (tenantId == null || tenantId < 0) {
            throw new IllegalStateException("tenantId is required for data source operation");
        }
        return tenantId;
    }

    /**
     * 校验并返回Text。
     */
    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing data source field: " + field);
        }
    }

    /**
     * 处理encrypt。
     */
    private static String encrypt(String raw) {
        return "enc:" + new StringBuilder(raw).reverse();
    }

    /**
     * 承载查询条件的数据快照。
     */
    public record Query(
            /**
             * 记录页码。
             */
            int page,
            /**
             * 记录分页大小。
             */
            int size,
            /**
             * 记录类型。
             */
            String type,
            /**
             * 记录启用状态。
             */
            Integer enabled,
            Long tenantId
    ) {
    }

    /**
     * 承载命令的数据快照。
     */
    public record Command(
            /**
             * 记录租户标识。
             */
            Long tenantId,
            /**
             * 记录名称。
             */
            String name,
            /**
             * 记录类型。
             */
            String type,
            /**
             * 记录url。
             */
            String url,
            /**
             * 记录username。
             */
            String username,
            /**
             * 记录password。
             */
            String password,
            /**
             * 记录driverClassName。
             */
            String driverClassName,
            /**
             * 记录描述。
             */
            String description,
            /**
             * 记录启用状态。
             */
            Integer enabled,
            String createdBy
    ) {
    }

    /**
     * 承载TenantScope的数据快照。
     */
    public record TenantScope(
            /**
             * 记录租户标识。
             */
            Long tenantId,
            boolean superAdmin
    ) {

        /**
         * 计算当前租户视角可见的数据租户。
         */
        public Long visibleTenantId(Long requestedTenantId) {
            if (superAdmin) {
                return requestedTenantId;
            }
            return tenantId;
        }

        /**
         * 计算当前身份写入数据时使用的租户。
         */
        public Long writeTenantId(Long requestedTenantId) {
            if (superAdmin && requestedTenantId != null) {
                return requestedTenantId;
            }
            return tenantId;
        }

        /**
         * 判断当前身份是否可以访问指定租户的数据。
         */
        public boolean canAccess(Long rowTenantId) {
            return superAdmin || Objects.equals(tenantId, rowTenantId);
        }
    }

    /**
     * 承载DataSourceConfig的数据快照。
     */
    public record DataSourceConfig(
            /**
             * 记录标识。
             */
            Long id,
            /**
             * 记录租户标识。
             */
            Long tenantId,
            /**
             * 记录名称。
             */
            String name,
            /**
             * 记录类型。
             */
            String type,
            /**
             * 记录url。
             */
            String url,
            /**
             * 记录username。
             */
            String username,
            /**
             * 记录storedPassword。
             */
            String storedPassword,
            /**
             * 记录driverClassName。
             */
            String driverClassName,
            /**
             * 记录描述。
             */
            String description,
            /**
             * 记录启用状态。
             */
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

    /**
     * 承载页码的数据快照。
     */
    public record Page<T>(
            /**
             * 记录总数。
             */
            long total,
            List<T> records
    ) {
        public Page {
            records = List.copyOf(new ArrayList<>(records));
        }
    }

    /**
     * 承载TableMeta的数据快照。
     */
    public record TableMeta(
            /**
             * 记录名称。
             */
            String name,
            List<String> columns
    ) {
    }
}
