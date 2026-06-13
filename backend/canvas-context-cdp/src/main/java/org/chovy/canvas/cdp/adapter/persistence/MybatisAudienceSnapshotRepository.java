package org.chovy.canvas.cdp.adapter.persistence;

import org.chovy.canvas.cdp.domain.AudienceSnapshot;
import org.chovy.canvas.cdp.domain.AudienceSnapshotRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisAudienceSnapshotRepository implements AudienceSnapshotRepository {

    private final AudienceSnapshotMapper snapshotMapper;
    private final AudienceDefinitionMapper definitionMapper;
    private final CdpPersistenceConverter converter;

    public MybatisAudienceSnapshotRepository(AudienceSnapshotMapper snapshotMapper,
                                             AudienceDefinitionMapper definitionMapper,
                                             CdpPersistenceConverter converter) {
        this.snapshotMapper = snapshotMapper;
        this.definitionMapper = definitionMapper;
        this.converter = converter;
    }

    @Override
    public List<String> resolveUsers(Long audienceId) {
        return List.of();
    }

    @Override
    public AudienceSnapshot save(AudienceSnapshot snapshot) {
        AudienceSnapshotDO row = converter.toAudienceSnapshotRow(snapshot);
        snapshotMapper.insert(row);
        return converter.toAudienceSnapshot(row);
    }

    @Override
    public AudienceSnapshot findSnapshot(Long snapshotId) {
        return converter.toAudienceSnapshot(snapshotMapper.selectById(snapshotId));
    }

    @Override
    public String defaultSnapshotMode(Long audienceId) {
        AudienceDefinitionDO row = definitionMapper.selectById(audienceId);
        return row == null ? null : row.getDefaultSnapshotMode();
    }
}
