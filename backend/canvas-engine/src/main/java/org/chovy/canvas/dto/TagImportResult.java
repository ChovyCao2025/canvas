package org.chovy.canvas.dto;

import lombok.Data;

@Data
public class TagImportResult {

    private Long batchId;
    private String status;
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
}
