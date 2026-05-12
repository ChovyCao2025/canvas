package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 旧版本清理 Job（设计文档 17.2节）。
 *
 * 策略：每个画布保留最近 N 个已发布版本，更旧的版本清空 graph_json
 * （保留元数据 id/version/status/created_at 供审计，减少存储占用）。
 *
 * 配置：canvas.version.max-keep-count=10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasVersionCleanupJob {

    private final CanvasVersionMapper canvasVersionMapper;
    private final CanvasMapper        canvasMapper;

    @Value("${canvas.version.max-keep-count:10}")
    private int maxKeepCount;

    @Scheduled(cron = "0 0 3 * * *")  // 每天凌晨3点
    public void cleanup() {
        log.info("[VERSION_CLEANUP] 开始执行版本清理，maxKeepCount={}", maxKeepCount);
        int totalCleaned = 0;

        List<Canvas> canvases = canvasMapper.selectList(null);
        for (Canvas canvas : canvases) {
            try {
                int cleaned = cleanupCanvas(canvas.getId());
                totalCleaned += cleaned;
            } catch (Exception e) {
                log.error("[VERSION_CLEANUP] 清理失败 canvasId={}: {}", canvas.getId(), e.getMessage());
            }
        }

        log.info("[VERSION_CLEANUP] 完成，共清理 {} 个旧版本 graph_json", totalCleaned);
    }

    /**
     * 清理指定画布的旧版本：超出 maxKeepCount 的已发布版本，清空其 graph_json。
     */
    public int cleanupCanvas(Long canvasId) {
        // 按版本号降序查出所有已发布版本
        List<CanvasVersion> published = canvasVersionMapper.selectList(
                new LambdaQueryWrapper<CanvasVersion>()
                        .eq(CanvasVersion::getCanvasId, canvasId)
                        .eq(CanvasVersion::getStatus, 1)
                        .orderByDesc(CanvasVersion::getVersion)
        );

        if (published.size() <= maxKeepCount) return 0;

        // 超出 maxKeepCount 的版本：清空 graph_json（保留 id/version/status/created_at）
        List<CanvasVersion> toClean = published.subList(maxKeepCount, published.size());
        int count = 0;
        for (CanvasVersion v : toClean) {
            if (v.getGraphJson() != null && !v.getGraphJson().isBlank()) {
                v.setGraphJson(null);
                canvasVersionMapper.updateById(v);
                count++;
            }
        }

        if (count > 0) {
            log.debug("[VERSION_CLEANUP] canvasId={} 清理了 {} 个旧版本 graph_json", canvasId, count);
        }
        return count;
    }
}
