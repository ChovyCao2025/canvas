package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.dataobject.CanvasWaitSubscriptionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasWaitSubscriptionMapper;
import org.chovy.canvas.engine.wait.WaitSubscriptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;

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

    /** 画布版本 Mapper，用于查询发布版本并清空旧版本 graphJson。 */
    private final CanvasVersionMapper canvasVersionMapper;
    /** 画布 Mapper，用于全量遍历需要清理的画布。 */
    private final CanvasMapper        canvasMapper;
    /** 执行记录 Mapper，用于保护仍在运行或挂起的版本。 */
    private final CanvasExecutionMapper canvasExecutionMapper;
    /** 等待订阅 Mapper，用于保护等待恢复链路需要的版本。 */
    private final CanvasWaitSubscriptionMapper waitSubscriptionMapper;

    /** 每个画布保留的最近已发布版本数量。 */
    @Value("${canvas.version.max-keep-count:10}")
    private int maxKeepCount;

    @Scheduled(cron = "0 0 3 * * *")  // 每天凌晨3点
    public void cleanup() {
        log.info("[VERSION_CLEANUP] 开始执行版本清理，maxKeepCount={}", maxKeepCount);
        int totalCleaned = 0;

        // 全量画布逐个清理，单画布失败不影响其他画布
        List<CanvasDO> canvases = canvasMapper.selectList(null);
        for (CanvasDO canvas : canvases) {
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
        List<CanvasVersionDO> published = canvasVersionMapper.selectList(
                new LambdaQueryWrapper<CanvasVersionDO>()
                        .eq(CanvasVersionDO::getCanvasId, canvasId)
                        .eq(CanvasVersionDO::getStatus, 1)
                        .orderByDesc(CanvasVersionDO::getVersion)
        );

        // 发布版本数未超过保留上限，无需清理
        if (published.size() <= maxKeepCount) return 0;

        // 超出 maxKeepCount 的版本：仅清理没有任何运行时或回滚引用的旧快照。
        List<CanvasVersionDO> toClean = published.subList(maxKeepCount, published.size());
        Set<Long> referencedVersionIds = referencedVersionIds(canvasId, toClean);
        int count = 0;
        for (CanvasVersionDO v : toClean) {
            if (referencedVersionIds.contains(v.getId())) {
                log.debug("[VERSION_CLEANUP] skip referenced version canvasId={} versionId={} version={}",
                        canvasId, v.getId(), v.getVersion());
                continue;
            }
            if (v.getGraphJson() != null && !v.getGraphJson().isBlank()) {
                // 只清空 graphJson，保留元数据用于审计与回溯
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

    private Set<Long> referencedVersionIds(Long canvasId, List<CanvasVersionDO> candidates) {
        Set<Long> referenced = new HashSet<>();
        CanvasDO canvas = canvasMapper.selectById(canvasId);
        if (canvas != null) {
            addIfPresent(referenced, canvas.getPublishedVersionId());
            addIfPresent(referenced, canvas.getPreviousVersionId());
            addIfPresent(referenced, canvas.getCanaryVersionId());
        }

        List<Long> candidateIds = candidates.stream()
                .map(CanvasVersionDO::getId)
                .filter(Objects::nonNull)
                .toList();
        if (candidateIds.isEmpty()) {
            return referenced;
        }
        referenced.addAll(activeExecutionVersionIds(canvasId, candidateIds));
        referenced.addAll(activeWaitVersionIds(canvasId, candidateIds));
        return referenced;
    }

    private Set<Long> activeExecutionVersionIds(Long canvasId, List<Long> candidateIds) {
        return canvasExecutionMapper.selectList(
                        new LambdaQueryWrapper<CanvasExecutionDO>()
                                .eq(CanvasExecutionDO::getCanvasId, canvasId)
                                .in(CanvasExecutionDO::getVersionId, candidateIds)
                                .in(CanvasExecutionDO::getStatus,
                                        List.of(ExecutionStatus.RUNNING.getCode(), ExecutionStatus.PAUSED.getCode()))
                ).stream()
                .map(CanvasExecutionDO::getVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<Long> activeWaitVersionIds(Long canvasId, List<Long> candidateIds) {
        return waitSubscriptionMapper.selectList(
                        new LambdaQueryWrapper<CanvasWaitSubscriptionDO>()
                                .eq(CanvasWaitSubscriptionDO::getCanvasId, canvasId)
                                .in(CanvasWaitSubscriptionDO::getVersionId, candidateIds)
                                .eq(CanvasWaitSubscriptionDO::getStatus, WaitSubscriptionService.STATUS_ACTIVE)
                ).stream()
                .map(CanvasWaitSubscriptionDO::getVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static void addIfPresent(Set<Long> values, Long value) {
        if (value != null) values.add(value);
    }
}
