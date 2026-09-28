package com.riksa.admin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class XpAdjustmentRequest {
    @NotNull
    @Min(-2147483647L)
    @Max(2147483647L)
    private Integer amount;

    @NotBlank
    @Size(max = 500)
    private String reason;

    @AssertTrue(message = "amount must not be zero")
    public boolean isAmountNonZero() {
        return amount == null || amount != 0;
    }
}
