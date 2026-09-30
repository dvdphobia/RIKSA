package com.riksa.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.riksa.common.enums.DifficultyLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Size;

/**
 * Omitted fields mean "don't change". Explicit nulls clear nullable fields such
 * as description, category, reflectionPrompt, and roadmapUnitId.
 * {@code exercises} is a special case: if present, it replaces all existing
 * exercises; if null, exercises are left untouched.
 */
@Getter
@Setter
public class LessonUpdateRequest {
    private String title;
    private String description;
    private DifficultyLevel difficulty;
    @Min(0)
    private Integer baseXp;
    @Min(1)
    private Integer position;

    @Min(1)
    private Integer estimatedMinutes;
    private UUID categoryId;
    private String reflectionPrompt;
    private UUID roadmapUnitId;

    /** Accepted from the panel; lesson publication is not persisted by the current schema. */
    private Boolean published;

    @JsonIgnore
    private boolean categoryIdProvided;
    @JsonIgnore
    private boolean descriptionProvided;
    @JsonIgnore
    private boolean reflectionPromptProvided;
    @JsonIgnore
    private boolean roadmapUnitIdProvided;

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
        this.categoryIdProvided = true;
    }

    public void setDescription(String description) {
        this.description = description;
        this.descriptionProvided = true;
    }

    public void setReflectionPrompt(String reflectionPrompt) {
        this.reflectionPrompt = reflectionPrompt;
        this.reflectionPromptProvided = true;
    }

    public void setRoadmapUnitId(UUID roadmapUnitId) {
        this.roadmapUnitId = roadmapUnitId;
        this.roadmapUnitIdProvided = true;
    }

    @JsonIgnore
    public boolean isCategoryIdProvided() {
        return categoryIdProvided;
    }

    @JsonIgnore
    public boolean isDescriptionProvided() {
        return descriptionProvided;
    }

    @JsonIgnore
    public boolean isReflectionPromptProvided() {
        return reflectionPromptProvided;
    }

    @JsonIgnore
    public boolean isRoadmapUnitIdProvided() {
        return roadmapUnitIdProvided;
    }

    @Valid
    @Size(min = 1)
    private List<ExerciseRequest> exercises;
}
