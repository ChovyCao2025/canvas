package org.chovy.canvas.canvas.application.dsl;

import java.util.List;

import org.chovy.canvas.canvas.api.dsl.CanvasDslDocument;

/**
 * 定义CanvasDslMappingService对外提供的能力契约。
 */
public interface CanvasDslMappingService {

    /**
     * 转换为graphJSON 内容。
     */
    MappingResult toGraphJson(CanvasDslDocument document);

    /**
     * 处理from graphJSON 内容。
     */
    CanvasDslDocument fromGraphJson(String graphJson);

    /**
     * 处理inspectUnsupportedExportSemantics。
     */
    List<CanvasDslValidationResult.Violation> inspectUnsupportedExportSemantics(String graphJson);

    /**
     * 处理diff。
     */
    DiffResult diff(CanvasDslDocument source, CanvasDslDocument target);

    /**
     * 承载MappingResult的数据快照。
     */
    record MappingResult(String templateKey, String graphJson) {
    }

    /**
     * 承载DiffResult的数据快照。
     */
    record DiffResult(boolean changed, List<DiffChange> changes) {

        public DiffResult {
            changes = List.copyOf(changes == null ? List.of() : changes);
        }
    }

    /**
     * 承载DiffChange的数据快照。
     */
    record DiffChange(String code, String path, String before, String after) {
    }
}
