package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.domain.constant.ExecutionStatus;
import org.chovy.canvas.domain.execution.CanvasExecution;
import org.chovy.canvas.domain.execution.CanvasExecutionMapper;
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
        List<CanvasExecution> executions = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecution>()
                        .eq(CanvasExecution::getCanvasId, canvasId)
                        .isNotNull(CanvasExecution::getUserId)
                        .orderByDesc(CanvasExecution::getCreatedAt));
        Map<String, List<CanvasExecution>> byUser = new LinkedHashMap<>();
        for (CanvasExecution execution : executions) {
            byUser.computeIfAbsent(execution.getUserId(), ignored -> new java.util.ArrayList<>()).add(execution);
        }
        return byUser.entrySet().stream()
                .map(entry -> toRow(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CanvasUserRowDTO::lastEnteredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public CanvasUserRowDTO getUserInCanvas(Long canvasId, String userId) {
        List<CanvasExecution> executions = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecution>()
                        .eq(CanvasExecution::getCanvasId, canvasId)
                        .eq(CanvasExecution::getUserId, userId)
                        .orderByDesc(CanvasExecution::getCreatedAt));
        if (executions.isEmpty()) {
            throw new IllegalArgumentException("该用户没有进入过画布: " + userId);
        }
        return toRow(userId, executions);
    }

    public List<CanvasExecution> listExecutions(Long canvasId, String userId) {
        return executionMapper.selectList(new LambdaQueryWrapper<CanvasExecution>()
                .eq(CanvasExecution::getCanvasId, canvasId)
                .eq(CanvasExecution::getUserId, userId)
                .orderByDesc(CanvasExecution::getCreatedAt)
                .last("LIMIT 100"));
    }

    private CanvasUserRowDTO toRow(String userId, List<CanvasExecution> executions) {
        executions.forEach(e -> userService.ensureUser(userId, "CANVAS_EXECUTION", e.getId()));
        LocalDateTime first = executions.stream().map(CanvasExecution::getCreatedAt)
                .min(Comparator.naturalOrder()).orElse(null);
        LocalDateTime last = executions.stream().map(CanvasExecution::getCreatedAt)
                .max(Comparator.naturalOrder()).orElse(null);
        CanvasExecution latest = executions.stream()
                .max(Comparator.comparing(CanvasExecution::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
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
