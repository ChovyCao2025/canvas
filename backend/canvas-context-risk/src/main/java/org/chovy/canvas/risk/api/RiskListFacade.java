package org.chovy.canvas.risk.api;

import java.util.List;

public interface RiskListFacade {

    List<RiskListView> listLists(Long tenantId);
}
