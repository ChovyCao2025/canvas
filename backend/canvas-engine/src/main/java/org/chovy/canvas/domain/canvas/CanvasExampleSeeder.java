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

    /** 官方示例导入时写入的创建人标识。 */
    static final String CREATED_BY = "example-seed";
    /** 历史单租户迁移创建的默认租户 ID。 */
    static final Long DEFAULT_TENANT_ID = 1L;

    /** 画布模板 Mapper，用于读取启用的官方模板。 */
    private final CanvasTemplateMapper templateMapper;
    /** 画布 Mapper，用于幂等创建示例画布。 */
    private final CanvasMapper canvasMapper;
    /** 画布版本 Mapper，用于写入示例的初始草稿版本。 */
    private final CanvasVersionMapper canvasVersionMapper;
    /** Jackson ObjectMapper，用于校验模板 graphJson 是否可解析。 */
    private final ObjectMapper objectMapper;
    /** 示例画布配置属性，控制启动导入开关。 */
    private final CanvasExamplesProperties properties;

    /**
     * 执行 run 对应的业务逻辑。
     *
     * <p>该方法在事务边界内执行，确保相关数据库写入保持一致。
     *
     * @param args args 方法执行所需的业务参数
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("Official canvas examples are disabled; skip import.");
            return;
        }

        // 启动时只导入启用的官方模板，后续按 sourceTemplateKey 做幂等保护。
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

    /**
     * 执行 import Template 对应的业务逻辑。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param template template 方法执行所需的业务参数
     */
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
            // 已导入过的官方模板不再覆盖，避免启动任务改写用户可能已查看或复制的示例数据。
            return;
        }

        CanvasDO canvas = new CanvasDO();
        canvas.setTenantId(DEFAULT_TENANT_ID);
        canvas.setName(template.getName());
        canvas.setDescription(template.getDescription());
        canvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
        canvas.setCreatedBy(CREATED_BY);
        canvas.setIsExample(1);
        canvas.setSourceTemplateKey(template.getTemplateKey());
        canvasMapper.insert(canvas);

        // 示例仅创建草稿版本，不注册触发路由，也不进入发布态执行链路。
        CanvasVersionDO version = new CanvasVersionDO();
        version.setTenantId(DEFAULT_TENANT_ID);
        version.setCanvasId(canvas.getId());
        version.setVersion(1);
        version.setGraphJson(template.getGraphJson());
        version.setStatus(VersionStatus.DRAFT.getCode());
        version.setCreatedBy(CREATED_BY);
        canvasVersionMapper.insert(version);
    }

    /**
     * 判断 is Valid Graph 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param template template 方法执行所需的业务参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean isValidGraph(CanvasTemplateDO template) {
        try {
            objectMapper.readTree(template.getGraphJson());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
