package org.chovy.canvas.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TagImportPushReq {

    private List<TagImportRow> rows = new ArrayList<>();
}
