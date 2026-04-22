package com.civicplatform.validator;

import com.civicplatform.dto.request.UserRequest;
import com.civicplatform.enums.UserType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UserValidator implements ConstraintValidator<ValidUser, UserRequest> {

    @Override
    public void initialize(ValidUser constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(UserRequest userRequest, ConstraintValidatorContext context) {
        if (userRequest == null || userRequest.getUserType() == null) {
            return false;
        }

        boolean isValid = true;

        if (userRequest.getUserType() == UserType.DONOR) {
            if (isBlank(userRequest.getAssociationName())) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Association name is required for DONOR users")
                       .addPropertyNode("associationName")
                       .addConstraintViolation();
                isValid = false;
            }
        }

        if (userRequest.getUserType() == UserType.CITIZEN) {
            if (isBlank(userRequest.getFirstName())) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("First name is required")
                       .addPropertyNode("firstName")
                       .addConstraintViolation();
                isValid = false;
            }
            if (isBlank(userRequest.getLastName())) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Last name is required")
                       .addPropertyNode("lastName")
                       .addConstraintViolation();
                isValid = false;
            }
            if (isBlank(userRequest.getBirthDate())) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Birth date is required for CITIZEN users")
                       .addPropertyNode("birthDate")
                       .addConstraintViolation();
                isValid = false;
            }
        }

        return isValid;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
