package org.chovy.canvas.canvas.adapter.persistence;

import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.VersionStatus;

/**
 * 封装CanvasVersionPersistenceMapper相关的业务逻辑。
 */
final class CanvasVersionPersistenceMapper {

    /**
     * 创建当前对象实例。
     */
    private CanvasVersionPersistenceMapper() {
    }

    /**
     * 转换为Row。
     */
    static CanvasVersionDO toRow(CanvasVersion version) {
        CanvasVersionDO row = new CanvasVersionDO();
        row.setId(version.id());
        row.setTenantId(version.tenantId());
        row.setCanvasId(version.canvasId());
        row.setVersion(version.version());
        row.setGraphJson(version.graphJson());
        row.setStatus(version.status().code());
        row.setCreatedBy(version.createdBy());
        return row;
    }

    /**
     * 转换为Domain。
     */
    static CanvasVersion toDomain(CanvasVersionDO row) {
        if (row == null) {
            return null;
        }
        return new CanvasVersion(
                row.getId(),
                row.getCanvasId(),
                row.getTenantId(),
                row.getVersion(),
                row.getGraphJson(),
                VersionStatus.fromCode(row.getStatus()),
                row.getCreatedBy());
    }
}
