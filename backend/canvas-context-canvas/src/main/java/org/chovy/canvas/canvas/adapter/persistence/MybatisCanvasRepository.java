package org.chovy.canvas.canvas.adapter.persistence;

import java.util.Optional;

import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasRepository;
import org.springframework.stereotype.Repository;

/**
 * 封装MybatisCanvasRepository相关的业务逻辑。
 */
@Repository
public class MybatisCanvasRepository implements CanvasRepository {

    /**
     * 保存映射器。
     */
    private final CanvasMapper mapper;

    /**
     * 创建当前对象实例。
     */
    public MybatisCanvasRepository(CanvasMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 保存。
     */
    @Override
    public Canvas save(Canvas canvas) {
        CanvasDO row = CanvasPersistenceMapper.toRow(canvas);
        if (row.getId() == null) {
            int inserted = mapper.insert(row);
            if (inserted <= 0) {
                throw new IllegalStateException("Canvas insert affected 0 rows");
            }
        } else {
            int updated = mapper.updateById(row);
            if (updated <= 0) {
                throw new IllegalStateException("Canvas update affected 0 rows: " + row.getId());
            }
        }
        return CanvasPersistenceMapper.toDomain(row);
    }

    /**
     * 查询by标识。
     */
    @Override
    public Optional<Canvas> findById(Long canvasId) {
        return Optional.ofNullable(CanvasPersistenceMapper.toDomain(mapper.selectById(canvasId)));
    }
}
