package org.chovy.canvas.domain.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionStatsDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectFolderDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectMemberDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectFolderMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectMemberMapper;
import org.chovy.canvas.dto.project.ProjectCreateReq;
import org.chovy.canvas.dto.project.ProjectDetailResp;
import org.chovy.canvas.dto.project.ProjectMemberResp;
import org.chovy.canvas.dto.project.ProjectMemberUpdateReq;
import org.chovy.canvas.dto.project.ProjectStatsResp;
import org.chovy.canvas.dto.project.ProjectSummaryResp;
import org.chovy.canvas.dto.project.ProjectUpdateReq;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CanvasProjectService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DISABLED = "DISABLED";
    private static final String MANUAL = "MANUAL";

    private final CanvasProjectMapper projectMapper;
    private final CanvasProjectMemberMapper memberMapper;
    private final CanvasProjectFolderMapper folderMapper;
    private final CanvasMapper canvasMapper;
    private final CanvasExecutionStatsMapper executionStatsMapper;

    public List<ProjectSummaryResp> list(Long tenantId, String username, boolean superAdmin, boolean tenantAdmin) {
        LambdaQueryWrapper<CanvasProjectDO> wrapper = new LambdaQueryWrapper<CanvasProjectDO>()
                .eq(!superAdmin, CanvasProjectDO::getTenantId, tenantId)
                .orderByDesc(CanvasProjectDO::getUpdatedAt);
        if (!superAdmin && !tenantAdmin) {
            Set<Long> projectIds = memberMapper.selectList(new LambdaQueryWrapper<CanvasProjectMemberDO>()
                            .eq(CanvasProjectMemberDO::getTenantId, tenantId)
                            .eq(CanvasProjectMemberDO::getUsername, requireText(username, "username")))
                    .stream()
                    .map(CanvasProjectMemberDO::getProjectId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (projectIds.isEmpty()) {
                return List.of();
            }
            wrapper.in(CanvasProjectDO::getId, projectIds);
        }
        return projectMapper.selectList(wrapper).stream()
                .map(this::summary)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectDetailResp create(Long tenantId, ProjectCreateReq req) {
        CanvasProjectDO row = new CanvasProjectDO();
        row.setTenantId(requireId(tenantId, "tenantId"));
        row.setProjectKey(normalizeKey(req.projectKey()));
        row.setProjectName(requireText(req.projectName(), "projectName"));
        row.setDescription(normalize(req.description()));
        row.setStatus(ACTIVE);
        row.setDefaultSettingsJson(normalize(req.defaultSettingsJson()));
        row.setRequireReviewBeforePublish(normalizeFlag(req.requireReviewBeforePublish()));
        row.setQuietHoursJson(normalize(req.quietHoursJson()));
        row.setCreatedBy(normalize(req.operator()));
        row.setUpdatedBy(normalize(req.operator()));
        projectMapper.insert(row);
        return detail(row);
    }

    public ProjectDetailResp detail(Long tenantId, Long projectId) {
        return detail(requireProject(tenantId, projectId));
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectDetailResp update(Long tenantId, Long projectId, ProjectUpdateReq req) {
        CanvasProjectDO row = requireProject(tenantId, projectId);
        row.setProjectName(requireText(req.projectName(), "projectName"));
        row.setDescription(normalize(req.description()));
        row.setDefaultSettingsJson(normalize(req.defaultSettingsJson()));
        row.setRequireReviewBeforePublish(normalizeFlag(req.requireReviewBeforePublish()));
        row.setQuietHoursJson(normalize(req.quietHoursJson()));
        row.setUpdatedBy(normalize(req.operator()));
        projectMapper.updateById(row);
        return detail(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disable(Long tenantId, Long projectId, String operator) {
        CanvasProjectDO row = requireProject(tenantId, projectId);
        row.setStatus(DISABLED);
        row.setUpdatedBy(normalize(operator));
        projectMapper.updateById(row);
    }

    public List<ProjectMemberResp> listMembers(Long tenantId, Long projectId) {
        requireProject(tenantId, projectId);
        return memberMapper.selectList(new LambdaQueryWrapper<CanvasProjectMemberDO>()
                        .eq(CanvasProjectMemberDO::getTenantId, tenantId)
                        .eq(CanvasProjectMemberDO::getProjectId, projectId)
                        .orderByAsc(CanvasProjectMemberDO::getUsername))
                .stream()
                .map(this::member)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectMemberResp setMember(Long tenantId, Long projectId, Long userId, ProjectMemberUpdateReq req) {
        CanvasProjectRole role = CanvasProjectRole.parse(req.role());
        String username = requireText(req.username(), "username");
        requireProject(tenantId, projectId);

        CanvasProjectMemberDO existing = memberMapper.selectOne(new LambdaQueryWrapper<CanvasProjectMemberDO>()
                .eq(CanvasProjectMemberDO::getTenantId, tenantId)
                .eq(CanvasProjectMemberDO::getProjectId, projectId)
                .eq(CanvasProjectMemberDO::getUsername, username)
                .last("LIMIT 1"));
        CanvasProjectMemberDO row = existing == null ? new CanvasProjectMemberDO() : existing;
        row.setTenantId(tenantId);
        row.setProjectId(projectId);
        row.setUserId(userId);
        row.setUsername(username);
        row.setRole(role.name());
        row.setSource(MANUAL);
        if (existing == null) {
            memberMapper.insert(row);
        } else {
            memberMapper.updateById(row);
        }
        return member(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long tenantId, Long projectId, Long userId) {
        requireProject(tenantId, projectId);
        memberMapper.delete(new LambdaQueryWrapper<CanvasProjectMemberDO>()
                .eq(CanvasProjectMemberDO::getTenantId, tenantId)
                .eq(CanvasProjectMemberDO::getProjectId, projectId)
                .eq(CanvasProjectMemberDO::getUserId, userId));
    }

    public ProjectStatsResp stats(Long tenantId, Long projectId) {
        CanvasProjectDO project = requireProject(tenantId, projectId);
        Long canvasCount = folderMapper.selectCount(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                .eq(CanvasProjectFolderDO::getTenantId, tenantId)
                .eq(CanvasProjectFolderDO::getProjectId, projectId));
        List<Long> canvasIds = assignedCanvasIds(project.getTenantId(), project.getId());
        if (canvasIds.isEmpty()) {
            return new ProjectStatsResp(projectId, canvasCount, 0L, 0L, 0L, 0L);
        }

        Long publishedCount = canvasMapper.selectCount(new LambdaQueryWrapper<CanvasDO>()
                .eq(CanvasDO::getTenantId, tenantId)
                .in(CanvasDO::getId, canvasIds)
                .eq(CanvasDO::getStatus, CanvasStatusEnum.PUBLISHED.getCode()));
        List<CanvasExecutionStatsDO> rows = executionStatsMapper.selectList(
                new LambdaQueryWrapper<CanvasExecutionStatsDO>()
                        .in(CanvasExecutionStatsDO::getCanvasId, canvasIds)
                        .ge(CanvasExecutionStatsDO::getStatDate, LocalDate.now().minusDays(6)));
        Long executionCount = rows.stream()
                .mapToLong(row -> nullToZero(row.getTotalCount()))
                .sum();
        Long failedExecutionCount = rows.stream()
                .mapToLong(row -> nullToZero(row.getFailCount()))
                .sum();
        Long totalDuration = rows.stream()
                .filter(row -> row.getAvgDurationMs() != null)
                .mapToLong(row -> nullToZero(row.getTotalCount()) * row.getAvgDurationMs())
                .sum();
        Long durationWeight = rows.stream()
                .filter(row -> row.getAvgDurationMs() != null)
                .mapToLong(row -> nullToZero(row.getTotalCount()))
                .sum();
        Long avgDuration = durationWeight == 0L ? 0L : totalDuration / durationWeight;
        return new ProjectStatsResp(projectId, canvasCount, publishedCount,
                executionCount, failedExecutionCount, avgDuration);
    }

    private List<Long> assignedCanvasIds(Long tenantId, Long projectId) {
        return folderMapper.selectList(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                        .eq(CanvasProjectFolderDO::getTenantId, tenantId)
                        .eq(CanvasProjectFolderDO::getProjectId, projectId))
                .stream()
                .map(CanvasProjectFolderDO::getCanvasId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private CanvasProjectDO requireProject(Long tenantId, Long projectId) {
        CanvasProjectDO row = projectMapper.selectOne(new LambdaQueryWrapper<CanvasProjectDO>()
                .eq(CanvasProjectDO::getTenantId, requireId(tenantId, "tenantId"))
                .eq(CanvasProjectDO::getId, requireId(projectId, "projectId"))
                .last("LIMIT 1"));
        if (row == null) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }
        return row;
    }

    private ProjectSummaryResp summary(CanvasProjectDO row) {
        Long canvasCount = folderMapper.selectCount(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                .eq(CanvasProjectFolderDO::getTenantId, row.getTenantId())
                .eq(CanvasProjectFolderDO::getProjectId, row.getId()));
        Integer memberCount = Math.toIntExact(memberMapper.selectCount(new LambdaQueryWrapper<CanvasProjectMemberDO>()
                .eq(CanvasProjectMemberDO::getTenantId, row.getTenantId())
                .eq(CanvasProjectMemberDO::getProjectId, row.getId())));
        return new ProjectSummaryResp(row.getId(), row.getTenantId(), row.getProjectKey(), row.getProjectName(),
                row.getDescription(), row.getStatus(), memberCount, canvasCount);
    }

    private ProjectDetailResp detail(CanvasProjectDO row) {
        return new ProjectDetailResp(row.getId(), row.getTenantId(), row.getProjectKey(), row.getProjectName(),
                row.getDescription(), row.getStatus(), row.getDefaultSettingsJson(),
                row.getRequireReviewBeforePublish(), row.getQuietHoursJson());
    }

    private ProjectMemberResp member(CanvasProjectMemberDO row) {
        return new ProjectMemberResp(row.getId(), row.getTenantId(), row.getProjectId(), row.getUserId(),
                row.getUsername(), row.getRole(), row.getSource());
    }

    private String normalizeKey(String value) {
        String trimmed = requireText(value, "projectKey").trim().toLowerCase();
        String normalized = trimmed.replaceAll("[^a-z0-9_-]+", "-").replaceAll("(^-|-$)", "");
        return requireText(normalized, "projectKey");
    }

    private String requireText(String value, String field) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private Long requireId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private Integer normalizeFlag(Integer value) {
        return value == null || value == 0 ? 0 : 1;
    }

    private long nullToZero(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
