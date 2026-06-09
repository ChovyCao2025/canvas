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

/**
 * 画布用户 Query CDP 领域服务。
 *
 * <p>负责用户画像、身份、标签和画布参与记录等客户数据能力，为画布执行和管理端查询提供统一入口。
 * <p>该层隔离 CDP 数据结构与上层业务，集中处理状态、历史和幂等语义。
 */
@Service
@RequiredArgsConstructor
public class CanvasUserQueryService {

    /** 画布执行 Mapper，用于聚合用户在画布中的执行历史。 */
    private final CanvasExecutionMapper executionMapper;
    /** CDP 标签服务。 */
    private final CdpTagService tagService;
    /** CDP 用户服务。 */
    private final CdpUserService userService;

    /** 查询用户列表或指定画布下的用户聚合视图。 */
    public List<CanvasUserRowDTO> listUsers(Long canvasId) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<CanvasExecutionDO> executions = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecutionDO>()
                        .eq(CanvasExecutionDO::getCanvasId, canvasId)
                        .isNotNull(CanvasExecutionDO::getUserId)
                        .orderByDesc(CanvasExecutionDO::getCreatedAt));
        Map<String, List<CanvasExecutionDO>> byUser = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CanvasExecutionDO execution : executions) {
            byUser.computeIfAbsent(execution.getUserId(), ignored -> new java.util.ArrayList<>()).add(execution);
        }
        return byUser.entrySet().stream()
                .map(entry -> toRow(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CanvasUserRowDTO::lastEnteredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** 查询指定用户在指定画布中的执行聚合信息。 */
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

    /** 查询指定用户在指定画布下的执行记录。 */
    public List<CanvasExecutionDO> listExecutions(Long canvasId, String userId) {
        return executionMapper.selectList(new LambdaQueryWrapper<CanvasExecutionDO>()
                .eq(CanvasExecutionDO::getCanvasId, canvasId)
                .eq(CanvasExecutionDO::getUserId, userId)
                .orderByDesc(CanvasExecutionDO::getCreatedAt)
                .last("LIMIT 100"));
    }

    /** 将同一用户的执行记录汇总为管理端列表行数据。 */
    private CanvasUserRowDTO toRow(String userId, List<CanvasExecutionDO> executions) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /** 将执行状态码转换为前端展示使用的状态文本。 */
    private String statusLabel(Integer status) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return String.valueOf(status);
    }
}
