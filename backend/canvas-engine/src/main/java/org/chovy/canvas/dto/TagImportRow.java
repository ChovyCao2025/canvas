package org.chovy.canvas.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TagImportRow {

    private Integer rowNo;
    private String idType;
    private String idValue;
    private String tagCode;
    private String tagValue;
    private LocalDateTime tagTime;
}
