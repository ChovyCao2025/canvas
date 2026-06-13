package org.chovy.canvas.canvas.adapter.persistence;

import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.VersionStatus;

final class CanvasVersionPersistenceMapper {

    private CanvasVersionPersistenceMapper() {
    }

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
