package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.CdpUserIdentityDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseRealtimeRetryDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.dal.mapper.CdpUserIdentityMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.CdpUserTagMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseRealtimeRetryMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CdpWarehousePrivacyErasureExecutionService {

    private static final String PASS = "PASS";
    private static final String WARN = "WARN";
    private static final String FAIL = "FAIL";
    private static final String SKIPPED = "SKIPPED";
    private static final String USER_ID = "USER_ID";

    private final CdpWarehousePrivacyErasureService erasureService;
    private final CdpUserProfileMapper profileMapper;
    private final CdpUserIdentityMapper identityMapper;
    private final CdpUserTagMapper tagMapper;
    private final CdpEventLogMapper eventLogMapper;
    private final CdpWarehouseRealtimeRetryMapper retryMapper;
    private final CdpWarehouseDorisPrivacyErasureExecutor dorisExecutor;

    @Autowired
    public CdpWarehousePrivacyErasureExecutionService(
            CdpWarehousePrivacyErasureService erasureService,
            CdpUserProfileMapper profileMapper,
            CdpUserIdentityMapper identityMapper,
            CdpUserTagMapper tagMapper,
            CdpEventLogMapper eventLogMapper,
            CdpWarehouseRealtimeRetryMapper retryMapper,
            ObjectProvider<CdpWarehouseDorisPrivacyErasureExecutor> dorisExecutor) {
        this(erasureService, profileMapper, identityMapper, tagMapper, eventLogMapper, retryMapper,
                dorisExecutor == null ? null : dorisExecutor.getIfAvailable());
    }

    public CdpWarehousePrivacyErasureExecutionService(
            CdpWarehousePrivacyErasureService erasureService,
            CdpUserProfileMapper profileMapper,
            CdpUserIdentityMapper identityMapper,
            CdpUserTagMapper tagMapper,
            CdpEventLogMapper eventLogMapper,
            CdpWarehouseRealtimeRetryMapper retryMapper,
            CdpWarehouseDorisPrivacyErasureExecutor dorisExecutor) {
        this.erasureService = erasureService;
        this.profileMapper = profileMapper;
        this.identityMapper = identityMapper;
        this.tagMapper = tagMapper;
        this.eventLogMapper = eventLogMapper;
        this.retryMapper = retryMapper;
        this.dorisExecutor = dorisExecutor;
    }

    public ErasureExecutionResult execute(Long tenantId, Long requestId, ErasureExecutionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("erasure execution command is required");
        }
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        String subjectValue = required(command.subjectValue(), "subjectValue");
        CdpWarehousePrivacyErasureService.ErasureRequestView request =
                erasureService.get(scopedTenantId, requestId);
        String subjectType = normalize(request.subjectType());
        String expectedHash = subjectHash(scopedTenantId, subjectType, subjectValue);
        if (!expectedHash.equalsIgnoreCase(request.subjectHash())) {
            throw new IllegalArgumentException("subjectValue does not match erasure request subject hash");
        }

        List<CdpEventLogDO> matchingEvents = safe(eventLogMapper.selectList(eventQuery(scopedTenantId, subjectType, subjectValue)));
        List<Long> matchingEventIds = matchingEvents.stream()
                .map(CdpEventLogDO::getId)
                .filter(id -> id != null && id > 0)
                .toList();
        List<String> userIds = resolveUserIds(scopedTenantId, subjectType, subjectValue);
        List<String> assets = selectedAssets(request, command.targetAssets());

        List<AssetExecutionResult> results = assets.stream()
                .map(asset -> executeAsset(scopedTenantId, requestId, subjectType, subjectValue,
                        command.dryRun(), actor(command.actor()), userIds, matchingEventIds, asset))
                .toList();
        return new ErasureExecutionResult(scopedTenantId, requestId, rollup(results), command.dryRun(), results);
    }

    private AssetExecutionResult executeAsset(Long tenantId,
                                              Long requestId,
                                              String subjectType,
                                              String subjectValue,
                                              boolean dryRun,
                                              String actor,
                                              List<String> userIds,
                                              List<Long> matchingEventIds,
                                              String assetKey) {
        AssetExecutionResult result;
        try {
            result = switch (normalize(assetKey)) {
                case "CDP_USER_PROFILE" -> executeUserScoped(
                        tenantId,
                        assetKey,
                        dryRun,
                        userIds,
                        profileMapper,
                        new LambdaQueryWrapper<CdpUserProfileDO>()
                                .eq(CdpUserProfileDO::getTenantId, tenantId)
                                .in(!userIds.isEmpty(), CdpUserProfileDO::getUserId, userIds));
                case "CDP_USER_IDENTITY" -> executeIdentity(tenantId, subjectType, subjectValue, dryRun, userIds);
                case "CDP_USER_TAG" -> executeUserScoped(
                        tenantId,
                        assetKey,
                        dryRun,
                        userIds,
                        tagMapper,
                        new LambdaQueryWrapper<CdpUserTagDO>()
                                .eq(CdpUserTagDO::getTenantId, tenantId)
                                .in(!userIds.isEmpty(), CdpUserTagDO::getUserId, userIds));
                case "CDP_EVENT_LOG" -> executeEventLog(tenantId, subjectType, subjectValue, dryRun, matchingEventIds);
                case "REALTIME_RETRY_BUFFER" -> executeRealtimeRetry(tenantId, dryRun, matchingEventIds);
                case "DORIS_ODS_CDP_EVENT_LOG", "DORIS_DWD_CDP_USER_EVENT_FACT" ->
                        executeDoris(tenantId, subjectType, subjectValue, dryRun, actor, assetKey);
                case "AUDIENCE_BITMAP_VERSION" -> new AssetExecutionResult(
                        assetKey, WARN, 0, 0,
                        "audience bitmap erasure requires materialization rebuild proof", null);
                default -> new AssetExecutionResult(assetKey, WARN, 0, 0,
                        "asset has no executable erasure handler", null);
            };
        } catch (RuntimeException e) {
            result = new AssetExecutionResult(assetKey, FAIL, 0, 0,
                    "asset erasure failed", e.getMessage());
        }
        recordProof(tenantId, requestId, dryRun, actor, result);
        return result;
    }

    private <T> AssetExecutionResult executeUserScoped(Long tenantId,
                                                       String assetKey,
                                                       boolean dryRun,
                                                       List<String> userIds,
                                                       BaseMapper<T> mapper,
                                                       LambdaQueryWrapper<T> query) {
        if (userIds.isEmpty()) {
            return new AssetExecutionResult(assetKey, SKIPPED, 0, 0,
                    "no user id resolved for subject", null);
        }
        return executeMapperDelete(assetKey, dryRun, mapper, query);
    }

    private AssetExecutionResult executeIdentity(Long tenantId,
                                                 String subjectType,
                                                 String subjectValue,
                                                 boolean dryRun,
                                                 List<String> userIds) {
        LambdaQueryWrapper<CdpUserIdentityDO> query = new LambdaQueryWrapper<CdpUserIdentityDO>()
                .eq(CdpUserIdentityDO::getTenantId, tenantId);
        if (USER_ID.equals(subjectType)) {
            if (userIds.isEmpty()) {
                return new AssetExecutionResult("CDP_USER_IDENTITY", SKIPPED, 0, 0,
                        "no user id resolved for subject", null);
            }
            query.in(CdpUserIdentityDO::getUserId, userIds);
        } else {
            query.eq(CdpUserIdentityDO::getIdentityType, subjectType)
                    .eq(CdpUserIdentityDO::getIdentityValue, subjectValue);
        }
        return executeMapperDelete("CDP_USER_IDENTITY", dryRun, identityMapper, query);
    }

    private AssetExecutionResult executeEventLog(Long tenantId,
                                                 String subjectType,
                                                 String subjectValue,
                                                 boolean dryRun,
                                                 List<Long> matchingEventIds) {
        long matched = matchingEventIds.size();
        int affected = 0;
        if (!dryRun && matched > 0) {
            affected = eventLogMapper.delete(eventQuery(tenantId, subjectType, subjectValue));
        }
        return new AssetExecutionResult("CDP_EVENT_LOG", status(dryRun), matched, affected,
                dryRun ? "dry-run matched CDP event rows" : "deleted CDP event rows", null);
    }

    private AssetExecutionResult executeRealtimeRetry(Long tenantId, boolean dryRun, List<Long> matchingEventIds) {
        if (matchingEventIds.isEmpty()) {
            return new AssetExecutionResult("REALTIME_RETRY_BUFFER", SKIPPED, 0, 0,
                    "no matching event log ids for retry buffer", null);
        }
        LambdaQueryWrapper<CdpWarehouseRealtimeRetryDO> query =
                new LambdaQueryWrapper<CdpWarehouseRealtimeRetryDO>()
                        .eq(CdpWarehouseRealtimeRetryDO::getTenantId, tenantId)
                        .in(CdpWarehouseRealtimeRetryDO::getEventLogId, matchingEventIds);
        return executeMapperDelete("REALTIME_RETRY_BUFFER", dryRun, retryMapper, query);
    }

    private <T> AssetExecutionResult executeMapperDelete(String assetKey,
                                                         boolean dryRun,
                                                         BaseMapper<T> mapper,
                                                         LambdaQueryWrapper<T> query) {
        long matched = nullToZero(mapper.selectCount(query));
        int affected = 0;
        if (!dryRun && matched > 0) {
            affected = mapper.delete(query);
        }
        return new AssetExecutionResult(assetKey, status(dryRun), matched, affected,
                dryRun ? "dry-run matched rows for " + assetKey : "deleted rows for " + assetKey, null);
    }

    private AssetExecutionResult executeDoris(Long tenantId,
                                              String subjectType,
                                              String subjectValue,
                                              boolean dryRun,
                                              String actor,
                                              String assetKey) {
        if (dorisExecutor == null) {
            String message = "Doris privacy erasure executor is not configured";
            return new AssetExecutionResult(assetKey, dryRun ? WARN : FAIL, 0, 0,
                    dryRun ? "dry-run skipped Doris delete because executor is not configured" : "Doris delete unavailable",
                    message);
        }
        CdpWarehouseDorisPrivacyErasureExecutor.Result output = dorisExecutor.execute(
                new CdpWarehouseDorisPrivacyErasureExecutor.Command(
                        assetKey, tenantId, subjectType, subjectValue, dryRun, actor));
        return new AssetExecutionResult(
                assetKey,
                normalizeOutputStatus(output.status(), dryRun),
                output.matchedCount(),
                output.affectedCount(),
                output.proofMessage(),
                output.errorMessage());
    }

    private void recordProof(Long tenantId,
                             Long requestId,
                             boolean dryRun,
                             String actor,
                             AssetExecutionResult result) {
        erasureService.recordAssetProof(tenantId, requestId,
                new CdpWarehousePrivacyErasureService.AssetProofCommand(
                        result.assetKey(),
                        assetLayer(result.assetKey()),
                        dryRun ? "DRY_RUN" : "DELETE",
                        result.status(),
                        result.matchedCount(),
                        result.affectedCount(),
                        result.proofMessage(),
                        result.errorMessage(),
                        actor,
                        LocalDateTime.now()));
    }

    private List<String> resolveUserIds(Long tenantId, String subjectType, String subjectValue) {
        if (USER_ID.equals(subjectType)) {
            return List.of(subjectValue);
        }
        List<CdpUserIdentityDO> identities = safe(identityMapper.selectList(
                new LambdaQueryWrapper<CdpUserIdentityDO>()
                        .eq(CdpUserIdentityDO::getTenantId, tenantId)
                        .eq(CdpUserIdentityDO::getIdentityType, subjectType)
                        .eq(CdpUserIdentityDO::getIdentityValue, subjectValue)));
        return identities.stream()
                .map(CdpUserIdentityDO::getUserId)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private LambdaQueryWrapper<CdpEventLogDO> eventQuery(Long tenantId, String subjectType, String subjectValue) {
        LambdaQueryWrapper<CdpEventLogDO> query = new LambdaQueryWrapper<CdpEventLogDO>()
                .eq(CdpEventLogDO::getTenantId, tenantId);
        switch (subjectType) {
            case "ANONYMOUS_ID" -> query.eq(CdpEventLogDO::getAnonymousId, subjectValue);
            case "DEVICE_ID" -> query.eq(CdpEventLogDO::getDeviceId, subjectValue);
            default -> query.eq(CdpEventLogDO::getUserId, subjectValue);
        }
        return query;
    }

    private List<String> selectedAssets(CdpWarehousePrivacyErasureService.ErasureRequestView request,
                                        List<String> requestedAssets) {
        Set<String> assets = new LinkedHashSet<>();
        List<String> input = requestedAssets == null || requestedAssets.isEmpty()
                ? request.assetProofs().stream()
                        .map(CdpWarehousePrivacyErasureService.AssetProofView::assetKey)
                        .toList()
                : requestedAssets;
        for (String asset : input) {
            if (asset != null && !asset.isBlank()) {
                assets.add(normalize(asset));
            }
        }
        if (assets.isEmpty()) {
            throw new IllegalArgumentException("no erasure assets selected");
        }
        return List.copyOf(assets);
    }

    private String subjectHash(Long tenantId, String subjectType, String subjectValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((tenantId + ":" + subjectType + ":" + subjectValue)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("failed to hash erasure subject", e);
        }
    }

    private String rollup(List<AssetExecutionResult> results) {
        List<String> statuses = results.stream().map(result -> normalize(result.status())).toList();
        if (statuses.contains(FAIL)) {
            return FAIL;
        }
        if (statuses.contains(WARN)) {
            return WARN;
        }
        return PASS;
    }

    private String status(boolean dryRun) {
        return dryRun ? WARN : PASS;
    }

    private String normalizeOutputStatus(String status, boolean dryRun) {
        String normalized = normalize(status);
        if (PASS.equals(normalized) || WARN.equals(normalized) || FAIL.equals(normalized) || SKIPPED.equals(normalized)) {
            return dryRun && PASS.equals(normalized) ? WARN : normalized;
        }
        return dryRun ? WARN : FAIL;
    }

    private String assetLayer(String assetKey) {
        String asset = normalize(assetKey);
        if (asset.startsWith("DORIS_ODS")) {
            return "ODS";
        }
        if (asset.startsWith("DORIS_DWD")) {
            return "DWD";
        }
        if (asset.startsWith("DORIS_DWS")) {
            return "DWS";
        }
        if (asset.startsWith("AUDIENCE")) {
            return "ADS";
        }
        if (asset.startsWith("REALTIME")) {
            return "REALTIME";
        }
        return "CDP";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String actor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private <T> List<T> safe(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    public record ErasureExecutionCommand(
            String subjectValue,
            boolean dryRun,
            String actor,
            List<String> targetAssets) {
    }

    public record ErasureExecutionResult(
            Long tenantId,
            Long requestId,
            String status,
            boolean dryRun,
            List<AssetExecutionResult> assetResults) {
        public ErasureExecutionResult {
            assetResults = assetResults == null ? List.of() : List.copyOf(assetResults);
        }
    }

    public record AssetExecutionResult(
            String assetKey,
            String status,
            long matchedCount,
            long affectedCount,
            String proofMessage,
            String errorMessage) {
    }
}
