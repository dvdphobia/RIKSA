package com.riksa.buddy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record BuddyUpdateRequest(
        @Size(max = 20) String species,
        @Size(max = 16) String name,
        @Min(0) @Max(100) Integer guestEnergy,
        @Min(0) Integer guestBond) {}
