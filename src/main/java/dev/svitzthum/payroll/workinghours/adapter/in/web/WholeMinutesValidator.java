package dev.svitzthum.payroll.workinghours.adapter.in.web;

import java.time.Duration;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

class WholeMinutesValidator implements ConstraintValidator<WholeMinutes, Duration> {

	@Override
	public boolean isValid(Duration value, ConstraintValidatorContext context) {
		// null is left to @NotNull so the messages stay separate
		return value == null || (value.toSecondsPart() == 0 && value.toNanosPart() == 0);
	}

}

