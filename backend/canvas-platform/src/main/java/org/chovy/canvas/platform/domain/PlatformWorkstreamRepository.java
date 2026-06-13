package org.chovy.canvas.platform.domain;

import java.util.List;

public interface PlatformWorkstreamRepository {

    List<PlatformWorkstream> list();

    PlatformWorkstream get(String workstreamKey);
}
