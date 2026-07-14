package net.bounceme.chronos.suncalc.validation;

import java.lang.annotation.Annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.Getter;
import lombok.Setter;

public abstract class Validador<A extends Annotation, F> implements ConstraintValidator<A, F> {

	@Getter
	@Setter
	private F campo;

	@Override
	public boolean isValid(F value, ConstraintValidatorContext context) {
		setCampo(value);
		boolean esValidado = true;

		if (esInvalido(value)) {
			componerMensaje(context);
			esValidado = false;
		}

		return esValidado;
	}

	/**
	 * @param context
	 */
	protected void componerMensaje(ConstraintValidatorContext context) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate("E0003").addPropertyNode("errorDomain").addConstraintViolation();
	}

	protected abstract boolean esInvalido(F value);
}
