package com.nexoia.auth.credential.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.length() < 8 || password.length() > 128
                || password.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }

        return password.codePoints().anyMatch(Character::isUpperCase)
                && password.codePoints().anyMatch(Character::isLowerCase)
                && password.codePoints().anyMatch(Character::isDigit)
                && password.codePoints().anyMatch(character -> !Character.isLetterOrDigit(character));
    }
}
