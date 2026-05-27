package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.domain.cdp.CdpTagService;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.dto.TagImportResult;
import org.chovy.canvas.dto.TagImportRow;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.chovy.canvas.dal.dataobject.IdentityTypeDO;
import org.chovy.canvas.dal.dataobject.TagImportBatchDO;
import org.chovy.canvas.dal.mapper.TagImportBatchMapper;
import org.chovy.canvas.dal.dataobject.TagImportErrorDO;
import org.chovy.canvas.dal.mapper.TagImportErrorMapper;

/**
 * 标签导入 元数据领域服务。
 *
 * <p>负责事件、接口、标签、系统选项或实验分组等配置型数据的维护和查询。
 * <p>元数据会影响画布运行时行为，因此该层需要兼顾管理端易用性与执行链路缓存一致性。
 */
@Service
@RequiredArgsConstructor
public class TagImportService {

    /** 身份类型服务，用于校验导入行中的用户识别方式是否允许导入。 */
    private final IdentityTypeService identityTypeService;
    /** 标签定义服务，用于校验导入标签和标签值。 */
    private final TagDefinitionService tagDefinitionService;
    /** CDP 用户服务，用于导入或执行时创建用户画像。 */
    private final CdpUserService cdpUserService;
    /** CDP 标签服务，用于写入用户标签。 */
    private final CdpTagService cdpTagService;
    /** 标签导入批次 Mapper。 */
    private final TagImportBatchMapper tagImportBatchMapper;
    /** 标签导入错误 Mapper。 */
    private final TagImportErrorMapper tagImportErrorMapper;

    /** 导入标签行数据，写入批次、错误明细、用户和标签。 */
    @Transactional(rollbackFor = Exception.class)
    public TagImportResult importRows(String sourceType, String fileName, String externalUrl, List<TagImportRow> rows) {
        List<TagImportRow> safeRows = rows == null ? List.of() : rows;
        LocalDateTime now = LocalDateTime.now();

        TagImportBatchDO batch = new TagImportBatchDO();
        batch.setSourceType(sourceType);
        batch.setStatus("PROCESSING");
        batch.setFileName(fileName);
        batch.setExternalUrl(externalUrl);
        batch.setTotalRows(safeRows.size());
        batch.setSuccessRows(0);
        batch.setFailedRows(0);
        batch.setStartedAt(now);
        // 先落批次主记录，后续每行错误和 CDP 标签历史都用 batchId 串联审计。
        tagImportBatchMapper.insert(batch);

        int successRows = 0;
        int failedRows = 0;
        Set<String> seenKeys = new HashSet<>();
        for (int i = 0; i < safeRows.size(); i++) {
            TagImportRow row = safeRows.get(i);
            int rowNo = resolveRowNo(row, i);
            String duplicateKey = buildDuplicateKey(row);
            if (!seenKeys.add(duplicateKey)) {
                failedRows++;
                // 批次内同一身份 + 标签只处理一次，避免文件重复行造成多次打标。
                writeError(batch.getId(), rowNo, row, "DUPLICATE_ROW", "duplicate row in same batch");
                continue;
            }
            try {
                IdentityTypeDO identityType = identityTypeService.requireImportable(row.getIdType());
                tagDefinitionService.requireEnabledTagAndValidateValue(row.getTagCode(), row.getTagValue());
                // 导入来源的新枚举值先同步到元数据，保证后续管理端可见。
                tagDefinitionService.ensureValue(row.getTagCode(), row.getTagValue(), sourceType);
                writeCdpTag(batch.getId(), rowNo, sourceType, identityType, row);
                successRows++;
            } catch (Exception ex) {
                failedRows++;
                writeError(batch.getId(), rowNo, row, "ROW_ERROR", ex.getMessage());
            }
        }

        String finalStatus = resolveStatus(successRows, failedRows);
        batch.setStatus(finalStatus);
        batch.setTotalRows(safeRows.size());
        batch.setSuccessRows(successRows);
        batch.setFailedRows(failedRows);
        batch.setFinishedAt(LocalDateTime.now());
        batch.setErrorMessage(null);
        // 同一事务内最终回写批次状态，确保批次统计和错误明细一起提交或回滚。
        tagImportBatchMapper.updateById(batch);

        TagImportResult result = new TagImportResult();
        result.setBatchId(batch.getId());
        result.setStatus(finalStatus);
        result.setTotalRows(safeRows.size());
        result.setSuccessRows(successRows);
        result.setFailedRows(failedRows);
        return result;
    }

