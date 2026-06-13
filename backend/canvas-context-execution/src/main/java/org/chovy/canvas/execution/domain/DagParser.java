package org.chovy.canvas.execution.domain;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;

public class DagParser {

    private final DagRuntimeService runtimeService;

    public DagParser() {
        this(new DagRuntimeService());
    }

    public DagParser(DagRuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public DagGraph parse(PublishedCanvasDefinition definition) {
        return runtimeService.validate(definition);
    }
}
