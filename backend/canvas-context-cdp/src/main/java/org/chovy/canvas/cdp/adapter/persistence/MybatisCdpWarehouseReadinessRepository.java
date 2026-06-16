package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.cdp.domain.CdpWarehouseReadinessEvidence;
import org.chovy.canvas.cdp.domain.CdpWarehouseReadinessRepository;
import org.chovy.canvas.cdp.domain.WarehouseBiDatasource;
import org.chovy.canvas.cdp.domain.WarehouseRealtimeStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 定义 MybatisCdpWarehouseReadiness 的持久化访问契约。
 */
@Repository
public class MybatisCdpWarehouseReadinessRepository implements CdpWarehouseReadinessRepository {

    /**
     * RECENT LIMIT。
     */
    private static final int RECENT_LIMIT = 20;

    /**
     * sync Run Mapper。
     */
    private final CdpWarehouseSyncRunMapper syncRunMapper;

    /**
     * watermark Mapper。
     */
    private final CdpWarehouseWatermarkMapper watermarkMapper;

    /**
     * incident Mapper。
     */
    private final CdpWarehouseIncidentMapper incidentMapper;

    /**
     * materialization Run Mapper。
     */
    private final AudienceMaterializationRunMapper materializationRunMapper;

    /**
     * 持久化转换器。
     */
    private final CdpPersistenceConverter converter;

    /**
     * 创建当前组件实例。
     */
    public MybatisCdpWarehouseReadinessRepository(CdpWarehouseSyncRunMapper syncRunMapper,
                                                  CdpWarehouseWatermarkMapper watermarkMapper,
                                                  CdpWarehouseIncidentMapper incidentMapper,
                                                  AudienceMaterializationRunMapper materializationRunMapper,
                                                  CdpPersistenceConverter converter) {
        this.syncRunMapper = syncRunMapper;
        this.watermarkMapper = watermarkMapper;
        this.incidentMapper = incidentMapper;
        this.materializationRunMapper = materializationRunMapper;
        this.converter = converter;
    }

    /**
     * 执行 evidence 对应的 CDP 业务操作。
     */
    @Override
    public CdpWarehouseReadinessEvidence evidence(Long tenantId) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        return new CdpWarehouseReadinessEvidence(
                scopedTenantId,
                syncRunMapper.selectList(new LambdaQueryWrapper<CdpWarehouseSyncRunDO>()
                                .eq(CdpWarehouseSyncRunDO::getTenantId, scopedTenantId)
                                .orderByDesc(CdpWarehouseSyncRunDO::getFinishedAt)
                                .last("LIMIT " + RECENT_LIMIT))
                        .stream()
                        .map(converter::toSyncRun)
                        .toList(),
                watermarkMapper.selectList(new LambdaQueryWrapper<CdpWarehouseWatermarkDO>()
                                .eq(CdpWarehouseWatermarkDO::getTenantId, scopedTenantId)
                                .orderByDesc(CdpWarehouseWatermarkDO::getUpdatedAt)
                                .last("LIMIT " + RECENT_LIMIT))
                        .stream()
                        .map(converter::toWatermark)
                        .toList(),
                new WarehouseRealtimeStatus(0, 0, 0, 0),
                incidentMapper.selectList(new LambdaQueryWrapper<CdpWarehouseIncidentDO>()
                                .eq(CdpWarehouseIncidentDO::getTenantId, scopedTenantId)
                                .eq(CdpWarehouseIncidentDO::getStatus, "OPEN")
                                .orderByDesc(CdpWarehouseIncidentDO::getLastSeenAt)
                                .last("LIMIT 100"))
                        .stream()
                        .map(converter::toIncident)
                        .toList(),
                List.<WarehouseBiDatasource>of(),
                materializationRunMapper.selectList(new LambdaQueryWrapper<AudienceMaterializationRunDO>()
                                .eq(AudienceMaterializationRunDO::getTenantId, scopedTenantId)
                                .orderByDesc(AudienceMaterializationRunDO::getFinishedAt)
                                .last("LIMIT " + RECENT_LIMIT))
                        .stream()
                        .map(converter::toMaterializationRun)
                        .toList());
    }
}
