package org.chovy.canvas.domain.approval;

import org.chovy.canvas.dal.dataobject.ApprovalInstanceDO;

public interface ApprovalAutoActionHandler {

    boolean supports(String autoAction);

    default boolean supportsStatus(String autoAction, String status) {
        return ApprovalWorkflowService.STATUS_APPROVED.equalsIgnoreCase(status);
    }

    void execute(ApprovalInstanceDO instance, String actor);
}
