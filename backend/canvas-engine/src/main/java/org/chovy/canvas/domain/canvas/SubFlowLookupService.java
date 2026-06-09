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

    /**
     * 创建 SubFlowLookupService 实例并注入 domain.canvas 场景依赖。
     * @param canvasMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasVersionMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public SubFlowLookupService(CanvasMapper canvasMapper, CanvasVersionMapper canvasVersionMapper) {
        this.canvasMapper = canvasMapper;
        this.canvasVersionMapper = canvasVersionMapper;
    }

    /**
     * 按 ID 查询子流程引用的 Canvas。
     * 返回 {@code null} 表示引用目标不存在，调用方负责后续租户和发布状态校验。
     */
    public CanvasDO findCanvas(Long canvasId) {
        return canvasMapper.selectById(canvasId);
    }

    /**
     * 按版本 ID 查询子流程引用的 Canvas 版本。
     * 该方法只做数据查找，不校验版本是否可执行。
     */
    public CanvasVersionDO findVersion(Long versionId) {
        return canvasVersionMapper.selectById(versionId);
    }

    /**
     * 将 Canvas 编号和版本号解析为版本主键。
     * 找不到匹配版本时返回 {@code null}，便于上游决定使用默认版本或报错。
     */
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
