package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dto.cdp.CanvasUserRowDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CanvasUserQueryService {

    private final CanvasExecutionMapper executionMapper;
    private final CdpTagService tagService;
    private final CdpUserService userService;

    public List<CanvasUserRowDTO> listUsers(Long canvasId) {
        List<CanvasExecutionDO> executions = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecutionDO>()
                        .eq(CanvasExecutionDO::getCanvasId, canvasId)
                        .isNotNull(CanvasExecutionDO::getUserId)
                        .orderByDesc(CanvasExecutionDO::getCreatedAt));
        Map<String, List<CanvasExecutionDO>> byUser = new LinkedHashMap<>();
        for (CanvasExecutionDO execution : executions) {
            byUser.computeIfAbsent(execution.getUserId(), ignored -> new java.util.ArrayList<>()).add(execution);
        }
        return byUser.entrySet().stream()
                .map(entry -> toRow(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CanvasUserRowDTO::lastEnteredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public CanvasUserRowDTO getUserInCanvas(Long canvasId, String userId) {
        List<CanvasExecutionDO> executions = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecutionDO>()
                        .eq(CanvasExecutionDO::getCanvasId, canvasId)
                        .eq(CanvasExecutionDO::getUserId, userId)
                        .orderByDesc(CanvasExecutionDO::getCreatedAt));
        if (executions.isEmpty()) {
            throw new IllegalArgumentException("该用户没有进入过画布: " + userId);
        }
        return toRow(userId, executions);
    }

    public List<CanvasExecutionDO> listExecutions(Long canvasId, String userId) {
        return executionMapper.selectList(new LambdaQueryWrapper<CanvasExecutionDO>()
                .eq(CanvasExecutionDO::getCanvasId, canvasId)
                .eq(CanvasExecutionDO::getUserId, userId)
                .orderByDesc(CanvasExecutionDO::getCreatedAt)
                .last("LIMIT 100"));
    }

    private CanvasUserRowDTO toRow(String userId, List<CanvasExecutionDO> executions) {
        executions.forEach(e -> userService.ensureUser(userId, "CANVAS_EXECUTION", e.getId()));
        LocalDateTime first = executions.stream().map(CanvasExecutionDO::getCreatedAt)
                .min(Comparator.naturalOrder()).orElse(null);
        LocalDateTime last = executions.stream().map(CanvasExecutionDO::getCreatedAt)
                .max(Comparator.naturalOrder()).orElse(null);
        CanvasExecutionDO latest = executions.stream()
                .max(Comparator.comparing(CanvasExecutionDO::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        long success = executions.stream().filter(e -> e.getStatus() != null && e.getStatus() == ExecutionStatus.SUCCESS.getCode()).count();
        long failed = executions.stream().filter(e -> e.getStatus() != null && e.getStatus() == ExecutionStatus.FAILED.getCode()).count();
        return new CanvasUserRowDTO(
                userId,
                userId,
                executions.size(),
                success,
                failed,
                latest == null ? "-" : statusLabel(latest.getStatus()),
                first,
                last,
                tagService.listCurrentTags(userId)
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
        return String.valueOf(status);
    }
}
