package org.chovy.canvas.risk.domain.governance;

import java.util.List;

import org.chovy.canvas.risk.api.RiskListView;

public interface RiskListRepository {

    List<RiskListView> listLists(Long tenantId);
}
