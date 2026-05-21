package org.chovy.canvas.dto;

import lombok.Data;

@Data
public class SafeUpdateReq {
    private String name;
    private String description;
    private String graphJson;
    private int editVersion;
}
