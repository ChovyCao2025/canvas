package org.chovy.canvas.cdp.adapter.persistence;

import org.chovy.canvas.cdp.domain.AudienceSnapshot;
import org.chovy.canvas.cdp.domain.AudienceSnapshotRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 定义 MybatisAudienceSnapshot 的持久化访问契约。
 */
@Repository
public class MybatisAudienceSnapshotRepository implements AudienceSnapshotRepository {

    /**
     * snapshot Mapper。
     */
    private final AudienceSnapshotMapper snapshotMapper;

    /**
     * definition Mapper。
     */
    private final AudienceDefinitionMapper definitionMapper;

    /**
     * 持久化转换器。
     */
    private final CdpPersistenceConverter converter;

    /**
     * 创建当前组件实例。
     */
    public MybatisAudienceSnapshotRepository(AudienceSnapshotMapper snapshotMapper,
                                             AudienceDefinitionMapper definitionMapper,
                                             CdpPersistenceConverter converter) {
        this.snapshotMapper = snapshotMapper;
        this.definitionMapper = definitionMapper;
        this.converter = converter;
    }

    /**
     * 执行 resolveUsers 对应的 CDP 业务操作。
     */
    @Override
    public List<String> resolveUsers(Long audienceId) {
        return List.of();
    }

    /**
     * 保存save。
     */
    @Override
    public AudienceSnapshot save(AudienceSnapshot snapshot) {
        AudienceSnapshotDO row = converter.toAudienceSnapshotRow(snapshot);
        snapshotMapper.insert(row);
        return converter.toAudienceSnapshot(row);
    }

    /**
     * 查找Snapshot。
     */
    @Override
    public AudienceSnapshot findSnapshot(Long snapshotId) {
        return converter.toAudienceSnapshot(snapshotMapper.selectById(snapshotId));
    }

    /**
     * 返回默认的Snapshot Mode。
     */
    @Override
    public String defaultSnapshotMode(Long audienceId) {
        AudienceDefinitionDO row = definitionMapper.selectById(audienceId);
        return row == null ? null : row.getDefaultSnapshotMode();
    }
}
