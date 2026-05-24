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
import java.util.Set;
import java.util.stream.Collectors;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;

@Service
@RequiredArgsConstructor
public class CdpUserDirectoryService {

    private final CdpUserProfileMapper profileMapper;
    private final CanvasExecutionMapper executionMapper;
    private final CdpTagService tagService;

    public List<CanvasUserRowDTO> listUsers(String keyword) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        List<CdpUserProfileDO> profiles = profileMapper.selectList(new LambdaQueryWrapper<CdpUserProfileDO>()
                .like(normalizedKeyword != null && !normalizedKeyword.isBlank(), CdpUserProfileDO::getUserId, normalizedKeyword)
                .or(normalizedKeyword != null && !normalizedKeyword.isBlank())
                .like(normalizedKeyword != null && !normalizedKeyword.isBlank(), CdpUserProfileDO::getDisplayName, normalizedKeyword)
                .orderByDesc(CdpUserProfileDO::getLastSeenAt)
                .orderByDesc(CdpUserProfileDO::getId));

        if (profiles.isEmpty()) {
            return List.of();
        }

        Set<String> userIds = profiles.stream().map(CdpUserProfileDO::getUserId).collect(Collectors.toSet());
        List<CanvasExecutionDO> executions = executionMapper.selectList(new LambdaQueryWrapper<CanvasExecutionDO>()
                .in(CanvasExecutionDO::getUserId, userIds)
                .orderByDesc(CanvasExecutionDO::getCreatedAt));
        Map<String, List<CanvasExecutionDO>> executionsByUser = new LinkedHashMap<>();
        for (CanvasExecutionDO execution : executions) {
            executionsByUser.computeIfAbsent(execution.getUserId(), ignored -> new java.util.ArrayList<>()).add(execution);
        }

        return profiles.stream()
                .map(profile -> toRow(profile, executionsByUser.getOrDefault(profile.getUserId(), List.of())))
                .toList();
    }

    private CanvasUserRowDTO toRow(CdpUserProfileDO profile, List<CanvasExecutionDO> executions) {
        long successCount = executions.stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == ExecutionStatus.SUCCESS.getCode())
                .count();
        long failedCount = executions.stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == ExecutionStatus.FAILED.getCode())
                .count();
        CanvasExecutionDO latest = executions.stream()
                .max(Comparator.comparing(CanvasExecutionDO::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
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
