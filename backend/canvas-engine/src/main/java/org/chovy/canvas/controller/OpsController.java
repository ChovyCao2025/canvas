package org.chovy.canvas.controller;

import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.canvas.*;
import org.chovy.canvas.domain.approval.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 运营工具 API（设计文档第二十三章）
 */
@RestController
@RequiredArgsConstructor
public class OpsController {

    private final CanvasTemplateMapper       templateMapper;
    private final CanvasMapper               canvasMapper;
    private final CanvasVersionMapper        canvasVersionMapper;
    private final CanvasManualApprovalMapper approvalMapper;

    // ── 画布模板（23.1节） ─────────────────────────────────────────

    /**
     * 获取画布模板列表
     * @param category 模板分类（可选）
     * @return 模板列表
     */
    @GetMapping("/canvas/templates")
    public Mono<R<List<CanvasTemplate>>> listTemplates(
            @RequestParam(required = false) String category) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<CanvasTemplate> q = new LambdaQueryWrapper<CanvasTemplate>()
                    .orderByDesc(CanvasTemplate::getUseCount);
            if (category != null) q.eq(CanvasTemplate::getCategory, category);
            return templateMapper.selectList(q);
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 将当前画布另存为模板
     * @param id 画布 ID
     * @param req 模板信息（名称、分类等）
     * @return 模板对象
     */
    @PostMapping("/canvas/{id}/save-as-template")
    public Mono<R<CanvasTemplate>> saveAsTemplate(
            @PathVariable Long id,
            @RequestBody SaveTemplateReq req) {
        return Mono.fromCallable(() -> {
            Canvas canvas = canvasMapper.selectById(id);
            if (canvas == null) throw new IllegalArgumentException("画布不存在");
            CanvasTemplate tpl = new CanvasTemplate();
            tpl.setName(req.getName() != null ? req.getName() : canvas.getName() + " 模板");
            tpl.setDescription(req.getDescription());
            tpl.setCategory(req.getCategory());
            // 获取最新草稿的 graphJson
            var draft = canvasVersionMapper.selectOne(
                    new LambdaQueryWrapper<CanvasVersion>()
                            .eq(CanvasVersion::getCanvasId, id)
                            .eq(CanvasVersion::getStatus, 0)
                            .orderByDesc(CanvasVersion::getVersion).last("LIMIT 1"));
            tpl.setGraphJson(draft != null ? draft.getGraphJson() : "{\"nodes\":[]}");
            tpl.setIsOfficial(0);
            tpl.setUseCount(0);
            tpl.setCreatedBy("current_user");
            templateMapper.insert(tpl);
            return tpl;
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    /**
     * 基于模板创建新画布
     * @param templateId 模板 ID
     * @param req 画布名称
     * @return 新画布信息
     */
    @PostMapping("/canvas/from-template/{templateId}")
    public Mono<R<Canvas>> createFromTemplate(@PathVariable Long templateId,
                                               @RequestBody FromTemplateReq req) {
        return Mono.fromCallable(() -> {
            CanvasTemplate tpl = templateMapper.selectById(templateId);
            if (tpl == null) throw new IllegalArgumentException("模板不存在");

            Canvas canvas = new Canvas();
            canvas.setName(req.getName() != null ? req.getName() : tpl.getName() + " (副本)");
            canvas.setDescription(tpl.getDescription());
            canvas.setStatus(0);
            canvas.setCreatedBy("current_user");
            canvasMapper.insert(canvas);

            // 更新模板使用次数
            tpl.setUseCount(tpl.getUseCount() + 1);
            templateMapper.updateById(tpl);

            return canvas;
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

    // ── 发布审批（23.2节） ─────────────────────────────────────────

    /**
     * 获取待审批的发布请求列表
     * @return 审批记录列表
     */
    @GetMapping("/canvas/pending-reviews")
    public Mono<R<List<CanvasManualApproval>>> pendingReviews() {
        return Mono.fromCallable(() ->
                approvalMapper.selectList(
                        new LambdaQueryWrapper<CanvasManualApproval>()
                                .eq(CanvasManualApproval::getStatus, "PENDING")
                                .orderByAsc(CanvasManualApproval::getTimeoutAt))
        ).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }


    // ── DTOs ──────────────────────────────────────────────────────

    @Data static class SaveTemplateReq {
        private String name, description, category;
    }
    @Data static class FromTemplateReq {
        private String name;
    }
}
