package org.chovy.canvas.domain.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CanvasStatusEnum {


    DRAFT(0),
    PUBLISHED(1),
    OFFLINE(2),
    ARCHIVED(3),
    KILLED(4);

    private final Integer code;

}
