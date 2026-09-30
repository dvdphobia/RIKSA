package com.riksa.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class BulkLessonResult {
    private int successCount;
    private int failureCount;
    private List<String> errors;
}