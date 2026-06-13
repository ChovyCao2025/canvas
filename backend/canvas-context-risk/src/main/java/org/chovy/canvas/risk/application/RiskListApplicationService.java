package org.chovy.canvas.risk.application;

import java.util.List;
import java.util.Objects;

import org.chovy.canvas.risk.api.RiskListFacade;
import org.chovy.canvas.risk.api.RiskListView;
import org.chovy.canvas.risk.domain.governance.RiskListRepository;
import org.springframework.stereotype.Service;

@Service
public class RiskListApplicationService implements RiskListFacade {

    private final RiskListRepository repository;

    public RiskListApplicationService(RiskListRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RiskListView> listLists(Long tenantId) {
        Objects.requireNonNull(tenantId, "tenantId");
        return repository.listLists(tenantId);
    }
}
