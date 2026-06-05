package org.chovy.canvas.domain.compliance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.chovy.canvas.dal.dataobject.CdpUserIdentityDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.dataobject.MarketingSuppressionDO;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.chovy.canvas.dal.mapper.CdpUserIdentityMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.CdpUserTagMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.MarketingSuppressionMapper;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DataDeletionService {

    private final CdpUserProfileMapper profileMapper;
    private final CdpUserIdentityMapper identityMapper;
    private final CdpUserTagMapper tagMapper;
    private final MarketingConsentMapper consentMapper;
    private final MarketingSuppressionMapper suppressionMapper;
    private final MessageSendRecordMapper messageMapper;
    private final CanvasExecutionTraceMapper traceMapper;

    public DataDeletionService(CdpUserProfileMapper profileMapper,
                               CdpUserIdentityMapper identityMapper,
                               CdpUserTagMapper tagMapper,
                               MarketingConsentMapper consentMapper,
                               MarketingSuppressionMapper suppressionMapper,
                               MessageSendRecordMapper messageMapper) {
        this(profileMapper, identityMapper, tagMapper, consentMapper, suppressionMapper, messageMapper, null);
    }

    @Autowired
    public DataDeletionService(CdpUserProfileMapper profileMapper,
                               CdpUserIdentityMapper identityMapper,
                               CdpUserTagMapper tagMapper,
                               MarketingConsentMapper consentMapper,
                               MarketingSuppressionMapper suppressionMapper,
                               MessageSendRecordMapper messageMapper,
                               CanvasExecutionTraceMapper traceMapper) {
        this.profileMapper = profileMapper;
        this.identityMapper = identityMapper;
        this.tagMapper = tagMapper;
        this.consentMapper = consentMapper;
        this.suppressionMapper = suppressionMapper;
        this.messageMapper = messageMapper;
        this.traceMapper = traceMapper;
    }

    @Transactional
    public DeletionResult deleteUserData(DeleteUserDataCommand command) {
        validate(command);
        List<TableDeletionResult> tableResults = new ArrayList<>();
        tableResults.add(processTable(
                "cdp_user_profile",
                profileMapper,
                new LambdaQueryWrapper<CdpUserProfileDO>()
                        .eq(CdpUserProfileDO::getTenantId, command.tenantId())
                        .eq(CdpUserProfileDO::getUserId, command.userId()),
                command.dryRun()));
        tableResults.add(processTable(
                "cdp_user_identity",
                identityMapper,
                new LambdaQueryWrapper<CdpUserIdentityDO>()
                        .eq(CdpUserIdentityDO::getTenantId, command.tenantId())
                        .eq(CdpUserIdentityDO::getUserId, command.userId()),
                command.dryRun()));
        tableResults.add(processTable(
                "cdp_user_tag",
                tagMapper,
                new LambdaQueryWrapper<CdpUserTagDO>()
                        .eq(CdpUserTagDO::getTenantId, command.tenantId())
                        .eq(CdpUserTagDO::getUserId, command.userId()),
                command.dryRun()));
        tableResults.add(processTable(
                "marketing_consent",
                consentMapper,
                new LambdaQueryWrapper<MarketingConsentDO>()
                        .eq(MarketingConsentDO::getTenantId, command.tenantId())
                        .eq(MarketingConsentDO::getUserId, command.userId()),
                command.dryRun()));
        tableResults.add(processTable(
                "marketing_suppression",
                suppressionMapper,
                new LambdaQueryWrapper<MarketingSuppressionDO>()
                        .eq(MarketingSuppressionDO::getTenantId, command.tenantId())
                        .eq(MarketingSuppressionDO::getUserId, command.userId()),
                command.dryRun()));
        tableResults.add(processTable(
                "message_send_record",
                messageMapper,
                new LambdaQueryWrapper<MessageSendRecordDO>()
                        .eq(MessageSendRecordDO::getTenantId, command.tenantId())
                        .eq(MessageSendRecordDO::getUserId, command.userId()),
                command.dryRun()));
        if (traceMapper != null) {
            tableResults.add(processTable(
                    "canvas_execution_trace",
                    traceMapper,
                    new LambdaQueryWrapper<CanvasExecutionTraceDO>()
                            .eq(CanvasExecutionTraceDO::getTenantId, command.tenantId())
                            .and(w -> w.like(CanvasExecutionTraceDO::getInputData, command.userId())
                                    .or()
                                    .like(CanvasExecutionTraceDO::getOutputData, command.userId())
                                    .or()
                                    .like(CanvasExecutionTraceDO::getErrorMsg, command.userId())),
                    command.dryRun()));
        }
        long totalMatched = tableResults.stream().mapToLong(TableDeletionResult::matchedRows).sum();
        long totalDeleted = tableResults.stream().mapToLong(TableDeletionResult::deletedRows).sum();
        return new DeletionResult(command.dryRun(), totalMatched, totalDeleted, List.copyOf(tableResults));
    }

    private <T> TableDeletionResult processTable(String tableName,
                                                 BaseMapper<T> mapper,
                                                 LambdaQueryWrapper<T> query,
                                                 boolean dryRun) {
        long matchedRows = mapper.selectCount(query);
        int deletedRows = 0;
        if (!dryRun && matchedRows > 0) {
            deletedRows = mapper.delete(query);
        }
        return new TableDeletionResult(tableName, matchedRows, deletedRows);
    }

    private void validate(DeleteUserDataCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.tenantId(), "tenantId must not be null");
        if (command.userId() == null || command.userId().isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (!command.dryRun() && (command.actor() == null || command.actor().isBlank())) {
            throw new IllegalArgumentException("actor must not be blank for executed deletion");
        }
    }

    public record DeleteUserDataCommand(Long tenantId, String userId, boolean dryRun, String actor) {
    }

    public record DeletionResult(boolean dryRun,
                                 long totalMatched,
                                 long totalDeleted,
                                 List<TableDeletionResult> tableResults) {
    }

    public record TableDeletionResult(String tableName, long matchedRows, long deletedRows) {
    }
}
