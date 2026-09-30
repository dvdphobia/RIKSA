package com.riksa.learning.entity;

/**
 * Student-facing exercise formats. New formats can be added without changing
 * the persisted column type because the database stores the enum name as text.
 */
public enum ExerciseType {
    TEXT,
    MULTIPLE_CHOICE,
    TRUE_FALSE
}
