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

@Service
@RequiredArgsConstructor
public class CdpUserInsightService {

    private final CdpUserService userService;
    private final CdpTagService tagService;
    private final CanvasExecutionMapper executionMapper;
    private final CanvasMapper canvasMapper;

    public CanvasUserDetailDTO getUserInsight(String userId) {
        CdpUserProfileDO profile = userService.getRequiredProfile(userId);
        List<CanvasExecutionDO> executions = executionMapper.selectList(new LambdaQueryWrapper<CanvasExecutionDO>()
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
                : canvasMapper.selectList(new LambdaQueryWrapper<CanvasDO>().in(CanvasDO::getId, canvasIds))
                .stream()
                .collect(Collectors.toMap(CanvasDO::getId, item -> item));

        List<CdpUserCanvasSummaryDTO> canvasRows = byCanvas.entrySet().stream()
                .map(entry -> toCanvasSummary(entry.getKey(), canvasMap.get(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(CdpUserCanvasSummaryDTO::lastEnteredAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return new CanvasUserDetailDTO(
                profile.getUserId(),
                userService.toDetail(profile),
                tagService.listCurrentTags(profile.getUserId()),
                canvasRows
        );
    }

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
}
