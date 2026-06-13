package org.chovy.canvas.canvas.adapter.persistence;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.CanvasVersionRepository;
import org.chovy.canvas.canvas.domain.VersionStatus;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisCanvasVersionRepository implements CanvasVersionRepository {

    private final CanvasVersionMapper mapper;

    public MybatisCanvasVersionRepository(CanvasVersionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CanvasVersion save(CanvasVersion version) {
        CanvasVersionDO row = CanvasVersionPersistenceMapper.toRow(version);
        if (row.getId() == null) {
            int inserted = mapper.insert(row);
            if (inserted <= 0) {
                throw new IllegalStateException("Canvas version insert affected 0 rows");
            }
        } else {
            int updated = mapper.updateById(row);
            if (updated <= 0) {
                throw new IllegalStateException("Canvas version update affected 0 rows: " + row.getId());
            }
        }
        return CanvasVersionPersistenceMapper.toDomain(row);
    }

    @Override
    public Optional<CanvasVersion> latestDraft(Long canvasId) {
        CanvasVersionDO row = mapper.selectOne(new LambdaQueryWrapper<CanvasVersionDO>()
                .eq(CanvasVersionDO::getCanvasId, canvasId)
                .eq(CanvasVersionDO::getStatus, VersionStatus.DRAFT.code())
                .orderByDesc(CanvasVersionDO::getVersion)
                .last("LIMIT 1"));
        return Optional.ofNullable(CanvasVersionPersistenceMapper.toDomain(row));
    }

    @Override
    public Optional<CanvasVersion> findById(Long versionId) {
        return Optional.ofNullable(CanvasVersionPersistenceMapper.toDomain(mapper.selectById(versionId)));
    }

    @Override
    public List<CanvasVersion> findByCanvasId(Long canvasId) {
        return mapper.selectList(new LambdaQueryWrapper<CanvasVersionDO>()
                        .eq(CanvasVersionDO::getCanvasId, canvasId)
                        .orderByDesc(CanvasVersionDO::getVersion))
                .stream()
                .map(CanvasVersionPersistenceMapper::toDomain)
                .toList();
    }
}
