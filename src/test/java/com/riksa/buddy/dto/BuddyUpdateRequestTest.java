package com.riksa.buddy.dto;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuddyUpdateRequestTest {

    @Test
    void guestCareAllowsTheCapsAndRejectsValuesOutsideThem() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            assertThat(validator.validate(new BuddyUpdateRequest(null, null, 100, 300))).isEmpty();
            assertThat(validator.validate(new BuddyUpdateRequest(null, null, null, null))).isEmpty();
            assertThat(validator.validate(new BuddyUpdateRequest(null, null, 101, 301)))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("guestEnergy", "guestBond");
            assertThat(validator.validate(new BuddyUpdateRequest(null, null, -1, -1)))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("guestEnergy", "guestBond");
        }
    }
}
