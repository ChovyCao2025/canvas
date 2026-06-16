package org.chovy.canvas.cdp.domain;

import java.util.List;

/**
 * 表示 CdpWarehouseReadinessEvidence 的业务数据或处理组件。
 */
public final class CdpWarehouseReadinessEvidence {

    /**
     * 租户标识。
     */
    private final Long tenantId;

    /**
     * offline Runs。
     */
    private final List<WarehouseSyncRun> offlineRuns;

    /**
     * watermarks。
     */
    private final List<WarehouseWatermark> watermarks;

    /**
     * realtime Status。
     */
    private final WarehouseRealtimeStatus realtimeStatus;

    /**
     * incidents。
     */
    private final List<WarehouseIncident> incidents;

    /**
     * bi Datasources。
     */
    private final List<WarehouseBiDatasource> biDatasources;

    /**
     * materialization Runs。
     */
    private final List<WarehouseMaterializationRun> materializationRuns;

    /**
     * 使用记录字段创建 CdpWarehouseReadinessEvidence。
     */
    public CdpWarehouseReadinessEvidence(
            Long tenantId,
            List<WarehouseSyncRun> offlineRuns,
            List<WarehouseWatermark> watermarks,
            WarehouseRealtimeStatus realtimeStatus,
            List<WarehouseIncident> incidents,
            List<WarehouseBiDatasource> biDatasources,
            List<WarehouseMaterializationRun> materializationRuns) {
        this.tenantId = tenantId;
        this.offlineRuns = offlineRuns;
        this.watermarks = watermarks;
        this.realtimeStatus = realtimeStatus;
        this.incidents = incidents;
        this.biDatasources = biDatasources;
        this.materializationRuns = materializationRuns;
    }

/**
 * 返回替换Incidents后的副本。
 */
public CdpWarehouseReadinessEvidence withIncidents(List<WarehouseIncident> newIncidents) {
        return new CdpWarehouseReadinessEvidence(tenantId, offlineRuns, watermarks, realtimeStatus, newIncidents,
                biDatasources, materializationRuns);
    }

    /**
     * 返回租户标识。
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回offline Runs。
     */
    public List<WarehouseSyncRun> offlineRuns() {
        return offlineRuns;
    }

    /**
     * 返回watermarks。
     */
    public List<WarehouseWatermark> watermarks() {
        return watermarks;
    }

    /**
     * 返回realtime Status。
     */
    public WarehouseRealtimeStatus realtimeStatus() {
        return realtimeStatus;
    }

    /**
     * 返回incidents。
     */
    public List<WarehouseIncident> incidents() {
        return incidents;
    }

    /**
     * 返回bi Datasources。
     */
    public List<WarehouseBiDatasource> biDatasources() {
        return biDatasources;
    }

    /**
     * 返回materialization Runs。
     */
    public List<WarehouseMaterializationRun> materializationRuns() {
        return materializationRuns;
    }

    /**
     * 按所有字段比较 CdpWarehouseReadinessEvidence。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpWarehouseReadinessEvidence that = (CdpWarehouseReadinessEvidence) o;
        return java.util.Objects.equals(tenantId, that.tenantId)
                && java.util.Objects.equals(offlineRuns, that.offlineRuns)
                && java.util.Objects.equals(watermarks, that.watermarks)
                && java.util.Objects.equals(realtimeStatus, that.realtimeStatus)
                && java.util.Objects.equals(incidents, that.incidents)
                && java.util.Objects.equals(biDatasources, that.biDatasources)
                && java.util.Objects.equals(materializationRuns, that.materializationRuns);
    }

    /**
     * 根据所有字段计算 CdpWarehouseReadinessEvidence 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(tenantId, offlineRuns, watermarks, realtimeStatus, incidents, biDatasources, materializationRuns);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpWarehouseReadinessEvidence[" + "tenantId=" + tenantId + ", offlineRuns=" + offlineRuns + ", watermarks=" + watermarks + ", realtimeStatus=" + realtimeStatus + ", incidents=" + incidents + ", biDatasources=" + biDatasources + ", materializationRuns=" + materializationRuns + "]";
    }
}
