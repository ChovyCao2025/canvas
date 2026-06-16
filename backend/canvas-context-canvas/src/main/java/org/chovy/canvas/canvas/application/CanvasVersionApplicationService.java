package org.chovy.canvas.canvas.application;

import java.util.Comparator;
import java.util.List;

import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.CanvasVersionRepository;
import org.springframework.stereotype.Service;

/**
 * 封装CanvasVersionApplicationService相关的业务逻辑。
 */
@Service
public class CanvasVersionApplicationService {

    /**
     * 保存versionRepository。
     */
    private final CanvasVersionRepository versionRepository;

    /**
     * 创建当前对象实例。
     */
    public CanvasVersionApplicationService(CanvasVersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    /**
     * 获取Versions。
     */
    public List<CanvasVersion> getVersions(Long canvasId) {
        return versionRepository.findByCanvasId(canvasId).stream()
                .sorted(Comparator.comparing(CanvasVersion::version).reversed())
                .toList();
    }

    /**
     * 获取Version。
     */
    public CanvasVersion getVersion(Long versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("版本不存在: " + versionId));
    }
}
