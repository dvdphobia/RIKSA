package com.riksa.admin.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BannedWordsSettings(@NotNull @Size(max = 1000) List<@NotBlank String> words) {
}
