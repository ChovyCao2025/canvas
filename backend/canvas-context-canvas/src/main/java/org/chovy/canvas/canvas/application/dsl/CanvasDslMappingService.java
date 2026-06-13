package org.chovy.canvas.canvas.application.dsl;

import java.util.List;

import org.chovy.canvas.canvas.api.dsl.CanvasDslDocument;

public interface CanvasDslMappingService {

    MappingResult toGraphJson(CanvasDslDocument document);

    CanvasDslDocument fromGraphJson(String graphJson);

    List<CanvasDslValidationResult.Violation> inspectUnsupportedExportSemantics(String graphJson);

    DiffResult diff(CanvasDslDocument source, CanvasDslDocument target);

    record MappingResult(String templateKey, String graphJson) {
    }

    record DiffResult(boolean changed, List<DiffChange> changes) {

        public DiffResult {
            changes = List.copyOf(changes == null ? List.of() : changes);
        }
    }

    record DiffChange(String code, String path, String before, String after) {
    }
}
