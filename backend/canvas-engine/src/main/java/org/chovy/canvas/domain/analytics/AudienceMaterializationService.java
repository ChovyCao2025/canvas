package org.chovy.canvas.domain.analytics;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.AudienceMaterializationRunDO;
import org.chovy.canvas.dal.mapper.AudienceMaterializationRunMapper;
import org.chovy.canvas.engine.audience.StableUserIndexService;
import org.chovy.canvas.engine.audience.VersionedAudienceBitmapStore;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AudienceMaterializationService {

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String SOURCE_OLAP = "OLAP";
    private static final int MAX_ERROR_LENGTH = 1000;

    private final AudienceDefinitionRepository definitions;
    private final BehaviorAudienceOlapRepository olap;
    private final BehaviorAudienceRuleCompiler compiler;
    private final StableUserIndexService indexService;
    private final VersionedAudienceBitmapStore bitmapStore;
    private final AudienceMaterializationRunMapper runMapper;
    private final int maxRows;

    @Autowired
    public AudienceMaterializationService(AudienceDefinitionRepository definitions,
                                          BehaviorAudienceOlapRepository olap,
                                          BehaviorAudienceRuleCompiler compiler,
                                          StableUserIndexService indexService,
                                          VersionedAudienceBitmapStore bitmapStore,
                                          AudienceMaterializationRunMapper runMapper,
                                          @Value("${canvas.warehouse.audience-materialization.max-rows:100000}")
                                          int maxRows) {
        this.definitions = definitions;
        this.olap = olap;
        this.compiler = compiler;
        this.indexService = indexService;
        this.bitmapStore = bitmapStore;
        this.runMapper = runMapper;
        this.maxRows = maxRows;
    }

    public MaterializationResult materialize(Long tenantId, Long audienceId, String operator) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (audienceId == null) {
            throw new IllegalArgumentException("audienceId is required");
        }

        AudienceDefinitionDO definition = definitions.requireEnabled(scopedTenantId, audienceId);
        if (definition == null) {
            throw new IllegalArgumentException("audience definition is required");
        }

        Long version = nextVersion(scopedTenantId, audienceId);
        AudienceMaterializationRunDO run = newRun(scopedTenantId, audienceId, version, definition, operator);
        runMapper.insert(run);

        try {
            BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery query =
                    compiler.compile(scopedTenantId, definition.getRuleJson(), maxRows);
            List<String> matchedUsers = olap.findMatchingUsers(query);
            if (matchedUsers == null) {
                matchedUsers = List.of();
            }

            RoaringBitmap bitmap = new RoaringBitmap();
            for (String userId : matchedUsers) {
                long userIndex = indexService.getOrCreateIndex(scopedTenantId, userId);
                if (userIndex < 0 || userIndex > Integer.MAX_VALUE) {
                    throw new IllegalStateException("user index exceeds bitmap range");
                }
                bitmap.add((int) userIndex);
            }

            String bitmapKey = bitmapStore.saveVersion(scopedTenantId, audienceId, version, bitmap, SOURCE_OLAP);
            bitmapStore.markReady(scopedTenantId, audienceId, version);

            run.setStatus(STATUS_SUCCESS);
            run.setMatchedUsers((long) matchedUsers.size());
            run.setBitmapKey(bitmapKey);
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
            return new MaterializationResult(STATUS_SUCCESS, matchedUsers.size());
        } catch (Exception ex) {
            run.setStatus(STATUS_FAILED);
            run.setErrorMessage(limit(ex.getMessage()));
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
            return new MaterializationResult(STATUS_FAILED, 0L);
        }
    }

    private AudienceMaterializationRunDO newRun(Long tenantId,
                                                Long audienceId,
                                                Long version,
                                                AudienceDefinitionDO definition,
                                                String operator) {
        AudienceMaterializationRunDO run = new AudienceMaterializationRunDO();
        run.setTenantId(tenantId);
        run.setAudienceId(audienceId);
        run.setVersion(version);
        run.setStatus(STATUS_RUNNING);
        run.setRuleJson(definition.getRuleJson());
        run.setMatchedUsers(0L);
        run.setStartedAt(LocalDateTime.now());
        run.setCreatedBy(operator);
        return run;
    }

    private Long nextVersion(Long tenantId, Long audienceId) {
        Long version = runMapper.nextVersion(tenantId, audienceId);
        return version == null || version < 1 ? 1L : version;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String limit(String message) {
        String value = message == null ? "materialization failed" : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    public interface AudienceDefinitionRepository {
        AudienceDefinitionDO requireEnabled(Long tenantId, Long audienceId);
    }

    public interface BehaviorAudienceOlapRepository {
        List<String> findMatchingUsers(BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery query);
    }

    public record MaterializationResult(String status, long matchedUsers) {
    }
}
