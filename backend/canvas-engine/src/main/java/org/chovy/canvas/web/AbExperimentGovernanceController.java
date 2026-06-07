package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.AbExperimentGovernanceService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/ab-experiments/{experimentId}/governance")
public class AbExperimentGovernanceController {

    private final AbExperimentGovernanceService service;

    public AbExperimentGovernanceController(AbExperimentGovernanceService service) {
        this.service = service;
    }

    @PostMapping("/evaluate")
    public Mono<R<AbExperimentGovernanceService.Evaluation>> evaluate(
            @PathVariable Long experimentId,
            @RequestParam(defaultValue = "A") String controlVariantKey) {
        return Mono.fromCallable(() -> R.ok(service.evaluate(experimentId, controlVariantKey)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
