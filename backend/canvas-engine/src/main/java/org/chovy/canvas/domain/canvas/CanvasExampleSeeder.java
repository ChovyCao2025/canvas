package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.VersionStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasTemplateDO;
import org.chovy.canvas.dal.mapper.CanvasTemplateMapper;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;

/**
 * Canvas Example Seeder 画布领域组件。
 *
 * <p>负责画布模板、示例、版本或生命周期相关业务能力，协调 Mapper、缓存、调度和执行引擎。
 * <p>该层承载画布域规则，控制器和基础设施代码不应绕过它直接修改核心状态。
 */
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

        List<CanvasTemplateDO> templates = templateMapper.selectList(
                new LambdaQueryWrapper<CanvasTemplateDO>()
                        .eq(CanvasTemplateDO::getIsOfficial, 1)
                        .eq(CanvasTemplateDO::getEnabled, 1)
                        .isNotNull(CanvasTemplateDO::getTemplateKey)
                        .orderByAsc(CanvasTemplateDO::getSortOrder)
        );

        for (CanvasTemplateDO template : templates) {
            importTemplate(template);
        }
    }

    private void importTemplate(CanvasTemplateDO template) {
        if (!isValidGraph(template)) {
            log.warn("Skip official canvas example template {} because graph_json is invalid.",
                    template.getTemplateKey());
            return;
        }

        CanvasDO existing = canvasMapper.selectOne(
                new LambdaQueryWrapper<CanvasDO>()
                        .eq(CanvasDO::getSourceTemplateKey, template.getTemplateKey())
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return;
        }

        CanvasDO canvas = new CanvasDO();
        canvas.setName(template.getName());
        canvas.setDescription(template.getDescription());
        canvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
        canvas.setCreatedBy(CREATED_BY);
        canvas.setIsExample(1);
        canvas.setSourceTemplateKey(template.getTemplateKey());
        canvasMapper.insert(canvas);

        CanvasVersionDO version = new CanvasVersionDO();
        version.setCanvasId(canvas.getId());
        version.setVersion(1);
        version.setGraphJson(template.getGraphJson());
        version.setStatus(VersionStatus.DRAFT.getCode());
        version.setCreatedBy(CREATED_BY);
        canvasVersionMapper.insert(version);
    }

    private boolean isValidGraph(CanvasTemplateDO template) {
        try {
            objectMapper.readTree(template.getGraphJson());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
