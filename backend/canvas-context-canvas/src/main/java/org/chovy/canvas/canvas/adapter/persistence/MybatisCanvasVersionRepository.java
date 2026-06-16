package org.chovy.canvas.canvas.adapter.persistence;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.CanvasVersionRepository;
import org.chovy.canvas.canvas.domain.VersionStatus;
import org.springframework.stereotype.Repository;

/**
 * 封装MybatisCanvasVersionRepository相关的业务逻辑。
 */
@Repository
public class MybatisCanvasVersionRepository implements CanvasVersionRepository {

    /**
     * 保存映射器。
     */
    private final CanvasVersionMapper mapper;

    /**
     * 创建当前对象实例。
     */
    public MybatisCanvasVersionRepository(CanvasVersionMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 保存。
     */
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

    /**
     * 处理latestDraft。
     */
    @Override
    public Optional<CanvasVersion> latestDraft(Long canvasId) {
        CanvasVersionDO row = mapper.selectOne(new LambdaQueryWrapper<CanvasVersionDO>()
                .eq(CanvasVersionDO::getCanvasId, canvasId)
                .eq(CanvasVersionDO::getStatus, VersionStatus.DRAFT.code())
                .orderByDesc(CanvasVersionDO::getVersion)
                .last("LIMIT 1"));
        return Optional.ofNullable(CanvasVersionPersistenceMapper.toDomain(row));
    }

    /**
     * 查询by标识。
     */
    @Override
    public Optional<CanvasVersion> findById(Long versionId) {
        return Optional.ofNullable(CanvasVersionPersistenceMapper.toDomain(mapper.selectById(versionId)));
    }

    /**
     * 查询by canvas标识。
     */
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
