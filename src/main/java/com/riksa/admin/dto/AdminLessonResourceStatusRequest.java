package com.riksa.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminLessonResourceStatusRequest {
    @NotNull
    private Boolean hidden;

    @Size(max = 500)
    private String reason;
}
