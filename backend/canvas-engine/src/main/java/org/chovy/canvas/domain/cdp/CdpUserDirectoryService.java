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

/**
 * CDP 用户 Directory CDP 领域服务。
 *
 * <p>负责用户画像、身份、标签和画布参与记录等客户数据能力，为画布执行和管理端查询提供统一入口。
 * <p>该层隔离 CDP 数据结构与上层业务，集中处理状态、历史和幂等语义。
 */
@Service
@RequiredArgsConstructor
public class CdpUserDirectoryService {

    /** CDP 用户画像 Mapper，用于用户目录检索和展示排序。 */
    private final CdpUserProfileMapper profileMapper;
    /** 画布执行记录 Mapper。 */
    private final CanvasExecutionMapper executionMapper;
    /** CDP 标签服务。 */
    private final CdpTagService tagService;

    /** 查询用户列表或指定画布下的用户聚合视图。 */
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

    /** 将用户画像与执行统计合并为用户目录列表行。 */
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

    /** 将最近一次执行状态码转换为目录展示文案。 */
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
