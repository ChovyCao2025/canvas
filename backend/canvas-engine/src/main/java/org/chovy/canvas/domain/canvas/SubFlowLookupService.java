package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.springframework.stereotype.Service;

/**
 * Domain boundary for sub-flow canvas and version lookup.
 */
@Service
public class SubFlowLookupService {

    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;

    public SubFlowLookupService(CanvasMapper canvasMapper, CanvasVersionMapper canvasVersionMapper) {
        this.canvasMapper = canvasMapper;
        this.canvasVersionMapper = canvasVersionMapper;
    }

    public CanvasDO findCanvas(Long canvasId) {
        return canvasMapper.selectById(canvasId);
    }

    public CanvasVersionDO findVersion(Long versionId) {
        return canvasVersionMapper.selectById(versionId);
    }

    public Long resolveVersionId(Long canvasId, int version) {
        return canvasVersionMapper.selectList(
                        new LambdaQueryWrapper<CanvasVersionDO>()
                                .eq(CanvasVersionDO::getCanvasId, canvasId)
                                .eq(CanvasVersionDO::getVersion, version))
                .stream()
                .findFirst()
                .map(CanvasVersionDO::getId)
                .orElse(null);
    }
}
