package org.chovy.canvas.domain.bi.datasource;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BiDatasourceOnboardingView 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param id id 字段。
 * @param sourceKey sourceKey 字段。
 * @param name name 字段。
 * @param type type 字段。
 * @param connectorType connectorType 字段。
 * @param enabled enabled 字段。
 * @param driverClassName driverClassName 字段。
 * @param maskedUrl maskedUrl 字段。
 * @param maskedUsername maskedUsername 字段。
 * @param connectionMode connectionMode 字段。
 * @param schemaSyncStatus schemaSyncStatus 字段。
 * @param tableCount tableCount 字段。
 * @param lastSyncedAt lastSyncedAt 字段。
 * @param supportedModes supportedModes 字段。
 * @param supportStatus supportStatus 字段。
 * @param capabilities capabilities 字段。
 */
public record BiDatasourceOnboardingView(
        Long id,
        String sourceKey,
        String name,
        String type,
        String connectorType,
        boolean enabled,
        String driverClassName,
        String maskedUrl,
        String maskedUsername,
        String connectionMode,
        String schemaSyncStatus,
        int tableCount,
        LocalDateTime lastSyncedAt,
        List<String> supportedModes,
        String supportStatus,
        List<String> capabilities
) {

    public BiDatasourceOnboardingView {
        supportedModes = supportedModes == null ? List.of() : List.copyOf(supportedModes);
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
    }
}
