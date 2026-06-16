package org.chovy.canvas.canvas.api;

import java.util.List;

/**
 * 定义DataSourceConfigFacade对外提供的能力契约。
 */
public interface DataSourceConfigFacade {

    /**
     * 列出。
     */
    PageView<DataSourceConfigView> list(TenantIdentity operator, DataSourceListQuery query);

    /**
     * 获取Tables。
     */
    List<DataSourceTableMetaView> getTables(TenantIdentity operator, Long id);

    /**
     * 创建。
     */
    DataSourceConfigView create(TenantIdentity operator, DataSourceConfigCommand command);

    /**
     * 更新。
     */
    DataSourceConfigView update(TenantIdentity operator, Long id, DataSourceConfigCommand command);

    /**
     * 删除。
     */
    void delete(TenantIdentity operator, Long id);

    /**
     * 承载DataSourceListQuery的数据快照。
     */
    record DataSourceListQuery(
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
     * 承载DataSourceConfigCommand的数据快照。
     */
    record DataSourceConfigCommand(
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
     * 承载DataSourceConfigView的数据快照。
     */
    record DataSourceConfigView(
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
     * 承载DataSourceTableMetaView的数据快照。
     */
    record DataSourceTableMetaView(
            /**
             * 记录名称。
             */
            String name,
            List<String> columns
    ) {
    }

    /**
     * 承载PageView的数据快照。
     */
    record PageView<T>(
            /**
             * 记录总数。
             */
            long total,
            List<T> records
    ) {
    }

    /**
     * 承载TenantIdentity的数据快照。
     */
    record TenantIdentity(
            /**
             * 记录租户标识。
             */
            Long tenantId,
            /**
             * 记录role。
             */
            String role,
            String username
    ) {

        /**
         * 创建普通租户身份。
         */
        public static TenantIdentity tenant(Long tenantId, String username) {
            return new TenantIdentity(tenantId, "TENANT_ADMIN", username);
        }

        /**
         * 创建超级管理员身份。
         */
        public static TenantIdentity superAdmin(String username) {
            return new TenantIdentity(null, "SUPER_ADMIN", username);
        }

        /**
         * 判断当前身份是否为超级管理员。
         */
        public boolean isSuperAdmin() {
            return "SUPER_ADMIN".equals(role) || "ADMIN".equals(role);
        }
    }
}
