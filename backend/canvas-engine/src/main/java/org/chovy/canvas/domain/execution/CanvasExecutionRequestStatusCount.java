package org.chovy.canvas.domain.execution;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CanvasExecutionRequestStatusCount {

    private String status;
    private Long count;
}
