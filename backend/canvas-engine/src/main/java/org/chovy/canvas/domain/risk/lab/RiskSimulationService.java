package org.chovy.canvas.domain.risk.lab;

import org.chovy.canvas.config.CanvasRuntimeMetrics;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionAction;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionRunRecord;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 风控仿真服务，在不激活运行时策略的前提下比较基线决策和候选版本。
 */
public class RiskSimulationService {

    private final RiskSimulationSampleRepository sampleRepository;
    private final RiskSimulationActivationGuard activationGuard;
    private final CanvasRuntimeMetrics metrics;
    private final RiskSimulationRunRepository runRepository;

    /**
     * 创建不记录指标的仿真服务。
     */
    public RiskSimulationService(RiskSimulationSampleRepository sampleRepository,
                                 RiskSimulationActivationGuard activationGuard) {
        this(sampleRepository, activationGuard, null, new InMemoryRiskSimulationRunRepository());
    }

    /**
     * 创建仿真服务，并注入样本仓储、激活保护和可选指标组件。
     */
    public RiskSimulationService(RiskSimulationSampleRepository sampleRepository,
                                 RiskSimulationActivationGuard activationGuard,
                                 CanvasRuntimeMetrics metrics) {
        this(sampleRepository, activationGuard, metrics, new InMemoryRiskSimulationRunRepository());
    }

    /**
     * 创建仿真服务，并注入样本仓储、激活保护、指标和仿真运行仓储。
     */
    public RiskSimulationService(RiskSimulationSampleRepository sampleRepository,
                                 RiskSimulationActivationGuard activationGuard,
                                 CanvasRuntimeMetrics metrics,
                                 RiskSimulationRunRepository runRepository) {
        this.sampleRepository = sampleRepository;
        this.activationGuard = activationGuard;
        this.metrics = metrics;
        this.runRepository = runRepository == null ? new InMemoryRiskSimulationRunRepository() : runRepository;
    }

    /**
     * 执行一次离线仿真，统计候选版本相对基线的动作分布变化。
     */
    public RiskSimulationResult run(RiskSimulationRequest request) {
        Instant startedAt = Instant.now();
        validate(request);
        List<RiskDecisionRunRecord> samples = sampleRepository.findSamples(
                request.tenantId(), request.sceneKey(), request.sampleLimit());
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("simulation sample source is required");
        }

        Map<RiskDecisionAction, Integer> distribution = new EnumMap<>(RiskDecisionAction.class);
        Map<String, Integer> changes = new LinkedHashMap<>();
        int changed = 0;
        for (RiskDecisionRunRecord sample : samples) {
            RiskDecisionAction baseline = sample.response().action();
            distribution.merge(baseline, 1, Integer::sum);
            RiskDecisionAction candidate = sampleRepository.evaluateCandidate(
                    sample, request.strategyKey(), request.candidateVersion());
            if (candidate != baseline) {
                changed++;
                changes.merge(baseline.name() + "->" + candidate.name(), 1, Integer::sum);
            }
        }

        // 显式保留激活依赖但不调用：仿真流程不能修改活跃运行时策略状态。
        if (activationGuard == null) {
            throw new IllegalStateException("activation guard is required");
        }

        RiskSimulationResult result = new RiskSimulationResult(
                simulationId(request),
                RiskSimulationStatus.COMPLETED,
                samples.size(),
                Map.copyOf(distribution),
                changed,
                Map.copyOf(changes));
        if (metrics != null) {
            metrics.recordRiskSimulationRun(request.sceneKey(), result.status().name(), result.sampleSize(),
                    Duration.between(startedAt, Instant.now()).toMillis());
        }
        runRepository.save(request, result);
        return result;
    }

    /**
     * 查询仿真历史。
     */
    public List<RiskSimulationHistoryView> listRuns(Long tenantId, String sceneKey, int limit) {
        return runRepository.list(tenantId, sceneKey, limit);
    }

    /**
     * 校验仿真请求的租户、场景、策略和样本数量。
     */
    private void validate(RiskSimulationRequest request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request == null) {
            throw new IllegalArgumentException("simulation request is required");
        }
        if (request.tenantId() == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (request.sceneKey() == null || request.sceneKey().isBlank()) {
            throw new IllegalArgumentException("sceneKey is required");
        }
        if (request.strategyKey() == null || request.strategyKey().isBlank()) {
            throw new IllegalArgumentException("strategyKey is required");
        }
        if (request.sampleLimit() <= 0) {
            throw new IllegalArgumentException("sampleLimit must be positive");
        }
    }

    /**
     * 基于请求关键字段生成稳定仿真编号。
     */
    private String simulationId(RiskSimulationRequest request) {
        return "sim-" + hash(request.tenantId() + ":" + request.sceneKey() + ":"
                + request.strategyKey() + ":" + request.baselineVersion() + ":"
                + request.candidateVersion() + ":" + request.sampleLimit()).substring(0, 12);
    }

    /**
     * 计算 SHA-256 十六进制哈希。
     */
    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }
}
