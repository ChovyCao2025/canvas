package org.chovy.canvas.domain.cdp;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dto.cdp.CdpBatchTagReq;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
