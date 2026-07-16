package com.riksa.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String description;

    private String iconUrl;
}