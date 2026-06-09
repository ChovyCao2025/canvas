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

/**
 * CanvasProjectService 编排 domain.project 场景的领域业务规则。
 */
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

    /**
     * 查询当前用户可见的项目列表。
     * 超级管理员可跨租户查看，租户管理员查看本租户全部项目，普通用户只返回自己作为项目成员的项目。
     */
    public List<ProjectSummaryResp> list(Long tenantId, String username, boolean superAdmin, boolean tenantAdmin) {
        LambdaQueryWrapper<CanvasProjectDO> wrapper = new LambdaQueryWrapper<CanvasProjectDO>()
                .eq(!superAdmin, CanvasProjectDO::getTenantId, tenantId)
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                .orderByDesc(CanvasProjectDO::getUpdatedAt);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!superAdmin && !tenantAdmin) {
            Set<Long> projectIds = memberMapper.selectList(new LambdaQueryWrapper<CanvasProjectMemberDO>()
                            .eq(CanvasProjectMemberDO::getTenantId, tenantId)
                            .eq(CanvasProjectMemberDO::getUsername, requireText(username, "username")))
                    // 遍历候选数据并按业务规则筛选、转换或聚合。
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
    /**
     * 在指定租户下创建 Canvas 项目。
     * 方法会规范化项目 key、默认发布审核和静默时间配置，并记录创建/更新操作者。
     */
    public ProjectDetailResp create(Long tenantId, ProjectCreateReq req) {
        // 准备本次处理所需的上下文和中间变量。
        CanvasProjectDO row = new CanvasProjectDO();
        row.setTenantId(requireId(tenantId, "tenantId"));
        row.setProjectKey(normalizeKey(req.projectKey()));
        row.setProjectName(requireText(req.projectName(), "projectName"));
        row.setDescription(normalize(req.description()));
        row.setStatus(ACTIVE);
        row.setDefaultSettingsJson(normalize(req.defaultSettingsJson()));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setRequireReviewBeforePublish(normalizeFlag(req.requireReviewBeforePublish()));
        row.setQuietHoursJson(normalize(req.quietHoursJson()));
        row.setCreatedBy(normalize(req.operator()));
        row.setUpdatedBy(normalize(req.operator()));
        projectMapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return detail(row);
    }

    /**
     * 查询租户内项目详情。
     * 找不到或项目不属于该租户时由 requireProject 抛出异常。
     */
    public ProjectDetailResp detail(Long tenantId, Long projectId) {
        return detail(requireProject(tenantId, projectId));
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 更新租户内项目基础配置。
     * 会覆盖名称、描述、默认设置、发布前审核策略和静默时间，并记录更新人。
     */
    public ProjectDetailResp update(Long tenantId, Long projectId, ProjectUpdateReq req) {
        // 准备本次处理所需的上下文和中间变量。
        CanvasProjectDO row = requireProject(tenantId, projectId);
        row.setProjectName(requireText(req.projectName(), "projectName"));
        row.setDescription(normalize(req.description()));
        row.setDefaultSettingsJson(normalize(req.defaultSettingsJson()));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setRequireReviewBeforePublish(normalizeFlag(req.requireReviewBeforePublish()));
        row.setQuietHoursJson(normalize(req.quietHoursJson()));
        row.setUpdatedBy(normalize(req.operator()));
        projectMapper.updateById(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return detail(row);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 禁用租户内项目。
     * 项目记录会被置为 DISABLED，权限服务随后会阻止除读取和执行外的项目操作。
     */
    public void disable(Long tenantId, Long projectId, String operator) {
        CanvasProjectDO row = requireProject(tenantId, projectId);
        row.setStatus(DISABLED);
        row.setUpdatedBy(normalize(operator));
        projectMapper.updateById(row);
    }

    /**
     * 查询租户内项目成员列表。
     * 先校验项目归属，再按用户名排序返回成员角色和来源。
     */
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
    /**
     * 新增或更新项目成员角色。
     * 成员以租户、项目和用户名唯一定位，写入手工维护来源，返回更新后的成员视图。
     */
    public ProjectMemberResp setMember(Long tenantId, Long projectId, Long userId, ProjectMemberUpdateReq req) {
        // 准备本次处理所需的上下文和中间变量。
        CanvasProjectRole role = CanvasProjectRole.parse(req.role());
        String username = requireText(req.username(), "username");
        requireProject(tenantId, projectId);

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return member(row);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 从租户项目中移除指定用户成员关系。
     * 只删除匹配租户、项目和 userId 的成员记录，不影响用户本身。
     */
    public void removeMember(Long tenantId, Long projectId, Long userId) {
        requireProject(tenantId, projectId);
        memberMapper.delete(new LambdaQueryWrapper<CanvasProjectMemberDO>()
                .eq(CanvasProjectMemberDO::getTenantId, tenantId)
                .eq(CanvasProjectMemberDO::getProjectId, projectId)
                .eq(CanvasProjectMemberDO::getUserId, userId));
    }

    /**
     * 汇总租户内项目运行统计。
     * 统计项目下画布数量、已发布画布数、近 7 天执行和失败次数以及加权平均耗时。
     */
    public ProjectStatsResp stats(Long tenantId, Long projectId) {
        CanvasProjectDO project = requireProject(tenantId, projectId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        Long canvasCount = folderMapper.selectCount(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                .eq(CanvasProjectFolderDO::getTenantId, tenantId)
                .eq(CanvasProjectFolderDO::getProjectId, projectId));
        List<Long> canvasIds = assignedCanvasIds(project.getTenantId(), project.getId());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param projectId 业务对象 ID，用于定位具体记录。
     * @return 返回 assigned canvas ids 汇总后的集合、分页或映射视图。
     */
    private List<Long> assignedCanvasIds(Long tenantId, Long projectId) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return folderMapper.selectList(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                        .eq(CanvasProjectFolderDO::getTenantId, tenantId)
                        .eq(CanvasProjectFolderDO::getProjectId, projectId))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(CanvasProjectFolderDO::getCanvasId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param projectId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireProject 流程生成的业务结果。
     */
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 summary 流程生成的业务结果。
     */
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 detail 流程生成的业务结果。
     */
    private ProjectDetailResp detail(CanvasProjectDO row) {
        return new ProjectDetailResp(row.getId(), row.getTenantId(), row.getProjectKey(), row.getProjectName(),
                row.getDescription(), row.getStatus(), row.getDefaultSettingsJson(),
                row.getRequireReviewBeforePublish(), row.getQuietHoursJson());
    }

    /**
     * 执行 member 流程，围绕 member 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 member 流程生成的业务结果。
     */
    private ProjectMemberResp member(CanvasProjectMemberDO row) {
        return new ProjectMemberResp(row.getId(), row.getTenantId(), row.getProjectId(), row.getUserId(),
                row.getUsername(), row.getRole(), row.getSource());
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeKey(String value) {
        String trimmed = requireText(value, "projectKey").trim().toLowerCase();
        String normalized = trimmed.replaceAll("[^a-z0-9_-]+", "-").replaceAll("(^-|-$)", "");
        return requireText(normalized, "projectKey");
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 require text 生成的文本或业务键。
     */
    private String requireText(String value, String field) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 require id 计算得到的数量、金额或指标值。
     */
    private Long requireId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Integer normalizeFlag(Integer value) {
        return value == null || value == 0 ? 0 : 1;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to zero 计算得到的数量、金额或指标值。
     */
    private long nullToZero(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
