package com.riksa.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RoadmapUnitRequest {
    @NotNull
    private UUID categoryId;

    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;
}
