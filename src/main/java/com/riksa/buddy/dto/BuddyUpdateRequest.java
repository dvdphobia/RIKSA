package com.riksa.buddy.dto;

import com.riksa.buddy.service.BuddyCareRules;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record BuddyUpdateRequest(
        @Size(max = 20) String species,
        @Size(max = 16) String name,
        @Min(0) @Max(BuddyCareRules.MAX_ENERGY) Integer guestEnergy,
        @Min(0) @Max(BuddyCareRules.MAX_GUEST_BOND) Integer guestBond) {}
