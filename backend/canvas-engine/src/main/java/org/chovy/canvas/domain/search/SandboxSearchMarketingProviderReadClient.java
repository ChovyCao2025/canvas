package org.chovy.canvas.domain.search;

import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.chovy.canvas.domain.providerwrite.ProviderWriteSandboxSupport;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * SandboxSearchMarketingProviderReadClient 编排 domain.search 场景的领域业务规则。
 */
@Component
public class SandboxSearchMarketingProviderReadClient implements SearchMarketingProviderReadClient {

    private static final Set<String> RUN_TYPES = Set.of(
            "PERFORMANCE",
            "SEO_TECHNICAL",
            "PROVIDER_STATE",
            "CHANGE_RECONCILIATION");

    /**
     * supports 处理 domain.search 场景的业务逻辑。
     * @param provider provider 参数，用于 supports 流程中的校验、计算或对象转换。
     * @param runType 类型标识，用于选择对应处理分支。
     * @return 返回 supports 的布尔判断结果。
     */
    @Override
    public boolean supports(String provider, String runType) {
        return ProviderWriteSandboxSupport.supportsSandboxProvider(provider)
                && RUN_TYPES.contains(normalize(runType));
    }

    /**
     * sync 创建或触发 domain.search 场景的业务处理。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param credential credential 参数，用于 sync 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    @Override
    public SearchMarketingProviderSyncResult sync(SearchMarketingSyncCommand command,
                                                  SearchMarketingCredentialRef credential) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            return SearchMarketingProviderSyncResult.failure(
                    "INVALID_SEARCH_SYNC_REQUEST",
                    "search marketing sync command is required",
                    false,
                    Map.of());
        }
        if (!supports(command.provider(), command.runType())) {
            return SearchMarketingProviderSyncResult.failure(
                    "UNSUPPORTED_SEARCH_SYNC_PROVIDER",
                    "sandbox search marketing read client does not support this provider or run type",
                    false,
                    Map.of("provider", command.provider(), "runType", command.runType()));
        }
        LocalDate start = command.windowStart() == null ? LocalDate.now().minusDays(1) : command.windowStart();
        LocalDate end = command.windowEnd() == null ? start : command.windowEnd();
        String seed = sha256(command.sourceId() + "|" + normalize(command.runType()) + "|" + start + "|" + end);
        long impressions = 1000L + Long.parseLong(seed.substring(0, 4), 16) % 5000L;
        long clicks = Math.max(1L, impressions / 20L);
        SearchMarketingPerformanceRow performance = new SearchMarketingPerformanceRow(
                "sandbox keyword " + command.sourceId(),
                "EXACT",
                "https://example.test/search/" + command.sourceId(),
                end,
                "ALL",
                "ALL",
                "SANDBOX",
                impressions,
                clicks,
                BigDecimal.valueOf(clicks).multiply(new BigDecimal("0.80")),
                Math.max(0L, clicks / 10L),
                BigDecimal.valueOf(Math.max(0L, clicks / 10L)).multiply(new BigDecimal("9.90")),
                new BigDecimal("3.2000"),
                Map.of("fixtureSeed", seed.substring(0, 12)));
        SearchMarketingUrlInspectionRow urlInspection = new SearchMarketingUrlInspectionRow(
                "https://example.test/search/" + command.sourceId(),
                end,
                "INDEXED",
                "CRAWLED",
                "https://example.test/search/" + command.sourceId(),
                "PRESENT",
                "PASS",
                LocalDateTime.of(end, java.time.LocalTime.NOON),
                Map.of("fixtureSeed", seed.substring(12, 24)));
        boolean seoOnly = "SEO_TECHNICAL".equals(normalize(command.runType()));
        Map<String, Object> evidence = ProviderWriteEvidenceSanitizer.sanitizeMap(Map.of(
                "adapter", "sandbox",
                "provider", command.provider(),
                "runType", command.runType(),
                "metadata", command.metadata(),
                "credential", credential.safeEvidence()));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return SearchMarketingProviderSyncResult.success(
                "sandbox-search-read-" + seed.substring(0, 16),
                seoOnly ? List.of() : List.of(performance),
                "PERFORMANCE".equals(normalize(command.runType())) ? List.of() : List.of(urlInspection),
                evidence);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 执行 sha256 流程，围绕 sha256 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sha256 生成的文本或业务键。
     */
    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash sandbox search marketing evidence", ex);
        }
    }
}
