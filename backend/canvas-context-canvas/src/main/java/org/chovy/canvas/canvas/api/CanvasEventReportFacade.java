package org.chovy.canvas.canvas.api;

import java.util.Map;

public interface CanvasEventReportFacade {

    Map<String, Object> report(String rawBody);
}
