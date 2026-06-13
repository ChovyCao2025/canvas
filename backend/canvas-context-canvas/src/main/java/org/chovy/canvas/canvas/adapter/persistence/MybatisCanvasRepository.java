package org.chovy.canvas.canvas.adapter.persistence;

import java.util.Optional;

import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisCanvasRepository implements CanvasRepository {

    private final CanvasMapper mapper;

    public MybatisCanvasRepository(CanvasMapper mapper) {
        this.mapper = mapper;
    }

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

    @Override
    public Optional<Canvas> findById(Long canvasId) {
        return Optional.ofNullable(CanvasPersistenceMapper.toDomain(mapper.selectById(canvasId)));
    }
}
