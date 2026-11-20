package com.bhoomi.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class AuthRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void registrationRejectsInvalidEmailAndWeakPassword() {
        AuthDtos.RegisterRequest request = new AuthDtos.RegisterRequest(
                "not-an-email", null, "A", "short", RegistrationType.BUYER, null);

        Set<String> violations = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString()).collect(java.util.stream.Collectors.toSet());

        assertThat(violations).contains("email", "fullName", "password");
    }
}
