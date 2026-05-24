package org.chovy.canvas.domain.task;

import org.chovy.canvas.dal.dataobject.AsyncTaskDO;


public record AsyncTaskCreateResult(AsyncTaskDO task, boolean created) {
}
