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

@Service
@RequiredArgsConstructor
public class TagImportService {

    private final IdentityTypeService identityTypeService;
    private final TagDefinitionService tagDefinitionService;
    private final CdpUserService cdpUserService;
    private final CdpTagService cdpTagService;
    private final TagImportBatchMapper tagImportBatchMapper;
    private final TagImportErrorMapper tagImportErrorMapper;

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
                writeError(batch.getId(), rowNo, row, "DUPLICATE_ROW", "duplicate row in same batch");
                continue;
            }
            try {
                IdentityTypeDO identityType = identityTypeService.requireImportable(row.getIdType());
                tagDefinitionService.requireEnabledTagAndValidateValue(row.getTagCode(), row.getTagValue());
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
        tagImportBatchMapper.updateById(batch);

        TagImportResult result = new TagImportResult();
        result.setBatchId(batch.getId());
        result.setStatus(finalStatus);
        result.setTotalRows(safeRows.size());
        result.setSuccessRows(successRows);
        result.setFailedRows(failedRows);
        return result;
    }

    public List<TagImportBatchDO> listBatches() {
        return tagImportBatchMapper.selectList(new LambdaQueryWrapper<TagImportBatchDO>()
                .orderByDesc(TagImportBatchDO::getId));
    }

    public List<TagImportErrorDO> listErrors(Long batchId) {
        return tagImportErrorMapper.selectList(new LambdaQueryWrapper<TagImportErrorDO>()
                .eq(TagImportErrorDO::getBatchId, batchId)
                .orderByAsc(TagImportErrorDO::getRowNo)
                .orderByAsc(TagImportErrorDO::getId));
    }

    private void writeCdpTag(Long batchId, int rowNo, String sourceType, IdentityTypeDO identityType, TagImportRow row) {
        String sourceRefId = batchId == null ? null : String.valueOf(batchId);
        String userId = cdpUserService.ensureUserByIdentity(
                identityType.getCode(),
                row.getIdValue(),
                sourceType,
                sourceRefId).getUserId();
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

    private void writeError(Long batchId, Integer rowNo, TagImportRow row, String errorCode, String errorMsg) {
        TagImportErrorDO error = new TagImportErrorDO();
        error.setBatchId(batchId);
        error.setRowNo(rowNo);
        error.setRawPayload(formatRawPayload(row));
        error.setErrorCode(errorCode);
        error.setErrorMsg(errorMsg);
        tagImportErrorMapper.insert(error);
    }

    private static Integer resolveRowNo(TagImportRow row, int index) {
        if (row != null && row.getRowNo() != null) {
            return row.getRowNo();
        }
        return index + 1;
    }

    private static String buildDuplicateKey(TagImportRow row) {
        return String.join("|",
                normalizeIdType(row == null ? null : row.getIdType()),
                normalizeText(row == null ? null : row.getIdValue()),
                normalizeText(row == null ? null : row.getTagCode()));
    }

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

    private static String resolveStatus(int successRows, int failedRows) {
        if (failedRows == 0) {
            return "SUCCESS";
        }
        if (successRows == 0) {
            return "FAILED";
        }
        return "PARTIAL_SUCCESS";
    }

    private static String normalizeIdType(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
