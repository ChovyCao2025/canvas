package org.chovy.canvas.canvas.application;

import java.util.Comparator;
import java.util.List;

import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.CanvasVersionRepository;
import org.springframework.stereotype.Service;

@Service
public class CanvasVersionApplicationService {

    private final CanvasVersionRepository versionRepository;

    public CanvasVersionApplicationService(CanvasVersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    public List<CanvasVersion> getVersions(Long canvasId) {
        return versionRepository.findByCanvasId(canvasId).stream()
                .sorted(Comparator.comparing(CanvasVersion::version).reversed())
                .toList();
    }

    public CanvasVersion getVersion(Long versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("版本不存在: " + versionId));
    }
}
