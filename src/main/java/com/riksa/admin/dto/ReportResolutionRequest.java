package com.riksa.admin.dto;

import com.riksa.report.enums.ReportAction;
import com.riksa.report.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportResolutionRequest {
    @NotNull
    private ReportStatus status;

    private ReportAction action;
}
