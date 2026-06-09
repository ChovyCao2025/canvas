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
/**
 * DataDeletionService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class DataDeletionService {

    private final CdpUserProfileMapper profileMapper;
    private final CdpUserIdentityMapper identityMapper;
    private final CdpUserTagMapper tagMapper;
    private final MarketingConsentMapper consentMapper;
    private final MarketingSuppressionMapper suppressionMapper;
    private final MessageSendRecordMapper messageMapper;
    private final CanvasExecutionTraceMapper traceMapper;

    /**
     * 初始化 DataDeletionService 实例。
     *
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param identityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param tagMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param consentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param suppressionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param messageMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public DataDeletionService(CdpUserProfileMapper profileMapper,
                               CdpUserIdentityMapper identityMapper,
                               CdpUserTagMapper tagMapper,
                               MarketingConsentMapper consentMapper,
                               MarketingSuppressionMapper suppressionMapper,
                               MessageSendRecordMapper messageMapper) {
        this(profileMapper, identityMapper, tagMapper, consentMapper, suppressionMapper, messageMapper, null);
    }

    @Autowired
    /**
     * 初始化 DataDeletionService 实例。
     *
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param identityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param tagMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param consentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param suppressionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param messageMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param traceMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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
    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 deleteUserData 流程生成的业务结果。
     */
    public DeletionResult deleteUserData(DeleteUserDataCommand command) {
        // 准备本次处理所需的上下文和中间变量。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        long totalDeleted = tableResults.stream().mapToLong(TableDeletionResult::deletedRows).sum();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return new DeletionResult(command.dryRun(), totalMatched, totalDeleted, List.copyOf(tableResults));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tableName 名称文本，用于展示或唯一性校验。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param query query 参数，用于 processTable 流程中的校验、计算或对象转换。
     * @param dryRun dry run 参数，用于 processTable 流程中的校验、计算或对象转换。
     * @return 返回 processTable 流程生成的业务结果。
     */
    private <T> TableDeletionResult processTable(String tableName,
                                                 BaseMapper<T> mapper,
                                                 LambdaQueryWrapper<T> query,
                                                 boolean dryRun) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        long matchedRows = mapper.selectCount(query);
        int deletedRows = 0;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!dryRun && matchedRows > 0) {
            deletedRows = mapper.delete(query);
        }
        return new TableDeletionResult(tableName, matchedRows, deletedRows);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     */
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

    /**
     * DeleteUserDataCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record DeleteUserDataCommand(Long tenantId, String userId, boolean dryRun, String actor) {
    }

    /**
     * DeletionResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record DeletionResult(boolean dryRun,
                                 long totalMatched,
                                 long totalDeleted,
                                 List<TableDeletionResult> tableResults) {
    }

    /**
     * TableDeletionResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record TableDeletionResult(String tableName, long matchedRows, long deletedRows) {
    }
}
