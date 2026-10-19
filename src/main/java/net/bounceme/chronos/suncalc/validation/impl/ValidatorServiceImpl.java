package net.bounceme.chronos.suncalc.validation.impl;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.SneakyThrows;
import net.bounceme.chronos.suncalc.model.Registro;
import net.bounceme.chronos.suncalc.validation.ValidatorService;

@SuppressWarnings("hiding")
public class ValidatorServiceImpl<Registro> implements ValidatorService<Registro> {

	@Override
	@SneakyThrows(ConstraintViolationException.class)
	public void validate(Registro obj) {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		
		Set<ConstraintViolation<Registro>> violations = validator.validate(obj);
		
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}

}
