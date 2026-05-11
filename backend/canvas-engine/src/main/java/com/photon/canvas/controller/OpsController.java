package com.photon.canvas.controller;

import com.photon.canvas.common.R;
import com.photon.canvas.domain.canvas.*;
import com.photon.canvas.domain.approval.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 运营工具 API（设计文档第二十三章）：
 * - 画布模板管理
 * - 发布审批流
 */
@RestController
@RequiredArgsConstructor
public class OpsController {

    private final CanvasTemplateMapper  templateMapper;
    private final CanvasMapper          canvasMapper;
    private final CanvasManualApprovalMapper approvalMapper;

    // ── 画布模板（23.1节） ─────────────────────────────────────────

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
            tpl.setGraphJson(canvas.getDescription()); // 实际应取 graphJson，简化
            tpl.setIsOfficial(0);
            tpl.setUseCount(0);
            tpl.setCreatedBy("current_user");
            templateMapper.insert(tpl);
            return tpl;
        }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
    }

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
