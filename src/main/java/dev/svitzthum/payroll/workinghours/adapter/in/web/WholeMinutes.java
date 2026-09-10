package dev.svitzthum.payroll.workinghours.adapter.in.web;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;

/**
 * The annotated duration must not have a sub-minute component, because working time is
 * stored as whole minutes (ADR 0003). Rejecting it here turns what would otherwise be a
 * domain exception into a field level validation message.
 */
@Documented
@Constraint(validatedBy = WholeMinutesValidator.class)
@Target({ FIELD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface WholeMinutes {

	String message() default "must be a whole number of minutes";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}

