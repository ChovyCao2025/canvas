package org.chovy.canvas.canvas.api;

import java.util.List;

public interface DataSourceConfigFacade {

    PageView<DataSourceConfigView> list(TenantIdentity operator, DataSourceListQuery query);

    List<DataSourceTableMetaView> getTables(TenantIdentity operator, Long id);

    DataSourceConfigView create(TenantIdentity operator, DataSourceConfigCommand command);

    DataSourceConfigView update(TenantIdentity operator, Long id, DataSourceConfigCommand command);

    void delete(TenantIdentity operator, Long id);

    record DataSourceListQuery(
            int page,
            int size,
            String type,
            Integer enabled,
            Long tenantId
    ) {
    }

    record DataSourceConfigCommand(
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

    record DataSourceConfigView(
            Long id,
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

    record DataSourceTableMetaView(
            String name,
            List<String> columns
    ) {
    }

    record PageView<T>(
            long total,
            List<T> records
    ) {
    }

    record TenantIdentity(
            Long tenantId,
            String role,
            String username
    ) {

        public static TenantIdentity tenant(Long tenantId, String username) {
            return new TenantIdentity(tenantId, "TENANT_ADMIN", username);
        }

        public static TenantIdentity superAdmin(String username) {
            return new TenantIdentity(null, "SUPER_ADMIN", username);
        }

        public boolean isSuperAdmin() {
            return "SUPER_ADMIN".equals(role) || "ADMIN".equals(role);
        }
    }
}
