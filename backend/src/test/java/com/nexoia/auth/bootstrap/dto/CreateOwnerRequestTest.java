package com.nexoia.auth.bootstrap.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CreateOwnerRequestTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsPasswordWithoutTheRequiredComposition() {
        var request = new CreateOwnerRequest("owner", "owner@nexo.local", "Nexo Owner", "password");

        var messages = validator.validate(request).stream()
                .map(violation -> violation.getMessage())
                .toList();

        assertThat(messages)
                .contains("must have 8 to 128 characters with uppercase, lowercase, number and special character, without spaces");
    }

    @Test
    void acceptsPasswordWithEveryRequiredCharacterType() {
        var request = new CreateOwnerRequest("owner", "owner@nexo.local", "Nexo Owner", "Nexo123!");

        assertThat(validator.validate(request)).isEmpty();
    }
}
