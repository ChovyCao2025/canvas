package org.chovy.canvas.domain.cdp;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dto.cdp.CdpBatchTagReq;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.stream.Collectors;
import org.chovy.canvas.dal.dataobject.CdpTagOperationDO;
import org.chovy.canvas.dal.mapper.CdpTagOperationMapper;

/**
 * CDP 标签操作 CDP 领域服务。
 *
 * <p>负责用户画像、身份、标签和画布参与记录等客户数据能力，为画布执行和管理端查询提供统一入口。
 * <p>该层隔离 CDP 数据结构与上层业务，集中处理状态、历史和幂等语义。
 */
@Service
@RequiredArgsConstructor
public class CdpTagOperationService {

    /** 标签操作 Mapper，用于记录批量打标任务进度和结果。 */
    private final CdpTagOperationMapper operationMapper;
    /** CDP 标签服务。 */
    private final CdpTagService tagService;

    /** 创建新记录，并执行必要的唯一性、格式和默认值处理。 */
    public CdpTagOperationDO create(CdpBatchTagReq req) {
        List<String> userIds = req.userIds() == null ? List.of() : req.userIds();
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("批量打标用户不能为空");
        }
        CdpTagOperationDO op = new CdpTagOperationDO();
        op.setOperationType(req.operationType());
        op.setTagCode(req.tagCode());
        op.setTagValue(req.tagValue());
        op.setTotalCount(userIds.size());
        op.setSuccessCount(0);
        op.setFailCount(0);
        op.setStatus("RUNNING");
        op.setCreatedBy(req.operator());
        operationMapper.insert(op);

        // 批量打标可能耗时较长，创建操作记录后交给虚拟线程逐用户执行并回写统计。
        Thread.ofVirtual().start(() -> run(op, userIds, req));
        return op;
    }

    /** 按主键或业务键查询记录。 */
    public CdpTagOperationDO get(Long id) {
        CdpTagOperationDO op = operationMapper.selectById(id);
        if (op == null) {
            throw new IllegalArgumentException("批量标签任务不存在: " + id);
        }
        return op;
    }

    /** 查询最近的操作记录。 */
    public List<CdpTagOperationDO> listRecent(int limit) {
        return operationMapper.selectList(new LambdaQueryWrapper<CdpTagOperationDO>()
                .orderByDesc(CdpTagOperationDO::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100))));
    }

    /** 重试失败的批量标签操作。 */
    public CdpTagOperationDO retryFailed(Long id, String operator) {
        CdpTagOperationDO existing = get(id);
        List<String> failedUserIds = extractFailedUserIds(existing.getErrorMsg());
        if (failedUserIds.isEmpty()) {
            throw new IllegalArgumentException("任务没有可重试的失败用户: " + id);
        }
        String resolvedOperator = operator == null || operator.isBlank() ? existing.getCreatedBy() : operator.trim();
        return create(new CdpBatchTagReq(
                existing.getOperationType(),
                existing.getTagCode(),
                existing.getTagValue(),
                failedUserIds,
                "retry failed users from operation #" + id,
                resolvedOperator
        ));
    }

    /** 在线程中逐个执行批量打标/删标，并回写成功失败统计。 */
    private void run(CdpTagOperationDO op, List<String> userIds, CdpBatchTagReq req) {
        int success = 0;
        int fail = 0;
        StringBuilder errors = new StringBuilder();
        for (String userId : userIds) {
            try {
                if ("BATCH_REMOVE".equals(req.operationType())) {
                    tagService.removeTag(userId, req.tagCode(), req.reason(), req.operator());
                } else {
                    // 幂等键绑定 operationId + userId + tagCode，重试同一任务不会重复写历史。
                    tagService.setTag(userId, new CdpTagWriteReq(req.tagCode(), req.tagValue(),
                            req.reason(), null, "BATCH", String.valueOf(op.getId()),
                            req.operator(), op.getId() + ":" + userId + ":" + req.tagCode()));
                }
                success++;
            } catch (RuntimeException e) {
                fail++;
                if (errors.length() < 900) {
                    // 错误摘要控制长度，完整失败用户可通过重试解析前缀 userId。
                    errors.append(userId).append(": ").append(e.getMessage()).append("; ");
                }
            }
        }
        op.setSuccessCount(success);
        op.setFailCount(fail);
        op.setStatus(fail == 0 ? "SUCCESS" : "PARTIAL_FAILED");
        op.setErrorMsg(errors.isEmpty() ? null : errors.toString());
        operationMapper.updateById(op);
    }

    /** 从任务错误摘要中提取失败用户 ID，供失败重试复用。 */
    private List<String> extractFailedUserIds(String errorMsg) {
        if (errorMsg == null || errorMsg.isBlank()) {
            return List.of();
        }
        return Arrays.stream(errorMsg.split(";"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(item -> {
                    int idx = item.indexOf(':');
                    return idx > 0 ? item.substring(0, idx).trim() : "";
                })
                .filter(item -> !item.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
