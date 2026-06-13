package org.chovy.canvas.platform.api;

import java.util.List;

public interface PlatformWorkstreamFacade {

    List<WorkstreamStatusView> statuses();

    WorkstreamStatusView requireExecutableChildSpec(String workstreamKey);
}
