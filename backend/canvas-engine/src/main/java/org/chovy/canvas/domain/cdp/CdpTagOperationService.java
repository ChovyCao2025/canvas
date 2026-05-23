package org.chovy.canvas.domain.cdp;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dto.cdp.CdpBatchTagReq;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CdpTagOperationService {

    private final CdpTagOperationMapper operationMapper;
    private final CdpTagService tagService;

    public CdpTagOperation create(CdpBatchTagReq req) {
        List<String> userIds = req.userIds() == null ? List.of() : req.userIds();
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("批量打标用户不能为空");
        }
        CdpTagOperation op = new CdpTagOperation();
        op.setOperationType(req.operationType());
        op.setTagCode(req.tagCode());
        op.setTagValue(req.tagValue());
        op.setTotalCount(userIds.size());
        op.setSuccessCount(0);
        op.setFailCount(0);
        op.setStatus("RUNNING");
        op.setCreatedBy(req.operator());
        operationMapper.insert(op);

        Thread.ofVirtual().start(() -> run(op, userIds, req));
        return op;
    }

    public CdpTagOperation get(Long id) {
        CdpTagOperation op = operationMapper.selectById(id);
        if (op == null) {
            throw new IllegalArgumentException("批量标签任务不存在: " + id);
        }
        return op;
    }

    public List<CdpTagOperation> listRecent(int limit) {
        return operationMapper.selectList(new LambdaQueryWrapper<CdpTagOperation>()
                .orderByDesc(CdpTagOperation::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100))));
    }

    public CdpTagOperation retryFailed(Long id, String operator) {
        CdpTagOperation existing = get(id);
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

    private void run(CdpTagOperation op, List<String> userIds, CdpBatchTagReq req) {
        int success = 0;
        int fail = 0;
        StringBuilder errors = new StringBuilder();
        for (String userId : userIds) {
            try {
                if ("BATCH_REMOVE".equals(req.operationType())) {
                    tagService.removeTag(userId, req.tagCode(), req.reason(), req.operator());
                } else {
                    tagService.setTag(userId, new CdpTagWriteReq(req.tagCode(), req.tagValue(),
                            req.reason(), null, "BATCH", String.valueOf(op.getId()),
                            req.operator(), op.getId() + ":" + userId + ":" + req.tagCode()));
                }
                success++;
            } catch (RuntimeException e) {
                fail++;
                if (errors.length() < 900) {
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
