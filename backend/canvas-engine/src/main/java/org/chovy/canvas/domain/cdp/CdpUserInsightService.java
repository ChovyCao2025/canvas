package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dto.cdp.CanvasUserDetailDTO;
import org.chovy.canvas.dto.cdp.CdpUserCanvasSummaryDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;

/**
 * CDP 用户 Insight CDP 领域服务。
 *
 * <p>负责用户画像、身份、标签和画布参与记录等客户数据能力，为画布执行和管理端查询提供统一入口。
 * <p>该层隔离 CDP 数据结构与上层业务，集中处理状态、历史和幂等语义。
 */
@Service
@RequiredArgsConstructor
public class CdpUserInsightService {

    /** CDP 用户服务，用于读取画像详情并补齐用户基础信息。 */
    private final CdpUserService userService;
    /** CDP 标签服务。 */
    private final CdpTagService tagService;
    /** 画布执行记录 Mapper。 */
    private final CanvasExecutionMapper executionMapper;
    /** 画布主表 Mapper。 */
    private final CanvasMapper canvasMapper;

    /** 聚合用户画像、标签和画布参与记录，形成用户洞察详情。 */
    public CanvasUserDetailDTO getUserInsight(String userId) {
        return buildUserInsight(null, userService.getRequiredProfile(userId));
    }

    /** 聚合指定租户内用户画像、标签和画布参与记录，形成用户洞察详情。 */
    public CanvasUserDetailDTO getUserInsight(Long tenantId, String userId) {
        return buildUserInsight(tenantId, userService.getRequiredProfile(tenantId, userId));
    }

    private CanvasUserDetailDTO buildUserInsight(Long tenantId, CdpUserProfileDO profile) {
        List<CanvasExecutionDO> executions = executionMapper.selectList(executionQuery(tenantId)
                .eq(CanvasExecutionDO::getUserId, profile.getUserId())
                .isNotNull(CanvasExecutionDO::getCanvasId)
                .orderByDesc(CanvasExecutionDO::getCreatedAt));
        Map<Long, List<CanvasExecutionDO>> byCanvas = new LinkedHashMap<>();
        for (CanvasExecutionDO execution : executions) {
            byCanvas.computeIfAbsent(execution.getCanvasId(), ignored -> new java.util.ArrayList<>()).add(execution);
        }

        Set<Long> canvasIds = byCanvas.keySet();
        Map<Long, CanvasDO> canvasMap = canvasIds.isEmpty()
                ? Map.of()
                : canvasMapper.selectList(canvasQuery(tenantId).in(CanvasDO::getId, canvasIds))
                .stream()
                .collect(Collectors.toMap(CanvasDO::getId, item -> item));

        List<CdpUserCanvasSummaryDTO> canvasRows = byCanvas.entrySet().stream()
                .map(entry -> toCanvasSummary(entry.getKey(), canvasMap.get(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(CdpUserCanvasSummaryDTO::lastEnteredAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return new CanvasUserDetailDTO(
                profile.getUserId(),
                userService.toDetail(profile),
                tagService.listCurrentTags(tenantId, profile.getUserId()),
                canvasRows
        );
    }

    /** 汇总用户在单个画布中的进入次数、成功失败数和最近状态。 */
    private CdpUserCanvasSummaryDTO toCanvasSummary(Long canvasId, CanvasDO canvas, List<CanvasExecutionDO> executions) {
        long successCount = executions.stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == ExecutionStatus.SUCCESS.getCode())
                .count();
        long failedCount = executions.stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == ExecutionStatus.FAILED.getCode())
                .count();
        LocalDateTime firstEnteredAt = executions.stream().map(CanvasExecutionDO::getCreatedAt)
                .min(Comparator.nullsLast(Comparator.naturalOrder()))
                .orElse(null);
        LocalDateTime lastEnteredAt = executions.stream().map(CanvasExecutionDO::getCreatedAt)
                .max(Comparator.nullsLast(Comparator.naturalOrder()))
                .orElse(null);
        CanvasExecutionDO latest = executions.stream()
                .max(Comparator.comparing(CanvasExecutionDO::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        return new CdpUserCanvasSummaryDTO(
                canvasId,
                canvas == null ? "画布#" + canvasId : canvas.getName(),
                executions.size(),
                successCount,
                failedCount,
                latest == null ? "-" : statusLabel(latest.getStatus()),
                firstEnteredAt,
                lastEnteredAt
        );
    }

    /** 将执行状态码转换为用户洞察页展示文案。 */
    private String statusLabel(Integer status) {
        if (status != null && status == ExecutionStatus.SUCCESS.getCode()) {
            return "SUCCESS";
        }
        if (status != null && status == ExecutionStatus.FAILED.getCode()) {
            return "FAILED";
        }
        if (status != null && status == ExecutionStatus.PAUSED.getCode()) {
            return "PAUSED";
        }
        if (status != null && status == ExecutionStatus.RUNNING.getCode()) {
            return "RUNNING";
        }
        return status == null ? "-" : String.valueOf(status);
    }

    private LambdaQueryWrapper<CanvasExecutionDO> executionQuery(Long tenantId) {
        LambdaQueryWrapper<CanvasExecutionDO> query = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            query.eq(CanvasExecutionDO::getTenantId, tenantId);
        }
        return query;
    }

    private LambdaQueryWrapper<CanvasDO> canvasQuery(Long tenantId) {
        LambdaQueryWrapper<CanvasDO> query = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            query.eq(CanvasDO::getTenantId, tenantId);
        }
        return query;
    }
}
