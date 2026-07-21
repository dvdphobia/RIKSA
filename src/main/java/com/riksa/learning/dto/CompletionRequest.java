package com.riksa.learning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompletionRequest {
    @NotBlank(message = "Reflection text is required")
    private String reflectionText;
}