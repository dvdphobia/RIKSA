package com.riksa.admin.dto;

import com.riksa.market.entity.MarketRarity;
import com.riksa.market.entity.MarketSlot;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class AdminMarketItemRequest {
    @NotNull
    private MarketSlot slot;

    @NotBlank
    @Size(max = 80)
    private String name;

    @NotBlank
    @Size(max = 120)
    private String nameKm;

    @NotNull
    @PositiveOrZero
    private Long price;

    @NotNull
    private MarketRarity rarity;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9_]{1,64}$", message = "must contain only lowercase letters, digits, and underscores")
    private String assetKey;

    @Pattern(regexp = "(?i)^(crocodile|capybara)(\\s*,\\s*(crocodile|capybara))*$",
            message = "must be a comma-separated list of crocodile and/or capybara")
    private String speciesMask;

    @Size(max = 32)
    private String minTier;

    private OffsetDateTime availableFrom;
    private OffsetDateTime availableUntil;

    @NotNull
    private Integer sortOrder;

    @NotNull
    private Boolean active;

    @AssertTrue(message = "availableUntil must be after availableFrom")
    public boolean isAvailabilityWindowValid() {
        return availableFrom == null || availableUntil == null || availableUntil.isAfter(availableFrom);
    }
}
