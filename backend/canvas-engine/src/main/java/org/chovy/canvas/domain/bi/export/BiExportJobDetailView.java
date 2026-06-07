package org.chovy.canvas.domain.bi.export;

public record BiExportJobDetailView(
        BiExportJobView job,
        BiExportJobCommand request) {
}
