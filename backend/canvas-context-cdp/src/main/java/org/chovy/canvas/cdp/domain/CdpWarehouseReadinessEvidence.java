package org.chovy.canvas.cdp.domain;

import java.util.List;

public record CdpWarehouseReadinessEvidence(
        Long tenantId,
        List<WarehouseSyncRun> offlineRuns,
        List<WarehouseWatermark> watermarks,
        WarehouseRealtimeStatus realtimeStatus,
        List<WarehouseIncident> incidents,
        List<WarehouseBiDatasource> biDatasources,
        List<WarehouseMaterializationRun> materializationRuns) {

    public CdpWarehouseReadinessEvidence withIncidents(List<WarehouseIncident> newIncidents) {
        return new CdpWarehouseReadinessEvidence(tenantId, offlineRuns, watermarks, realtimeStatus, newIncidents,
                biDatasources, materializationRuns);
    }
}
