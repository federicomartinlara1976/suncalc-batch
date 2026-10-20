package net.bounceme.chronos.suncalc.validation.impl;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.SneakyThrows;
import net.bounceme.chronos.suncalc.validation.ValidatorService;

public class ValidatorServiceImpl<T> implements ValidatorService<T> {

	@Override
	@SneakyThrows(ConstraintViolationException.class)
	public void validate(T obj) {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		
		Set<ConstraintViolation<T>> violations = validator.validate(obj);
		
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}

}
