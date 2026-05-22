package org.chovy.canvas.domain.canvas;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.engine.dag.DagGraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CanvasTransactionService {
    private final CanvasMapper  canvasMapper;

    @Transactional
    Long offlineDb(Long id) {
        Canvas canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);
        Long publishedVersionId = canvas.getPublishedVersionId();
        canvas.setStatus(CanvasStatusEnum.OFFLINE.getCode());
        canvas.setPublishedVersionId(null);
        canvasMapper.updateById(canvas);
        return publishedVersionId;
    }

    @Transactional
    void archiveDb(Long id) {
        Canvas canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);
        canvas.setStatus(CanvasStatusEnum.ARCHIVED.getCode());
        canvasMapper.updateById(canvas);
    }



}
