package org.chovy.canvas.service;

import org.chovy.canvas.dto.EventReportReq;

import java.util.Map;

public interface EventDefinitionService {

    Map<String, Object> doReportEvent(EventReportReq req);
}
