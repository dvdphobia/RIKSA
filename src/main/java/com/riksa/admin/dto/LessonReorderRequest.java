package com.riksa.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class LessonReorderRequest {
    @NotNull
    private UUID categoryId;

    @NotEmpty
    private List<@NotNull UUID> ids;
}
