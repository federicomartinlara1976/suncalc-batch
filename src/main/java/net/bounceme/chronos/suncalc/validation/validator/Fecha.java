package net.bounceme.chronos.suncalc.validation.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = FechaImpl.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface Fecha {
	String message() default "";

	boolean esMovimiento() default false;
	
	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
