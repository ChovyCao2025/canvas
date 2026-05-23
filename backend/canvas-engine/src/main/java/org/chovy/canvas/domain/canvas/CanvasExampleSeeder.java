package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.VersionStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasExampleSeeder implements ApplicationRunner {

    static final String CREATED_BY = "example-seed";

    private final CanvasTemplateMapper templateMapper;
    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;
    private final ObjectMapper objectMapper;
    private final CanvasExamplesProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("Official canvas examples are disabled; skip import.");
            return;
        }

        List<CanvasTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<CanvasTemplate>()
                        .eq(CanvasTemplate::getIsOfficial, 1)
                        .eq(CanvasTemplate::getEnabled, 1)
                        .isNotNull(CanvasTemplate::getTemplateKey)
                        .orderByAsc(CanvasTemplate::getSortOrder)
        );

        for (CanvasTemplate template : templates) {
            importTemplate(template);
        }
    }

    private void importTemplate(CanvasTemplate template) {
        if (!isValidGraph(template)) {
            log.warn("Skip official canvas example template {} because graph_json is invalid.",
                    template.getTemplateKey());
            return;
        }

        Canvas existing = canvasMapper.selectOne(
                new LambdaQueryWrapper<Canvas>()
                        .eq(Canvas::getSourceTemplateKey, template.getTemplateKey())
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return;
        }

        Canvas canvas = new Canvas();
        canvas.setName(template.getName());
        canvas.setDescription(template.getDescription());
        canvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
        canvas.setCreatedBy(CREATED_BY);
        canvas.setIsExample(1);
        canvas.setSourceTemplateKey(template.getTemplateKey());
        canvasMapper.insert(canvas);

        CanvasVersion version = new CanvasVersion();
        version.setCanvasId(canvas.getId());
        version.setVersion(1);
        version.setGraphJson(template.getGraphJson());
        version.setStatus(VersionStatus.DRAFT.getCode());
        version.setCreatedBy(CREATED_BY);
        canvasVersionMapper.insert(version);
    }

    private boolean isValidGraph(CanvasTemplate template) {
        try {
            objectMapper.readTree(template.getGraphJson());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
