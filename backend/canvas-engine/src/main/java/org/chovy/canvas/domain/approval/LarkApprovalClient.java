package org.chovy.canvas.domain.approval;

public interface LarkApprovalClient {

    String createInstance(LarkApprovalCreateInstanceRequest request);

    LarkApprovalInstanceSnapshot getInstance(Long tenantId, String instanceCode);

    void approveTask(LarkApprovalTaskActionRequest request);

    void rejectTask(LarkApprovalTaskActionRequest request);
}
