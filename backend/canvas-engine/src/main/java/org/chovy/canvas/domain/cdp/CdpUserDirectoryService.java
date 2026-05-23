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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CdpUserDirectoryService {

    private final CdpUserProfileMapper profileMapper;
    private final CanvasExecutionMapper executionMapper;
    private final CdpTagService tagService;

    public List<CanvasUserRowDTO> listUsers(String keyword) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        List<CdpUserProfile> profiles = profileMapper.selectList(new LambdaQueryWrapper<CdpUserProfile>()
                .like(normalizedKeyword != null && !normalizedKeyword.isBlank(), CdpUserProfile::getUserId, normalizedKeyword)
                .or(normalizedKeyword != null && !normalizedKeyword.isBlank())
                .like(normalizedKeyword != null && !normalizedKeyword.isBlank(), CdpUserProfile::getDisplayName, normalizedKeyword)
                .orderByDesc(CdpUserProfile::getLastSeenAt)
                .orderByDesc(CdpUserProfile::getId));

        if (profiles.isEmpty()) {
            return List.of();
        }

        Set<String> userIds = profiles.stream().map(CdpUserProfile::getUserId).collect(Collectors.toSet());
        List<CanvasExecution> executions = executionMapper.selectList(new LambdaQueryWrapper<CanvasExecution>()
                .in(CanvasExecution::getUserId, userIds)
                .orderByDesc(CanvasExecution::getCreatedAt));
        Map<String, List<CanvasExecution>> executionsByUser = new LinkedHashMap<>();
        for (CanvasExecution execution : executions) {
            executionsByUser.computeIfAbsent(execution.getUserId(), ignored -> new java.util.ArrayList<>()).add(execution);
        }

        return profiles.stream()
                .map(profile -> toRow(profile, executionsByUser.getOrDefault(profile.getUserId(), List.of())))
                .toList();
    }

    private CanvasUserRowDTO toRow(CdpUserProfile profile, List<CanvasExecution> executions) {
        long successCount = executions.stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == ExecutionStatus.SUCCESS.getCode())
                .count();
        long failedCount = executions.stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == ExecutionStatus.FAILED.getCode())
                .count();
        CanvasExecution latest = executions.stream()
                .max(Comparator.comparing(CanvasExecution::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        LocalDateTime firstEnteredAt = profile.getFirstSeenAt();
        LocalDateTime lastEnteredAt = profile.getLastSeenAt();
        return new CanvasUserRowDTO(
                profile.getUserId(),
                profile.getDisplayName() == null || profile.getDisplayName().isBlank() ? profile.getUserId() : profile.getDisplayName(),
                executions.size(),
                successCount,
                failedCount,
                latest == null ? "-" : statusLabel(latest.getStatus()),
                firstEnteredAt,
                lastEnteredAt,
                tagService.listCurrentTags(profile.getUserId())
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
