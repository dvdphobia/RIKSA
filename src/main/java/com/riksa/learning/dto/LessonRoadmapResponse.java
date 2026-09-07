package com.riksa.learning.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/** Category-local, ordered lesson progression for the learner roadmap. */
@Getter
@Setter
@NoArgsConstructor
public class LessonRoadmapResponse {
    private UUID categoryId;
    private String categoryName;
    private List<LessonListResponse> lessons;
}