    /** 查询标签导入批次列表。 */
    public List<TagImportBatchDO> listBatches() {
        return tagImportBatchMapper.selectList(new LambdaQueryWrapper<TagImportBatchDO>()
                .orderByDesc(TagImportBatchDO::getId));
    }

    /** 查询指定导入批次的错误明细。 */
    public List<TagImportErrorDO> listErrors(Long batchId) {
        return tagImportErrorMapper.selectList(new LambdaQueryWrapper<TagImportErrorDO>()
                .eq(TagImportErrorDO::getBatchId, batchId)
                .orderByAsc(TagImportErrorDO::getRowNo)
                .orderByAsc(TagImportErrorDO::getId));
    }

    /** 将单行导入数据解析成 CDP 用户并写入用户标签。 */
    private void writeCdpTag(Long batchId, int rowNo, String sourceType, IdentityTypeDO identityType, TagImportRow row) {
        String sourceRefId = batchId == null ? null : String.valueOf(batchId);
        String userId = cdpUserService.ensureUserByIdentity(
                identityType.getCode(),
                row.getIdValue(),
                sourceType,
                sourceRefId).getUserId();
        // 幂等键精确到 batchId + rowNo，接口重放同一批次行不会重复写入标签历史。
        cdpTagService.setTag(userId, new CdpTagWriteReq(
                row.getTagCode(),
                row.getTagValue(),
                "标签导入",
                null,
                sourceType,
                sourceRefId,
                null,
                "TAG_IMPORT:" + batchId + ":" + rowNo));
    }

    /** 记录单行导入失败明细，保留原始载荷便于后台排查。 */
    private void writeError(Long batchId, Integer rowNo, TagImportRow row, String errorCode, String errorMsg) {
        TagImportErrorDO error = new TagImportErrorDO();
        error.setBatchId(batchId);
        error.setRowNo(rowNo);
        error.setRawPayload(formatRawPayload(row));
        error.setErrorCode(errorCode);
        error.setErrorMsg(errorMsg);
        tagImportErrorMapper.insert(error);
    }

    /** 解析原始行号，缺失时使用当前列表下标生成行号。 */
    private static Integer resolveRowNo(TagImportRow row, int index) {
        if (row != null && row.getRowNo() != null) {
            return row.getRowNo();
        }
        return index + 1;
    }

    /** 构造批次内去重键，避免同一身份和标签重复导入。 */
    private static String buildDuplicateKey(TagImportRow row) {
        return String.join("|",
                normalizeIdType(row == null ? null : row.getIdType()),
                normalizeText(row == null ? null : row.getIdValue()),
                normalizeText(row == null ? null : row.getTagCode()));
    }

    /** 将导入行格式化为错误表中的原始载荷文本。 */
    private static String formatRawPayload(TagImportRow row) {
        if (row == null) {
            return "rowNo=null,idType=null,idValue=null,tagCode=null,tagValue=null,tagTime=null";
        }
        return "rowNo=" + row.getRowNo()
                + ",idType=" + row.getIdType()
                + ",idValue=" + row.getIdValue()
                + ",tagCode=" + row.getTagCode()
                + ",tagValue=" + row.getTagValue()
                + ",tagTime=" + row.getTagTime();
    }

    /** 根据成功和失败行数计算批次最终状态。 */
    private static String resolveStatus(int successRows, int failedRows) {
        if (failedRows == 0) {
            return "SUCCESS";
        }
        if (successRows == 0) {
            return "FAILED";
        }
        return "PARTIAL_SUCCESS";
    }

    /** 规范化身份类型文本，用于批次内重复判断。 */
    private static String normalizeIdType(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /** 规范化普通文本字段，null 按空串参与去重。 */
    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
