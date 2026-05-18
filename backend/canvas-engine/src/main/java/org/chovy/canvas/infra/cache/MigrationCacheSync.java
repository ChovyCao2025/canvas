package org.chovy.canvas.infra.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * 启动时同步 Flyway 数据迁移与配置缓存。
 *
 * <p>问题背景：Flyway 迁移只更新 MySQL，不更新 Redis L2 缓存。
 * 若某次迁移修改了 canvas_version.graph_json（如 V31），Redis 里仍是旧数据，
 * 导致执行引擎使用旧 DAG 图，需要主动失效缓存。
 *
 * <p>策略：每次启动时检查 Flyway 最新执行的迁移版本，
 * 若迁移描述包含 {@code [cache-invalidate]} 标记，则失效所有已发布画布缓存。
 * 若迁移描述包含 {@code [cache-invalidate:id=N]} 标记，则仅失效指定画布。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MigrationCacheSync implements ApplicationRunner {

    private final Flyway           flyway;
    private final CanvasConfigCache configCache;
    private final CanvasMapper      canvasMapper;

    @Override
    public void run(ApplicationArguments args) {
        MigrationInfo[] applied = flyway.info().applied();
        if (applied == null || applied.length == 0) return;

        // 取最新一次迁移的描述（description 对应 SQL 文件的双下划线后的部分）
        MigrationInfo latest = applied[applied.length - 1];
        String desc = latest.getDescription() != null ? latest.getDescription() : "";

        // 按描述中的约定标记决定失效范围
        if (desc.contains("cache-invalidate:id=")) {
            // 仅失效指定画布，如 "cache-invalidate:id=7"
            int start = desc.indexOf("cache-invalidate:id=") + "cache-invalidate:id=".length();
            int end   = desc.indexOf(']', start);
            String idStr = (end > start ? desc.substring(start, end) : desc.substring(start)).trim();
            try {
                Long canvasId = Long.parseLong(idStr);
                invalidateSingleCanvas(canvasId);
            } catch (NumberFormatException e) {
                log.warn("[MIGRATION_CACHE_SYNC] 无法解析 canvas id: {}", idStr);
            }
        } else if (desc.contains("unify_start_trigger_node") || desc.contains("cache-invalidate-all")) {
            // V31 或带 all 标记的迁移：失效所有已发布画布
            invalidateAllPublishedCanvases();
        }
    }

    private void invalidateSingleCanvas(Long canvasId) {
        Canvas canvas = canvasMapper.selectById(canvasId);
        if (canvas == null) return;
        if (canvas.getPublishedVersionId() != null) {
            configCache.invalidate(canvasId, canvas.getPublishedVersionId());
            log.info("[MIGRATION_CACHE_SYNC] 已失效画布缓存 canvasId={} versionId={}",
                    canvasId, canvas.getPublishedVersionId());
        }
    }

    private void invalidateAllPublishedCanvases() {
        canvasMapper.selectList(
                new LambdaQueryWrapper<Canvas>()
                        .eq(Canvas::getStatus, CanvasStatusEnum.PUBLISHED.getCode())
                        .isNotNull(Canvas::getPublishedVersionId)
        ).forEach(canvas -> {
            configCache.invalidate(canvas.getId(), canvas.getPublishedVersionId());
            log.info("[MIGRATION_CACHE_SYNC] 已失效画布缓存 canvasId={} versionId={}",
                    canvas.getId(), canvas.getPublishedVersionId());
        });
    }
}
