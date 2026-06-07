package org.chovy.canvas.domain.approval;

import org.chovy.canvas.dal.dataobject.ApprovalDefinitionDO;
import org.chovy.canvas.dal.dataobject.ApprovalInstanceDO;
import org.chovy.canvas.dal.dataobject.ApprovalTaskDO;

import java.util.List;

public interface ApprovalExternalProvider {

    boolean supports(String provider);

    ApprovalExternalSubmissionResult submit(ApprovalDefinitionDO definition,
                                            ApprovalInstanceDO instance,
                                            List<ApprovalTaskDO> tasks,
                                            ApprovalSubmitCommand command);

    void decide(ApprovalDefinitionDO definition,
                ApprovalInstanceDO instance,
                ApprovalTaskDO task,
                ApprovalDecisionCommand command,
                boolean approve);

    default ApprovalExternalSyncResult sync(ApprovalDefinitionDO definition, ApprovalInstanceDO instance) {
        return null;
    }
}
